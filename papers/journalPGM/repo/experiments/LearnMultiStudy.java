package repo.experiments;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.idsia.credici.utility.Assertion.assertTrue;

/*

Parameters CLI:

-w  -tps 0.75 -x 1 -m 1 -sc KL -th 0.0 --seed 0 ./papers/journalPGM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_2.uai
-w -x 10 -m 100 -sc KL -th 0.0 --seed 0 ./papers/journalPGM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_2.uai
-w -rw -nh 0 -x 2 -m 1 -sc KL -th 0.0 --debug --seed 0 ./papers/journalEM/models/synthetic/s1/random_mc2_n7_mid3_d1000_05_mr098_r10_2.uai
* */


public class LearnMultiStudy extends Terminal {

    @CommandLine.Parameters(description = "Model path in UAI format.")
    private String modelPath;

    @CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM internal iterations. Default to 300")
    private int maxIter = 300;

    @CommandLine.Option(names={"-w", "--weighted"}, description = "If activated, improved weighted EM is run")
    boolean weighted = false;

    @CommandLine.Option(names={"-rw", "--rewrite"}, description = "If activated, results are rewritten. Otherwise, process is stopped if there are existing results.")
    boolean rewrite = false;

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names={"-lp", "--localparam"}, description = "One of the exogenous variables is set as local parameter")
    boolean localparam = false;

    @CommandLine.Option(names = {"-x", "--executions"}, description = "Number independent EM runs. Only for EM-based methods. Default to 40")
    private int executions = 40;

    @CommandLine.Option(names = {"-th", "--threshold"}, description = "KL threshold for stopping EM execution. Default to 0.0")
    private double threshold = 0.0;

    @CommandLine.Option(names = {"-tps", "--targetps"}, description = "Target P(S=1). Default to 0.25")
    private double targetPS = 0.25;


    @CommandLine.Option(names = {"-sc", "--stopcriteria"}, description = "Stopping criteria: ${COMPLETION-CANDIDATES}")
    private FrequentistCausalEM.StopCriteria stopCriteria = FrequentistCausalEM.StopCriteria.KL;




    /// Global ///
    TIntIntMap[] data = null;
    StructuralCausalModel model = null;
    Path wdir = null;
    HashMap<String, String> info;
    List<DataIntegrator> integrators = new ArrayList<>();

    String modelID = "";

    int cause, effect, Z, W;
    int trueState = 0, falseState = 1;

    List<CausalInference> inferences = new ArrayList<>();
    double pns_l, pns_u;
    double[] pnsValues;
    EMCredalBuilder builder = null;

    double pS1 = 1;
    int Svar = -1;

    HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();

    @Override
    protected void checkArguments() {
        logger.info("Checking arguments");
        assertTrue(maxIter > 0, " Wrong value for maxIter: " + maxIter);
        assertTrue(executions > 0, " Wrong value for maxIter: " + executions);
        //assertTrue(localparam, "Not implemented for with all global parameters");

    }

    @Override
    protected void entryPoint() throws Exception {
        init();
        buildAndLearn();
        processResults();
        save();


    }

    private void selectCauseEffect(){
        // Find cause and effect
        SparseDirectedAcyclicGraph dag = model.getNetwork();
        int[] order = DAGUtil.getTopologicalOrder(dag, model.getEndogenousVars());
        effect = order[order.length-1];
        for(int i=0; i<order.length; i++){
            int x = order[i];
            if(CausalGraphTools.isCofounded(dag, x)) {
                cause = x;
                Z = CausalGraphTools.getOtherCofounded(dag, x)[0];
                W = CollectionTools.shuffle(
                        IntStream.of(model.getEndogenousVars()).filter( v -> v!=cause && v!=effect && v!=Z).toArray())[0];
                return;
            }
        }
        throw new IllegalArgumentException("Cannot determine cause");

    }

    public void buildAndLearn() throws InterruptedException, ExecutionControl.NotImplementedException {

        selectCauseEffect();
        logger.info("Set cause="+cause+" effect="+effect+" Z="+Z+" W="+W);
        boolean isCofounded = CausalGraphTools.isCofounded(model.getNetwork(), cause);
        boolean dseparated = DAGUtil.dseparated(model.getNetwork(), model.getExogenousParents(cause)[0], effect);
        if(!isCofounded || dseparated)
            throw new IllegalArgumentException("Error setting cause/effect");


        if(!List.of(cause,effect,Z,W).stream().sequential().allMatch(new HashSet<>()::add))
            throw new IllegalArgumentException("Error setting cause/effect");


        Random r = RandomUtil.getRandom();
        RandomUtil.setRandomSeed(0);
        // Data sampling
        TIntIntMap[] dataInterX = model.samplesIntervened(data.length / 2, cause, model.getEndogenousVars());
        TIntIntMap[] dataInterXb = model.samplesIntervened(data.length / 2, cause, model.getEndogenousVars());

        RandomUtil.setRandom(r);
        // Learn all the models
        learnMultiStudyModelbiased(data, dataInterX, dataInterXb, "Dobs+Dx+Dxb ");
        learnMultiStudyModelbiased(data, null, dataInterXb, "Dobs+Dxb");
        learnMultiStudyModelbiased(null, dataInterX, dataInterXb, "Dx+Dxb");
        learnMultiStudyModelbiased(null, null, dataInterXb, "Dxb");
        learnMultiStudyModelbiased(null, dataInterX, null, "Dx");

    }



    private void learnMultiStudyModelbiased(TIntIntMap[] dataObs, TIntIntMap[] dataInterX, TIntIntMap[] dataInterXb, String descr) throws InterruptedException, ExecutionControl.NotImplementedException {


        int locaVar = W;
        int[] Sparents = new int[]{cause, effect, W};

        logger.info("Preparing model: "+descr);
        results.put(descr, new HashMap<String, String>());

        int numDatasets = (int) Stream.of(dataObs, dataInterX, dataInterXb).filter(d -> d != null).count();
        boolean biased = false;
        DataIntegrator integrator = null;
        if(numDatasets>1) {
            if(localparam)
                integrator = DataIntegrator.of(model, model.getExogenousParents(locaVar)[0]);
            else
                integrator = DataIntegrator.of(model);
            int s = 0;
            if (dataObs != null) {
                integrator.setObservationalData(dataObs, s);
                s++;
            }
            if (dataInterX != null) {
                integrator.setData(dataInterX, new int[]{cause}, s);
                s++;
            }

            if (dataInterXb != null) {
                biased = true;
                integrator.setData(dataInterXb, new int[]{cause}, s);
                s++;
            }
        }else{
            integrator = DataIntegrator.of(model);
            if (dataObs != null)
                integrator.setObservationalData(dataObs);
            if (dataInterX != null)
                integrator.setData(dataInterX, new int[]{cause});

            if (dataInterXb != null) {
                biased = true;
                integrator.setData(dataInterXb, new int[]{cause});
            }

        }

        integrator.setDescription(descr);
        integrator.compile();

        logger.debug("Built integrator model: "+integrator);

        StructuralCausalModel extModel = integrator.getExtendedModel(numDatasets>1);
        TIntIntMap[] extData = integrator.getExtendedData(numDatasets>1);
        //integrator.summary();


        if(biased){
            int[] extVars = extModel.getEndogenousVars();
            int[] endoVarsb = IntStream.range(extVars.length-model.getEndogenousVars().length, extVars.length)
                    .map(i -> extVars[i])
                    .toArray();

            int[] SparentsExt = ArraysUtil.slice(endoVarsb, Sparents);
            int[] Sassig = SelectionBias.getClosestAssignment(dataInterXb, model.getDomain(Sparents), targetPS);



            // Integrate selection bias
            extModel = SelectionBias.addSelector(extModel, SparentsExt, Sassig);
            Svar = SelectionBias.findSelector(extModel);
            extData = SelectionBias.applySelector(extData, extModel, Svar);

            int N0 = (int) Arrays.stream(extData).filter(d -> d.containsKey(Svar) && d.get(Svar)==0).count();
            int N1 = (int) Arrays.stream(extData).filter(d -> d.containsKey(Svar) && d.get(Svar)==1).count();
            pS1 = (double) N1 / (N0+N1);
            logger.info("Built biased dataset with P(S=1)="+pS1);

        }

        // Simplify
        if(numDatasets>1)
            extModel = extModel.subModel(extData);
        //System.out.println(extModel.getNetwork());


        int[] trainable = extModel.getExogenousVars();
        if(biased){
            int USvar = extModel.getExogenousParents(Svar)[0];
            trainable = Arrays.stream(extModel.getExogenousVars()).filter(v -> v != USvar).toArray();
        }


        Watch.start();
        builder = EMCredalBuilder.of(extModel, extData)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(executions)
                .setWeightedEM(weighted)
                .setTrainableVars(trainable)
                .setThreshold(threshold)
                .setStopCriteria(stopCriteria)
                .build();



        int[] iter = builder.getTrajectories().stream().mapToInt(t -> t.size()-1).toArray();
        int finalS = numDatasets;
        DataIntegrator finalIntegrator = integrator;
        List selectedPoints =
                builder.getSelectedPoints().stream()
                        .map(m -> {
                                    if(numDatasets>1)
                                        return finalIntegrator.removeInterventionalFromMultiStudy(m, finalS -1);
                                    return m.subModel(model.getVariables());
                                }
                        )
                        .collect(Collectors.toList());

        Watch.stopAndLog(logger, "Finished learning in: ");




        addResults("time_learn", Watch.getTime(), integrator.getDescription());

        /// Inference
        logger.info("Starting inference");
        Watch.start();
        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);

        pnsValues = ((CausalMultiVE) inf).getIndividualPNS(cause, effect, trueState, falseState);
        Watch.stopAndLog(logger, "Finished inference in: ");
        addResults("time_pns", Watch.getTime(), integrator.getDescription());
        for(int i=0; i<pnsValues.length; i++) {
            addResults("pns_"+i, pnsValues[i], integrator.getDescription());
            addResults("iter_"+i, iter[i], integrator.getDescription());

        }

        pns_u = Doubles.max(pnsValues);
        pns_l = Doubles.min(pnsValues);
        logger.info("PNS interval = [" + pns_l + "," + pns_u + "]");


    }




    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new LearnMultiStudy(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }



    public String getLabel() {
        //mIter500_wtrue_sparents3_x20_0
        String str = "";

        modelID = Arrays.stream(this.modelPath.split("/")).reduce((first, second) -> second).get().replace(".uai","_uai");
        str += modelID;
        str += "_"+this.stopCriteria.toString().toLowerCase();
        str += "_th"+String.valueOf(threshold).replace(".","");
        str += "_mIter" + this.maxIter;
        str += "_w" + this.weighted;
        str += "_x" + this.executions;
        str += "_lp" + this.localparam;
        str += "_tps"+String.valueOf(targetPS).replace(".","");
        str += "_" + this.seed;


        return str;
    }



    public void init() throws IOException, CsvException {

        wdir = Paths.get(".").toAbsolutePath();
        RandomUtil.setRandomSeed(seed);
        logger.info("Starting logger with seed "+seed);

        String targetFile = getTargetPath().toString();
        if(!rewrite && new File(targetFile).exists()){
            String msg = "Not rewriting. File exits: "+targetFile;
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

        // Load model
        String fullpath = wdir.resolve(modelPath).toString();
        CausalUAIParser.ignoreChecks = true;
        model = (StructuralCausalModel) IO.readUAI(fullpath);
        logger.info("Loaded model from: "+fullpath);


        int markovianity = CausalGraphTools.getMarkovianity(model.getNetwork());
        logger.debug("Markovianity: "+markovianity);
        if(markovianity!=1)
            throw new IllegalArgumentException("Markovian models are not of interest for this method");

        // Load data
        fullpath = wdir.resolve(modelPath.replace(".uai",".csv")).toString();
        data = DataUtil.fromCSV(fullpath);
        int datasize = data.length;
        logger.info("Loaded "+datasize+" data instances from: "+fullpath);


        fullpath = wdir.resolve(modelPath.replace(".uai","_info.csv")).toString();
        List<HashMap<String, String>> infolist = DataUtil.fromCSVtoStrMap(fullpath);
        if(infolist.size()!=1) throw new IllegalArgumentException("Wrong size for the info file");
        info = infolist.get(0);
        logger.info("Loaded model information from: "+fullpath);

    }



    private void addResults(String name, double value, String key) { results.get(key).put(name, String.valueOf(value));}
    private void addResults(String name, int value, String key) { results.get(key).put(name, String.valueOf(value));}
    private void addResults(String name, long value, String key) { results.get(key).put(name, String.valueOf(value));};
    private void addResults(String name, boolean value, String key) { results.get(key).put(name, String.valueOf(value));};
    private void addResults(String name, String value, String key) { results.get(key).put(name, value);};


    private void addToAllResults(String name, double value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    }
    private void addToAllResults(String name, int value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    }
    private void addToAllResults(String name, long value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    };
    private void addToAllResults(String name, boolean value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    };

    private void addToAllResults(String name, String value) {
        for(String key : results.keySet())
            results.get(key).put(name, value);
    };

    private void processResults(){
        System.out.println();

        addToAllResults("modelPath", modelPath);
        addToAllResults("modelID", modelID);
        addToAllResults("infoPath", modelPath.replace(".uai","_info.csv"));
        addToAllResults("datasize", data.length);

        addToAllResults("stop_criteria", stopCriteria.toString());
        addToAllResults("threshold", threshold);
        addToAllResults("iter_max", maxIter);


        addToAllResults("cause", cause);
        addToAllResults("effect", effect);
        addToAllResults("trueState", trueState);
        addToAllResults("falseState", falseState);
        addToAllResults("localparam", localparam);

        addToAllResults("target_ps1", targetPS);
        addToAllResults("ps1", pS1);


        for(String k : results.keySet())
            addResults("model_type", k, k);





    }

    private Path getTargetPath(){
        return Path.of(this.output, getLabel()+".csv");
    }

    private void save() throws IOException {
        String fullpath = this.wdir.resolve(getTargetPath()).toString();
        logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, results.values().stream().collect(Collectors.toList()));

    }



    }
