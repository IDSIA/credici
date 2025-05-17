package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.*;
import ch.idsia.credici.utility.experiments.ResultsManager;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

/*


TODO: compute queries

--output ./dev_ig/zeros_eqscaru/models -rw -s 1234


*/
//
public class GenerateSM extends Terminal {



    @CommandLine.Option(names = {"-nzr", "--nzerorate"}, description = "Rate of non-zeros in P(U). Default to 1.0")
    private  double nzerorate = 1.0;

    @CommandLine.Option(names = {"-zdr", "--zerodroprate"}, description = "Probability of keeping a zero configuration. Default to 1.0")
    private  double zerodroprate = 1.0;

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names={"-rw", "--rewrite"}, description = "If activated, results are rewritten. Otherwise, process is stopped if there are existing results.")
    boolean rewrite = false;

    //@CommandLine.Option(names={"-fs", "--forcesolution"}, description = "If activated, model is saved even if non solution is available.")
    //boolean forceSolution = false;



    /// Global ///
    TIntIntMap[] data = null;
    StructuralCausalModel m = null;
    Path wdir = null;

    String label = "";


    Path outputFolder = null;
    Path outputFile = null;


    int idinfo = 0;
    ResultsManager info = null;

    int X = 0;
    int Y1 = 1, Y2=2;



    @Override
    protected void entryPoint() throws Exception {
        init();
        generate();
        inferenceLoop();
        save();

    }


    protected void inferenceLoop() throws InterruptedException {
        boolean sampled = false;
        int size = 1000;

        do{
            try {
                sampleData(size);
                inference();
                sampled = true;



            }catch (Exception e){
                logger.info("No feasible solution... resampling");

                if(size>=10000)
                    throw new IllegalStateException("Unable to generate compatible data");

                size += 100;


            }
        }while (!sampled);



    }

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new GenerateSM(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    public void generate() throws IOException, CsvException, InterruptedException {



        String arcs = "(0,1),(1,2)";
        SparseDirectedAcyclicGraph dag = DAGUtil.build(arcs);


        int[] endoVarSizes = IntStream.range(0,dag.getVariables().length).map(x -> 2).toArray();

        m = CausalBuilder.of(dag,endoVarSizes).build();
        m = Cofounding.mergeExoParents(m, Y1,Y2);
        m.fillExogenousWithRandomFactors(3);
        int u  = m.getExogenousParents(Y1)[0];
        zeroPerturbation(u);
        logger.info(String.valueOf(m));
        logger.info(String.valueOf(m.getNetwork()));

    }

    private void sampleData(){
        boolean sampled = false;
        int size = 1000;

        do{
            try {
                sampleData(size);
                sampled = true;
            }catch (Error e){
                logger.info("No feasible solution... resampling");
            }


        }while (!sampled);
    }

    private void sampleData(int size) {
        boolean zeroX = false;
        for(int i = 0; i<10; i++){
            data = m.samples(size, m.getEndogenousVars());
            BayesianFactor p = DataUtil.getJointProb(data, m.getDomain(m.getEndogenousVars()));
            p = p.marginalize(Y1,Y2);
            zeroX = ArraysUtil.where(p.getData(), v -> v==0).length>0;
            if(!zeroX)
                break;
        }
        if(zeroX) throw new IllegalStateException("Conditioning on zero values");

        logger.info("Sampled "+data.length+" data instances");
    }

    private void inference() throws InterruptedException {
        logger.info("Starting exact inference ");


        // Exact solution
        Watch.start();
        CredalCausalVE ccve = new CredalCausalVE(m, data);
        long tlearn = Watch.stop();

        logger.info("Learning time: "+tlearn+" ms.");


        for(int x=0; x<2; x++) {
            for(int y=x+1; y<=2; y++) {
                Watch.start();
                VertexFactor res = ccve.probSufficiency(x, y, 1, 0, 1, 0);
                long tinfer = Watch.stop();
                double[] bounds = new double[]{Arrays.stream(Doubles.concat(res.getData()[0])).min().getAsDouble(),
                        Arrays.stream(Doubles.concat(res.getData()[0])).max().getAsDouble()};
                logger.info("PS(V" + x + ",V" + y + ") in " + Arrays.toString(bounds) + " in "+tinfer+" ms.");
                addQueryInfo("PS", x, y, bounds, tlearn, tinfer);
            }
        }

        for(int x=0; x<2; x++) {
            for(int y=x+1; y<=2; y++) {
                Watch.start();
                VertexFactor res = ccve.probNecessity(x, y, 1, 0, 1, 0);
                long tinfer = Watch.stop();
                double[] bounds = new double[]{Arrays.stream(Doubles.concat(res.getData()[0])).min().getAsDouble(),
                        Arrays.stream(Doubles.concat(res.getData()[0])).max().getAsDouble()};
                logger.info("PN(V" + x + ",V" + y + ") in " + Arrays.toString(bounds) + " in "+tinfer+" ms.");
                addQueryInfo("PN", x, y, bounds, tlearn, tinfer);
            }
        }


 /*       // Exact solution


        for(int i = 1; i<= numParents; i++) {
            Watch.start();
            VertexFactor res = ccve.probSufficiency(i, Y, 1,0,1,0);
            long tinfer = Watch.stop();
            double[] bounds = new double[]{Arrays.stream(Doubles.concat(res.getData()[0])).min().getAsDouble(),
                    Arrays.stream(Doubles.concat(res.getData()[0])).max().getAsDouble()};
            logger.info("PS(V"+i+",V"+Y+") in " + Arrays.toString(bounds) + "");
            addQueryInfo("PS", i, Y, bounds, tlearn, tinfer);
        }

        for(int i = 1; i<= numParents; i++) {
            Watch.start();
            VertexFactor res = ccve.probNecessity(i, Y, 1,0,1,0);
            long tinfer = Watch.stop();

            double[] bounds = new double[]{Arrays.stream(Doubles.concat(res.getData()[0])).min().getAsDouble(),
                    Arrays.stream(Doubles.concat(res.getData()[0])).max().getAsDouble()};
            logger.info("PN(V"+i+",V"+Y+") in " + Arrays.toString(bounds) + " ");
            addQueryInfo("PN", i, Y, bounds, tlearn, tinfer);
        }*/
    }

    private void zeroPerturbation(int u) {

        logger.info("U cardinality: "+m.getDomain(u).getCardinality(u));


        // Add some zeros to P(U)
        double[] values = m.getFactor(u).getData();
        for(int i = 0; i< values.length*(1- nzerorate); i++) values[i] = 0;
        values = CollectionTools.shuffle(values);
        values = ArraysUtil.roundNonZerosToTarget(values, 1.0, 3);
        m.setFactor(u, new BayesianFactor(m.getDomain(u), values));


        // Removing zero positions
        BayesianFactor f = m.getFactor(u);
        logger.info("!0 = " + Arrays.toString(ArraysUtil.where(f.getData(), x -> x != 0)));
        logger.info("0 = " + Arrays.toString(ArraysUtil.where(f.getData(), x -> x == 0)));
        int[] zeroPos = ArraysUtil.reverse(ArraysUtil.where(f.getData(), x -> x == 0));
        ArrayList removedPos = new ArrayList();


        for (int s : zeroPos) {
            if (zerodroprate > RandomUtil.getRandom().nextFloat()) {
                m = m.dropExoState(u, s);
                removedPos.add(s);
                logger.info("Dropping state "+s);
            }
        }

        values = f.getData();
        double[] finalValues = values;
        values = IntStream.range(0, f.getData().length).filter(i -> !removedPos.contains(i)).mapToDouble(i -> finalValues[i]).toArray();
        f = new BayesianFactor(m.getDomain(u), values);
        m.setFactor(u,f);

        logger.info("U cardinality: "+m.getDomain(u).getCardinality(u));

    }


    public void init() throws IOException, CsvException, ExecutionControl.NotImplementedException, InterruptedException {

        wdir = Paths.get(".");
        RandomUtil.setRandomSeed(seed);
        logger.info("Starting logger with seed " + seed);

        label = "semi_nzr"+String.valueOf(nzerorate).replace(".","")+"" +
                "_zdr"+String.valueOf(zerodroprate).replace(".","")+"";

        label +="_"+seed;
        logger.info(label);

        outputFolder  = Path .of(output, "/");
        File f = new File(outputFolder.toUri());
        if (!f.exists()) {
            String msg = "Creating folder: " + outputFolder;
            f.mkdirs();
        }

        outputFile = outputFolder.resolve(label+".csv");
        if(!rewrite && new File(output).exists()){
            String msg = "Not rewriting. File exits: "+outputFile;
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

    }


    public void addQueryInfo(String query, int i, int Y, double[] bounds, long tlearn, long tinfer) {

        if(info==null) info = new ResultsManager().setIncludeLabel(false);

        info.addExperiment(String.valueOf(idinfo));
        info.add(String.valueOf(idinfo), "query", query);
        info.add(String.valueOf(idinfo), "cause", "V"+ i);
        info.add(String.valueOf(idinfo), "effect", "V"+ Y);
        info.add(String.valueOf(idinfo), "low", bounds[0]);
        info.add(String.valueOf(idinfo), "upp", bounds[1]);
        info.add(String.valueOf(idinfo), "tlearn", tlearn);
        info.add(String.valueOf(idinfo), "tinfer", tinfer);



        idinfo++;
    }

    public void save() throws IOException {


        String outputfile = null;

        outputfile = outputFolder.resolve(label+".csv").toString();
        DataUtil.toCSV(outputfile,data);
        logger.info("Saved data to: "+outputfile);

        outputfile = outputFolder.resolve(label+".uai").toString();
        IO.writeUAI(m, outputfile);
        logger.info("Saved model to: "+outputfile);

        outputfile = outputFolder.resolve(label+"_query.csv").toString();
        if(info != null)
            info.save(outputfile);
            logger.info("Saved queries to: "+outputfile);


    }



}
