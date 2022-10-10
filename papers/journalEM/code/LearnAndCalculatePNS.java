package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.collect.Iterables;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static ch.idsia.credici.utility.Assertion.assertTrue;
import static ch.idsia.credici.utility.Assertion.assertTrueWarning;
import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

/*

Parameters CLI:
-w -x 20 -a CCVE --seed 0 ./papers/journalEM/models/synthetic/s1/set4/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai

* */


    /*

    save models
    see how to decide X and Y.... check if topological order is deterministic
    measure learning/inference time


    * */


public class LearnAndCalculatePNS extends Terminal {




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

    @CommandLine.Option(names = {"-x", "--executions"}, description = "Number independent EM runs. Only for EM-based methods. Default to 40")
    private int executions = 40;

    @CommandLine.Option(names = {"-th", "--klthreshold"}, description = "KL threshold for stopping EM execution. Default to 0.0")
    private double klthreshold = 0.0;

    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Learning and inference algorithm: ${COMPLETION-CANDIDATES}")
    private algorithms alg = algorithms.CCVE;


    public enum algorithms {
        CCVE,   // Credal causal VE
        CCALP,  // Credal causal approx LP
        EMCC,   // EM

    }

    /// Global ///
    TIntIntMap[] data = null;
    StructuralCausalModel model = null;
    Path wdir = null;
    HashMap<String, String> info;

    int cause, effect;
    List<HashMap> results = null;
    CausalInference inf = null;

    @Override
    protected void checkArguments() {
        logger.info("Checking arguments");
        if(alg == algorithms.EMCC) {
            assertTrue(maxIter > 0, " Wrong value for maxIter: " + maxIter);
            assertTrue(executions > 0, " Wrong value for maxIter: " + executions);
        }


    }

    @Override
    protected void entryPoint() throws Exception {
        init();
        learn();
        //save();

    }

    protected void learn() throws InterruptedException, ExecutionControl.NotImplementedException {

        HashMap empirical = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, data),5);

        logger.debug("Empirical from data: "+empirical);
        logger.info("Learning exogenous variables with algorithm: "+alg);

        Watch.start();
        if(alg == algorithms.CCVE){
            inf = new CredalCausalVE(model, empirical.values());
        }else if(alg == algorithms.CCALP){
            inf = new CredalCausalApproxLP(model, empirical.values());
        }else if(alg== algorithms.EMCC){
            EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                    .setMaxEMIter(maxIter)
                    .setNumTrajectories(executions)
                    .setWeightedEM(weighted)
                    .setTrainableVars(model.getExogenousVars())
                    .setKlthreshold(klthreshold)
                    .build();

            inf = new CausalMultiVE(builder.getSelectedPoints());

            for(StructuralCausalModel m : builder.getTrajectories().get(0)) {
            //    System.out.println(m.getFactors(m.getExogenousVars()));
            }

            double avgTrajectorySize = builder.getTrajectories().stream().mapToInt(t -> t.size()).average().getAsDouble();
            logger.info("Average trajectory size: "+avgTrajectorySize);

        }

        Watch.stopAndLog(logger, "Learning finished in ");

        System.out.println(inf.probNecessityAndSufficiency(cause, effect));



    }

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new LearnAndCalculatePNS(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    protected String getLabel(){
        //mIter500_wtrue_sparents3_x20_0
        String str = "";
        str += "_mIter"+this.maxIter;
        str += "_w"+this.weighted;
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

        int[] order = DAGUtil.getTopologicalOrder(model.getNetwork(), model.getEndogenousVars());
        cause = order[0];
        effect = order[order.length-1];
        logger.info("Determining query: cause="+cause+", effect="+effect);

        // initialize results
        results = new ArrayList<HashMap>();
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

        String filename = Iterables.getLast(Arrays.asList(this.modelPath.split("/"))).replace(".uai",getLabel());
        String fullpath = this.wdir.resolve(this.output).resolve(filename+".csv").toString();
        logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, results);


    }



    }
