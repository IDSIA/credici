package repo.experiments;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.utility.*;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
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
import java.util.HashMap;
import java.util.List;

import static ch.idsia.credici.utility.Assertion.assertTrue;

/*

Parameters CLI:
-w -x 20 -a CCVE --seed 0 ./papers/journalEM/models/synthetic/s1/set4/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai
-w -x 2 -m 500 -sc LLratio -th 0.999999 -a EMCC --seed 0 ./papers/journalEM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai
-w -rw -x 2 -ii 100 -m 500 -sc LLratio -th 0.999999 -a EMCC --debug --seed 0 --cause 4 ./papers/journalEM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai
-w -rw -x 1 -ii 5 -m 500 -sc LLratio -th 0.5 -a EMCC --debug --seed 5 --cause 3 --effect 0 ./papers/journalEM/models/triangolo/triangolo_causal.uai
-w -rw -x 1 -ii 5 -m 1 -sc KL -th 0.0 -a EMCC --debug --seed 5 --cause 3 --effect 0 /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/models/triangolo/triangolo_causal.uai

-rw -w -x 10 -m 100 -a EMCC --debug --seed 0 ./papers/journalEM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai
* */


public class BoundPNS extends Terminal {

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

    @CommandLine.Option(names = {"-ii", "--initindex"}, description = "Initial index for the results. Default to 0")
    private int initIndex = 0;

    @CommandLine.Option(names = {"-th", "--threshold"}, description = "KL threshold for stopping EM execution. Default to 0.0")
    private double threshold = 0.0;

    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Learning and inference algorithm: ${COMPLETION-CANDIDATES}")
    private algorithms alg = algorithms.CCVE;

    @CommandLine.Option(names = {"-c", "--cause"}, description = "Cause in the PNS query. Default to the 1st node in the topological order.")
    private int inputCause = -1;
    @CommandLine.Option(names = {"-e", "--effect"}, description = "Effect in the PNS query. Default to the last node in the topological order.")
    private int inputEffect = -1;


    @CommandLine.Option(names = {"-sc", "--stopcriteria"}, description = "Stopping criteria: ${COMPLETION-CANDIDATES}")
    private FrequentistCausalEM.StopCriteria stopCriteria = FrequentistCausalEM.StopCriteria.KL;



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

    String modelID = "";

    int cause = -1, effect = -1;
    int trueState = 0, falseState = 1;

    CausalInference inf = null;
    double pns_l, pns_u;
    double[] pnsValues;
    EMCredalBuilder builder = null;

