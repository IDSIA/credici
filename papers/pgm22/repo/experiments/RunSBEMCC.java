package repo.experiments;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

/*

Parameters CLI:
-w -x 20 --seed 0 rand13_mk0_nEndo4_k05_0.uai

* */

public class RunSBEMCC extends Terminal {
    @CommandLine.Parameters(description = "Model path in UAI format.")
    private String modelPath;

    @CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM internal iterations. Default to 500")
    private int maxIter = 500;

    @CommandLine.Option(names={"-w", "--weighted"}, description = "If activated, improved weighted EM is run")
    boolean weighted = true;

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names = {"-p", "--sparents"}, description = "Number of endogenous parents of the selector. Default to 3")
    private int numSelectorParents = 3;

    @CommandLine.Option(names = {"-x", "--executions"}, description = "Number independent EM runs. Default to 20")
    private int executions = 20;

    @CommandLine.Option(names = {"-r", "--ratioConv"}, description = "For the output statistics, ratio of the maximum interval. Default to 0.95")
    private double ratioConv = 0.90;

    @CommandLine.Option(names = {"-as", "--addSeed"}, description = "Aaddional seed only for staring points. Defaults to 0")
    private long addSeed = 0;


    /// Global ///
    TIntIntMap[] data = null;
    StructuralCausalModel model = null;
    Path wdir = null;
    HashMap queriesExact; // make string,double
    HashMap queriesExactData; // make string,double

    List<int[]> assigList = null;
    int cause, effect;
    int[] parents = null;
    List<HashMap> results = null;

    String treewidth = "";
    String endoTreewidth = "";
    String biasTreewidth = "";
    String biasEndoTreewidth = "";





    @Override
    protected void entryPoint() throws Exception {
        init();
        selectionParams();
        //learnExactModel();    // Just for testing
        learnUnbiassedModel();
        learnBiasedModels();
        save();

    }

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new RunSBEMCC(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    private String getID(){
        //mIter500_wtrue_sparents3_x20_0
        String str = "";
        str += "_mIter"+this.maxIter;
        str += "_w"+this.weighted;
        str += "_sparents"+this.numSelectorParents;
        str += "_x"+this.executions;
        str += "_"+this.seed;

        return str;

    }


    public void init() throws IOException, CsvException {

        wdir = Paths.get(".");
        RandomUtil.setRandomSeed(seed);
        logger.info("Starting logger with seed "+seed);

        // Load model
        String fullpath = wdir.resolve(modelPath).toString();
        model = (StructuralCausalModel) IO.readUAI(fullpath);
        logger.info("Loaded model from: "+fullpath);

        try {
            treewidth = String.valueOf(model.getTreewidth());
            endoTreewidth = String.valueOf(model.getTreewidth());
        }catch (Exception e){
            logger.warn("error calculating model treewidth");
        }



        // Load data
        fullpath = wdir.resolve(modelPath.replace(".uai",".csv")).toString();
        data = DataUtil.fromCSV(fullpath);
        int datasize = data.length;
        logger.info("Loaded "+datasize+" data instances from: "+fullpath);


        // Load queries
        fullpath = wdir.resolve(modelPath.replace(".uai","_queries.csv")).toString();
        //DataUtil.fromCSV(fullpath);


        //ReaderCSV reader = new ReaderCSV(fullpath).read();

        CSVReader csvReader =  new CSVReaderBuilder(new FileReader(fullpath))
                .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                .build();
        HashMap queriesExact = new HashMap<>();
        HashMap queriesExactData = new HashMap<>();
        String[] keys = Arrays.stream(csvReader.readNext()).filter(s -> !s.equals("data_based")).toArray(String[]::new);
        for(String[] r : csvReader.readAll()){

            HashMap queries = queriesExact;
            if(r[0].equals("true")) queries = queriesExactData;
            for(int i=0; i< keys.length; i++) queries.put(keys[i], Double.parseDouble(r[i+1]));
        }

        logger.debug("Exact queries: "+queriesExact);
        logger.debug("Exact queries (data-based): "+queriesExactData);

        cause = ((Double)queriesExactData.get("cause")).intValue();
        effect = ((Double)queriesExactData.get("effect")).intValue();

        logger.info("Loaded exact query results from : "+fullpath);
        logger.info(queriesExact.toString());

        // initialize results
        results = new ArrayList<HashMap>();

        if (!queriesExact.isEmpty()) {
            addResults("exact", false, 1.0,
                    Double.NaN, Double.NaN, Double.NaN,
                    (double) queriesExact.get("ace_l"), (double) queriesExact.get("ace_u"),
                    (double) queriesExact.get("pns_l"), (double) queriesExact.get("pns_u"), null, null);
        }
        addResults("exact_data_based", false, 1.0,
                Double.NaN, Double.NaN,Double.NaN,
                (double) queriesExactData.get("ace_l"), (double)queriesExactData.get("ace_u"),
                (double)queriesExactData.get("pns_l"), (double)queriesExactData.get("pns_u"), null, null);



    }

    private void selectionParams() {
        // Fix the parents of S
        int[] endoVars = model.getEndogenousVars();
        parents = new int[numSelectorParents];
        parents[0] = endoVars[0];
        parents[numSelectorParents-1] = endoVars[endoVars.length-1];
        int idx[] = CollectionTools.shuffle(IntStream.range(1, endoVars.length-1).toArray());
        for(int i=0; i<numSelectorParents-2; i++)
            parents[i+1] = endoVars[idx[i]];
        logger.info("Random selector parents: "+ Arrays.toString(parents));
        int parentComb = model.getDomain(parents).getCombinations();
        assigList = getRandomSeqIntMask(parentComb, true);

        //assigList = new ArrayList(Collections.singleton(assigList.get(assigList.size() - 1))); // todo: remove

        logger.info("Ramdom selector assignments: "+assigList.size());

        int i = 1;
        for(int[] assignments : assigList) {
            logger.debug("Assignment "+i+": "+Arrays.toString(assignments));
            i++;
        }



        }

    private void learnBiasedModels() throws ExecutionControl.NotImplementedException, InterruptedException {

        logger.info("Learning multiple biased model (with selector)");

        int i = 0;

        for(int[] assignments : assigList) {
            RandomUtil.setRandomSeed(seed+addSeed);

            StructuralCausalModel modelBiased = SelectionBias.addSelector(model, parents, assignments);

            if(i==0) {
                try {
                    biasTreewidth = String.valueOf(modelBiased.getTreewidth());
                    biasEndoTreewidth = String.valueOf(modelBiased.getTreewidth());
                }catch (Exception e){
                    logger.warn("error calculating bias treewidth");
                }
            }

            int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
            TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

            int n1 = (int) Stream.of(dataBiased).filter(d -> d.get(selectVar) == 1).count();
            double ps1 = (1.0 * n1) / dataBiased.length;

            logger.info("Learning model with p(S=1)=" + ps1);

            int[] trainable = Arrays.stream(modelBiased.getExogenousVars())
                    .filter(v -> !ArraysUtil.contains(selectVar, modelBiased.getChildren(v)))
                    .toArray();


            Watch.start();

            EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
                    .setMaxEMIter(maxIter)
                    .setNumTrajectories(executions)
                    .setWeightedEM(weighted)
                    .build();

            Watch.stopAndLog(logger, "Performed " + executions + " EM runs in ");
            long time_learn = Watch.getWatch().getTime();


            List endingPoints = builder.getSelectedPoints().stream().map(m -> {
                m = m.copy();
                m.removeVariable(m.getExogenousParents(selectVar)[0]);
                m.removeVariable(selectVar);
                return m;
            }).collect(Collectors.toList());
            CausalMultiVE inf = new CausalMultiVE(endingPoints);
            double[] res = runQueries(inf);

            int[] trsizes = builder.getTrajectories().stream().mapToInt(t -> t.size()-1).toArray();
            logger.debug("Trajectories sizes: "+Arrays.toString(trsizes));


            addResults("EMCC", true, ps1,
                    time_learn, res[0], res[1],
                    res[2],res[3],res[4],res[5],
                    builder, inf.getIndividualPNS(cause, effect, 0, 1));

            i++;


        }
    }

    private double[] runQueries(CausalInference inf) throws InterruptedException, ExecutionControl.NotImplementedException {

        double tACE, tPNS;
        List resList = new ArrayList();
        VertexFactor p = null;
        double[] v = null;

        Watch.start();
        p = (VertexFactor) inf.averageCausalEffects(cause, effect, 1, 1, 0);
        Watch.stopAndLog(logger, "Computed ACE in ");
        tACE = Watch.getWatch().getTime();
        v =  Doubles.concat(p.getData()[0]);
        if(v.length==1) v = new double[]{v[0],v[0]};
        Arrays.sort(v);
        for(double val : v) resList.add(val);
        logger.info("ACE: "+Arrays.toString(v));

        Watch.start();
        p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
        Watch.stopAndLog(logger, "Computed PNS in ");
        tPNS = Watch.getWatch().getTime();
        v =  Doubles.concat(p.getData()[0]);
        if(v.length==1) v = new double[]{v[0],v[0]};
        Arrays.sort(v);
        for(double val : v) resList.add(val);
        logger.info("PNS: "+Arrays.toString(v));

        resList.add(0, tPNS);
        resList.add(0, tACE);

        // return
        double[] res = resList.stream().mapToDouble(d -> (double)d).toArray();
        return res;
    }

    public void learnExactModel() throws ExecutionControl.NotImplementedException, InterruptedException {
        logger.info("Exact learning unbiased model (without selector)");

        Watch.start();

        System.out.println(model);
        data = model.samples(1000, model.getEndogenousVars());
        System.out.println(model.getEmpiricalMap(true));
        System.out.println(DataUtil.getEmpiricalMap(model, data));
        CredalCausalVE inf = new CredalCausalVE(model);
        System.out.println(inf.probNecessityAndSufficiency(cause, effect));

        //SparseModel vmodel = model.toVCredal(DataUtil.getEmpiricalMap(model, data).values());
        //CredalCausalVE inf = new CredalCausalVE(model, DataUtil.getEmpiricalMap(model, data).values());

        Watch.stopAndLog(logger, "Performed exact learning (without selector) in ");
        long time_learn = Watch.getWatch().getTime();
/*
        double[] res = runQueries(inf);


        addResults("exact", false, 1,
                time_learn, res[0], res[1],
                res[2],res[3],res[4],res[5],
                -1);*/
    }

    public  void learnUnbiassedModel() throws InterruptedException, ExecutionControl.NotImplementedException {

        logger.info("Learning unbiased model (without selector)");

        Watch.start();
        EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(executions)
                .setWeightedEM(weighted)
                .build();

        Watch.stopAndLog(logger, "Performed "+executions+" EM runs (without selector) in ");
        long time_learn = Watch.getWatch().getTime();

        CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
        double[] res = runQueries(inf);


        int[] trsizes = builder.getTrajectories().stream().mapToInt(t -> t.size()-1).toArray();
        logger.debug("Trajectories sizes: "+Arrays.toString(trsizes));

        addResults("EMCC", false, 1,
                time_learn, res[0], res[1],
                res[2],res[3],res[4],res[5],
                builder, inf.getIndividualPNS(cause, effect, 0, 1));

    }



    private void addResults(String method, boolean selector, double ps1,
                            double time_learn, double time_ace, double time_pns,
                            double ace_l, double ace_u, double pns_l, double pns_u,
                            EMCredalBuilder builder, double[] individualPNS){

        String msg = "Adding results:";

        HashMap r = new HashMap<String, String>();

        r.put("method", method);
        msg += " selector="+method;

        r.put("selector", String.valueOf(selector));
        msg += " selector="+selector;
        r.put("ps1", String.valueOf(ps1));
        msg += " ps1="+ps1;

        if(!Double.isNaN(time_learn)) {
            r.put("time_learn", String.valueOf(time_learn));
            msg += " time_learn=" + time_learn;
        }
        if(!Double.isNaN(time_ace)) {
            r.put("time_ace", String.valueOf(time_ace));
            msg += " time_ace=" + time_ace;
        }
        if(!Double.isNaN(time_pns)) {
            r.put("time_pns", String.valueOf(time_pns));
            msg += " time_pns=" + time_pns;
        }


        r.put("ace_l", String.valueOf(ace_l));
        msg += " ace_l="+ace_l;
        r.put("ace_u", String.valueOf(ace_u));
        msg += " ace_u="+ace_u;
        r.put("pns_l", String.valueOf(pns_l));
        msg += " pns_l="+pns_l;
        r.put("pns_u", String.valueOf(pns_u));
        msg += " pns_u="+pns_u;

        r.put("model_path", modelPath);

        if(selector)
            r.put("sparents", Arrays.toString(parents).replace(", ", "|"));

        r.put("treewdith", treewidth);
        r.put("endo_treewdith", endoTreewidth);
        r.put("bias_treewdith", biasTreewidth);
        r.put("bias_endo_treewdith", biasEndoTreewidth);

        if(builder != null) {
            int i = 0;
            for(List<StructuralCausalModel> t : builder.getTrajectories()){
                int size = t.size() - 1;
                r.put("trajectory_size_"+i, size);
                i++;

            }
        }
        if(individualPNS != null)
            for(int i=0; i<individualPNS.length; i++) r.put("pns_"+i, individualPNS[i]);


        results.add(r);
        logger.debug(msg);

    }

    private void save() throws IOException {
        //String filename = getGenID();
        //String fullpath;
        //wdir = Paths.get(this.outputPath);
        String filename = Iterables.getLast(Arrays.asList(this.modelPath.split("/"))).replace(".uai",getID());
        String fullpath = this.wdir.resolve(this.output).resolve(filename+".csv").toString();
        logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, results);



    }



    }
