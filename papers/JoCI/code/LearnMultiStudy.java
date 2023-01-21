package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.utility.ArraysTools;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.credici.utility.reconciliation.IntegrationChecker;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.jetbrains.annotations.NotNull;
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
-w -x 10 -m 100 -sc KL -th 0.0 --seed 0 ./papers/journalEM/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_2.uai
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

    @CommandLine.Option(names = {"-x", "--executions"}, description = "Number independent EM runs. Only for EM-based methods. Default to 40")
    private int executions = 40;

    @CommandLine.Option(names = {"-th", "--threshold"}, description = "KL threshold for stopping EM execution. Default to 0.0")
    private double threshold = 0.0;


    @CommandLine.Option(names = {"-sc", "--stopcriteria"}, description = "Stopping criteria: ${COMPLETION-CANDIDATES}")
    private FrequentistCausalEM.StopCriteria stopCriteria = FrequentistCausalEM.StopCriteria.KL;

    @CommandLine.Option(names = {"-nh", "--numhidden"}, description = "Number of hidden states due to selection bias. Default to 0 (No bias)")
    private int numHidden = 0;


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

    }

    @Override
    protected void entryPoint() throws Exception {
        init();
        buildAndLearn();
        learn();
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

    public void buildIntegrationModel() throws InterruptedException {

        selectCauseEffect();
        logger.info("Set cause="+cause+" effect="+effect+" Z="+Z+" W="+W);
        boolean isCofounded = CausalGraphTools.isCofounded(model.getNetwork(), cause);
        boolean dseparated = DAGUtil.dseparated(model.getNetwork(), model.getExogenousParents(cause)[0], effect);
        if(!isCofounded || dseparated)
            throw new IllegalArgumentException("Error setting cause/effect");


        if(!List.of(cause,effect,Z,W).stream().sequential().allMatch(new HashSet<>()::add))
            throw new IllegalArgumentException("Error setting cause/effect");

// TODO: POR AQUÍ

        Random r = RandomUtil.getRandom();
        RandomUtil.setRandomSeed(0);
        TIntIntMap[] dataX = model.samplesIntervened(data.length / 2, cause, model.getEndogenousVars());
        TIntIntMap[] dataZ = model.samplesIntervened(data.length / 2, Z, model.getEndogenousVars());
        RandomUtil.setRandom(r);

        // Build integrator objects
        integrators = new ArrayList<>();
        integrators.add(getMultiStudyIntegrator(data,dataX,dataZ,"DzDxDobs").compile());
        integrators.add(getMultiStudyIntegrator(null,dataX,dataZ,"DzDx").compile());
        integrators.add(getSingleStudyIntegrator(null, dataZ, Z, "Dz").compile());

        logger.info("Built data integrators");
        for (DataIntegrator I : integrators) {
            results.put(I.getDescription(), new HashMap<String, String>());
            logger.debug(I.toString());
        }
    }


    @NotNull
    private DataIntegrator getMultiStudyIntegrator(TIntIntMap[] dataObs, TIntIntMap[] dataInterX, TIntIntMap[] dataInterZ, String descr) {
        DataIntegrator integrator = DataIntegrator.of(model, model.getExogenousParents(W)[0]);
        //integrator.setData(dataInterX2, new int[]{X}, 0);
        int s = 0;
        if(dataInterZ !=null) {
            integrator.setData(dataInterZ, new int[]{Z}, s);
            s++;
        }
        if(dataInterX !=null) {
            integrator.setData(dataInterX, new int[]{cause}, s);
            s++;
        }
        if(dataObs !=null)
            integrator.setObservationalData(dataObs, s);

        integrator.setDescription(descr);
        return integrator;
    }
    @NotNull
    private  DataIntegrator getSingleStudyIntegrator(TIntIntMap[] dataObs, TIntIntMap[] dataInter, int interVar, String descr) {
        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataInter !=null)
            integrator.setData(dataInter, new int[]{interVar});

        if(dataObs !=null)
            integrator.setObservationalData(dataObs);
        integrator.setDescription(descr);
        return integrator;
    }


    protected void learn() throws InterruptedException, ExecutionControl.NotImplementedException {

        for(DataIntegrator I : this.integrators) {
            logger.debug(I.toString());
            boolean studySpecific = I.hasStudySpecificExoVars();
            StructuralCausalModel extModel = I.getExtendedModel(studySpecific);
            TIntIntMap[] extData = I.getExtendedData(studySpecific);
            if(studySpecific)
                extModel = extModel.subModel(extData);
            logger.debug("extData variables: "+Arrays.toString(DataUtil.variables(extData)));
            logger.debug("extModel: "+extModel.getNetwork());


            int[] trainable = extModel.getExogenousVars();

            if(numHidden>0) {

                int zbranch = I.getInterventionPosition(Z);
                int[] Sparents = I.getMap(false).getEquivalentVars(zbranch, cause, Z, effect);

                int numAssignments = extModel.getDomain(Sparents).getCombinations();
                int[] assignements =
                        //CollectionTools.shuffle(
                                Ints.concat(
                                ArraysTools.ones(numAssignments - numHidden),
                                ArraysTools.zeros(numHidden)
                                );
                        //);

                extModel = SelectionBias.addSelector(extModel, Sparents, assignements);
                Svar = SelectionBias.findSelector(extModel);
                int USvar = extModel.getExogenousParents(Svar)[0];
                trainable = Arrays.stream(extModel.getExogenousVars()).filter(v -> v != USvar).toArray();
                extData = SelectionBias.applySelector(extData, extModel, Svar);
                int n0 = (int) Stream.of(extData).filter(d -> d.get(Svar) == 0).count();
                int n1 = data.length - n0;
                pS1 = (1.0 * n1) / data.length;
                logger.info("Set up selection bias selector (S="+Svar+"): p(S=1)="+pS1);
                logger.debug("N0 = "+n0+" N1 = "+n1);

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

            List selectedPoints = builder.getSelectedPoints().stream().map(m -> cleanOutputModel(I, m)).collect(Collectors.toList());
            int[] iter = builder.getTrajectories().stream().mapToInt(t -> t.size()-1).toArray();

            CausalMultiVE inf = new CausalMultiVE(selectedPoints);

            Watch.stopAndLog(logger, "Finished learning in: ");
            addResults("time_learn", Watch.getTime(), I.getDescription());

            /// Inference
            logger.info("Starting inference");
            Watch.start();
            pnsValues = ((CausalMultiVE) inf).getIndividualPNS(cause, effect, trueState, falseState);
            Watch.stopAndLog(logger, "Finished inference in: ");
            addResults("time_pns", Watch.getTime(), I.getDescription());
            for(int i=0; i<pnsValues.length; i++) {
                addResults("pns_"+i, pnsValues[i], I.getDescription());
                addResults("iter_"+i, iter[i], I.getDescription());

            }

            pns_u = Doubles.max(pnsValues);
            pns_l = Doubles.min(pnsValues);
            logger.info("PNS interval = [" + pns_l + "," + pns_u + "]");
        }
    }

    private StructuralCausalModel cleanOutputModel(DataIntegrator I, StructuralCausalModel m) {
        if(numHidden>0) {
            m.removeVariable(m.getExogenousParents(Svar)[0]);
            m.removeVariable(Svar);
        }
        if(!I.hasStudySpecificExoVars()) {
            return I.removeInterventional(m);
        }
        return I.removeInterventionalFromMultiStudy(m, 0);
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
        str += "_nh" + this.numHidden;
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

        addToAllResults("num_hidden", numHidden);
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