    HashMap results = new HashMap<String, String>();

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
        makeInference();
        processResults();
        save();



    }

    protected void learn() throws InterruptedException, ExecutionControl.NotImplementedException {

        HashMap empirical = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, data),5);

        logger.debug("Empirical from data: "+empirical);
        logger.info("Learning exogenous variables with algorithm: "+alg);

        Watch.start();
        if(alg == algorithms.CCVE){
            inf = new CredalCausalVE(model, empirical.values());
        }else if(alg == algorithms.CCALP){
            //throw new ExecutionControl.NotImplementedException("Method not implemented yet");
            inf = new CredalCausalApproxLP(model, empirical.values());
        }else if(alg== algorithms.EMCC){
            builder = EMCredalBuilder.of(model, data)
                    .setMaxEMIter(maxIter)
                    .setNumTrajectories(executions)
                    .setWeightedEM(weighted)
                    .setTrainableVars(model.getExogenousVars())
                    .setThreshold(threshold)
                    .setStopCriteria(stopCriteria)
                    .build();
            inf = new CausalMultiVE(builder.getSelectedPoints());


        }


        Watch.stopAndLog(logger, "Finished learning in: ");
        addResults("time_learn", Watch.getTime());


    }
    public void makeInference() throws ExecutionControl.NotImplementedException, InterruptedException {

        logger.info("Starting inference");
        getCauseEffect();

        Watch.start();
        if(inf instanceof CausalMultiVE)
            pnsValues = ((CausalMultiVE) inf).getIndividualPNS(cause, effect, trueState, falseState);
        else{
            GenericFactor pns = inf.probNecessityAndSufficiency(cause, effect, trueState, falseState);
            if(pns instanceof VertexFactor)
                pnsValues = Doubles.concat(((VertexFactor) pns).getData()[0]);
            else
                pnsValues = Doubles.concat(((IntervalFactor)pns).getLower(), ((IntervalFactor)pns).getUpper());
        }

        Watch.stopAndLog(logger, "Finished inference in: ");
        addResults("time_pns", Watch.getTime());

        pns_u = Doubles.max(pnsValues);
        pns_l = Doubles.min(pnsValues);
        logger.info("PNS interval = ["+pns_l+","+pns_u+"]");

        logger.info("Total time: "
                +(Long.parseLong((String) results.get("time_learn"))
                +Long.parseLong((String) results.get("time_pns"))
        )+" ms.");


    }

    private void getCauseEffect() {

        int[] order = DAGUtil.getTopologicalOrder(model.getNetwork(), model.getEndogenousVars());
        if(inputCause<0) {
            cause = order[0];
            logger.info("Determining cause=" + cause);
        }else{
            cause = inputCause;
            logger.info("From arguments cause=" + cause);
        }
        if(inputEffect<0) {
            effect = order[order.length - 1];
            logger.info("Determining effect=" + effect);
        }else{
            effect = inputEffect;
            logger.info("From arguments effect=" + effect);
        }
        logger.info("Determining query: cause=" + cause + ", effect=" + effect);
    }


    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new BoundPNS(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }



    private String getLabel(algorithms alg) {
        //mIter500_wtrue_sparents3_x20_0
        String str = "";

        modelID = Arrays.stream(this.modelPath.split("/")).reduce((first, second) -> second).get().replace(".uai","_uai");
        str += modelID;
        str += "_"+alg.toString().toLowerCase();
        if(alg == algorithms.EMCC) {
            str += "_"+this.stopCriteria.toString().toLowerCase();
            str += "_th"+String.valueOf(threshold).replace(".","");
            str += "_mIter" + this.maxIter;
            str += "_w" + this.weighted;
            str += "_x" + this.executions;

        }
        if(inputCause>=0) str += "_c"+inputCause;
        if(inputEffect>=0) str += "_e"+inputEffect;



        if(alg != algorithms.CCVE) {
            str += "_" + this.seed;
        }

        return str;
    }

    protected String getLabel(){
        return getLabel(this.alg);
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
        if(markovianity>1 && alg != algorithms.EMCC)
            throw new IllegalArgumentException("A model of this markovianity cannot be solved with this method.");

        // Load data
        fullpath = wdir.resolve(modelPath.replace(".uai",".csv")).toString();
        data = DataUtil.fromCSV(fullpath);
        int datasize = data.length;
        logger.info("Loaded "+datasize+" data instances from: "+fullpath);


        fullpath = wdir.resolve(modelPath.replace(".uai","_info.csv")).toString();
        if(new File(fullpath).exists()) {
            List<HashMap<String, String>> infolist = DataUtil.fromCSVtoStrMap(fullpath);
            if (infolist.size() != 1) throw new IllegalArgumentException("Wrong size for the info file");
            info = infolist.get(0);
            logger.info("Loaded model information from: " + fullpath);
        }
        // initialize results
        results = new HashMap<String,String>();
    }



    private void addResults(String name, double value) { results.put(name, String.valueOf(value));}
    private void addResults(String name, int value) { results.put(name, String.valueOf(value));}
    private void addResults(String name, long value) { results.put(name, String.valueOf(value));};
    private void addResults(String name, boolean value) { results.put(name, String.valueOf(value));};

    private void addResults(String name, int[] values){
        for(int i=0; i<values.length; i++) addResults(name+(i+initIndex), values[i]);
    }
    private void addResults(String name, double[] values){
        for(int i=0; i<values.length; i++) addResults(name+(i+initIndex), values[i]);
    }

    private void processResults(){

        results.put("method", alg);
        results.put("modelPath", modelPath);
        results.put("modelID", modelID);
        results.put("infoPath", modelPath.replace(".uai","_info.csv"));
        results.put("exact", alg== algorithms.CCVE);

        if(alg != algorithms.CCVE){
            try {
                String exactPath = Path.of(this.output, getLabel(algorithms.CCVE)+".csv").toString();
                String fullpath = wdir.resolve(exactPath).toAbsolutePath().toString();
                results.put("exactPath", exactPath);
                logger.info("Checking exact results at:"+fullpath);
                HashMap<String, String> exactResults = DataUtil.fromCSVtoStrMap(fullpath).get(0);

                logger.info("Loaded exact results: ["+exactResults.get("pns_l")+","+exactResults.get("pns_u")+"]");
            }catch (Exception e) {
                logger.warn("Exact results not found.");
            }
        }

        if(alg== algorithms.EMCC){

            int[] iter = builder.getTrajectories().stream().mapToInt(t -> t.size()-1).toArray();
            double avgTrajectorySize = Arrays.stream(iter).average().getAsDouble();
            logger.info("Average trajectory size: "+avgTrajectorySize);

            //Store trajectory sizes
            addResults("iter_", iter);
            addResults("pns_", pnsValues);

            // maximum possible log-likelihood
            addResults("ll_max", Probability.maxLogLikelihood(model, data));
            // Add individual log-likelihoods and ratios
            List<StructuralCausalModel> selectedPoints = builder.getSelectedPoints();
            List ratios = new ArrayList();
            List ll = new ArrayList();

            for(int i=0; i<selectedPoints.size(); i++) {
                try {
                    ratios.add(selectedPoints.get(i).ratioLogLikelihood(data));
                    ll.add(selectedPoints.get(i).logLikelihood(data));
                }catch (Exception e) {
                    logger.warn("Cannot calculate the Likelihood for this model");
                }
            }

            if(ratios.size()>0)
                logger.debug("Average ratio: "+ Arrays.stream(CollectionTools.toDoubleArray(ratios)).average().getAsDouble());

            addResults("ratio_", CollectionTools.toDoubleArray(ratios));
            addResults("ll_", CollectionTools.toDoubleArray(ll));
            addResults("datasize", data.length);

            results.put("stop_criteria", stopCriteria.toString());
            addResults("threshold", threshold);
            addResults("iter_max", maxIter);
        }

        addResults("cause", cause);
        addResults("effect", effect);
        addResults("trueState", trueState);
        addResults("falseState", falseState);
        addResults("pns_u", pns_u);
        addResults("pns_l", pns_l);
    }

    private Path getTargetPath(){
        return Path.of(this.output, getLabel()+".csv");
    }

    private void save() throws IOException {
        String fullpath = this.wdir.resolve(getTargetPath()).toString();
        logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, List.of(results));

    }



    }
