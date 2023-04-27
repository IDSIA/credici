package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.StatisticsModel;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.model.transform.ExogenousReduction;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompatibleCausalModelGenerator {

    // Parameters
    private boolean DAGfromBNG = true;

    /** Number of nodes in the first DAG .*/
    private int numNodes = 5;

    private int markovianity = -1;  // -1 not-fixed, 0 markovian, 1 quasi-markovian, 2 non-markovian

    /** Parameter in case of DAGfromBNG=false*/
    private int lambda = 3;

    private int maxIndegree = 3;

    private int datasize = 1000;

    private int maxDataResamples = 5;

    private int maxExoFactorsResamples = 5;

    private int maxModelResamples = Integer.MAX_VALUE;

    private int maxCofoundedVars = 2;

    private double minCompatibilityDegree = 0.9;

    /** if 0 it remains the same, if 1.0, the same amount is added*/
    private double dataIncrementFactor = 0.5;


    private double reductionK = 1.0;


    // Global variables
    private StructuralCausalModel model = null;

    TIntIntMap[] data = null;

    private List<int[]> mergeExoPairs = new ArrayList<>();

    private Logger logger = Logger.getGlobal();

    private double ratio = 0.0;

    private StatisticsModel stats = null;

    // Add getters and setters here
    public StructuralCausalModel getModel() {
        return model;
    }

    public TIntIntMap[] getData() {
        return data;
    }

    public StatisticsModel getStatistics(){
        return this.stats;
    }

    public CompatibleCausalModelGenerator setLambda(int lambda) {
        this.lambda = lambda;
        return this;
    }

    public CompatibleCausalModelGenerator setNumNodes(int numNodes) {
        this.numNodes = numNodes;
        return this;
    }

    public CompatibleCausalModelGenerator setMaxIndegree(int maxIndegree) {
        this.maxIndegree = maxIndegree;
        return this;
    }

    public CompatibleCausalModelGenerator setDatasize(int datasize) {
        this.datasize = datasize;
        return this;
    }

    public CompatibleCausalModelGenerator setMaxDataResamples(int maxDataResamples) {
        this.maxDataResamples = maxDataResamples;
        return this;
    }

    public CompatibleCausalModelGenerator setMaxExoFactorsResamples(int maxExoFactorsResamples) {
        this.maxExoFactorsResamples = maxExoFactorsResamples;
        return this;
    }

    public CompatibleCausalModelGenerator setMaxModelResamples(int maxModelResamples) {
        this.maxModelResamples = maxModelResamples;
        return this;
    }

    public CompatibleCausalModelGenerator setMarkovianity(int markovianity) {
        this.markovianity = markovianity;
        return this;
    }

    public CompatibleCausalModelGenerator setMaxCofoundedVars(int maxCofoundedVars) {
        this.maxCofoundedVars = maxCofoundedVars;
        return this;
    }

    public CompatibleCausalModelGenerator setMinCompatibilityDegree(double minCompatibilityDegree) {
        this.minCompatibilityDegree = minCompatibilityDegree;
        return this;
    }

    public CompatibleCausalModelGenerator setDataIncrementFactor(double dataIncrementFactor) {
        this.dataIncrementFactor = dataIncrementFactor;
        return this;
    }

    // Generation loop
    public void run(){
        Boolean compatibleFound = false;

        modelLoop:
        for(int j=0; j<maxModelResamples; j++) {
            SparseDirectedAcyclicGraph causalDAG = generateDAG();
            initializeModel(causalDAG);
            removeRedundancies();


            factorLoop:
            for (int k = 0; k < maxExoFactorsResamples; k++) {
                sampleExoFactors();
                for (int i = 0; i < maxDataResamples; i++) {
                    sampleData(i);
                    compatibleFound = checkCompatibility();

                    if(compatibleFound==null)
                        break factorLoop;

                    if (compatibleFound.booleanValue()) {
                        logger.info("Found compatible model and data");
                        break modelLoop;
                    } else{
                        logger.debug("Model is not compatible.");
                    }
                }
            }
        }
        if(!compatibleFound)
            throw new IllegalStateException("Compatibility not found for current parameters");

        // Reduce
        reduceModel();

        // Print statistics of the model

        stats = StatisticsModel.of(model,data);
        stats.logStatistics(logger);

        // todo: add here additional operations, e.g. store to disk, calculate query (this proably should not be placed in this class)...

    }


    /**
     *  Steps 1,2 and 3: this will generate a DAG and make it valid for SCM
     * */
    private SparseDirectedAcyclicGraph generateDAG(){

        SparseDirectedAcyclicGraph dag = null;
        boolean repeat;
        int markObtained;

        do {

            repeat = false;

            if (DAGfromBNG) {
                dag = DAGUtil.randomFromBNGenerator(numNodes, maxIndegree);
            } else {
                dag = DAGUtil.random(numNodes, lambda, maxIndegree);
            }


            // step 2,3
            if (!CausalGraphTools.isValidCausalDAG(dag)) {
                logger.debug("Non-valid sampled DAG before transformation: " + dag);
                dag = CausalGraphTools.makeValidCausalDAG(dag);
            }
            markObtained = CausalGraphTools.getMarkovianity(dag);


            if(markovianity >=0 && markovianity != markObtained) {
                logger.debug("Sampled DAG: " + dag);
                logger.debug("Markovianity obtained is "+markObtained+"while desired is "+markovianity+". Resampling DAG.");
                repeat = true;
            }

            SparseDirectedAcyclicGraph finalDag = dag;
            int maxCof = Arrays.stream(CausalGraphTools.getExogenous(dag)).map(u -> finalDag.getChildren(u).length).max().getAsInt();
            if(maxCof>maxCofoundedVars){
                logger.debug("Maximum cofounded variables is "+maxCof+" while it should be "+maxCofoundedVars);
                repeat = true;
            }


        }while(repeat);
        markovianity = markObtained;
        //if(markovianity>1)
        //    throw new NotImplementedException("Code not implemented for markovianity "+markovianity);

        if(markovianity != 0){
            logger.debug("Splitting dag DAG: "+dag);
            mergeExoPairs = new ArrayList<>();
            dag = Cofounding.splitExoCofounders(dag, mergeExoPairs);
            logger.info("Exo vars to merge: "+mergeExoPairs.stream().map(s -> Arrays.toString((int[]) s)).collect(Collectors.toList()));
        }



        logger.info("Sampled DAG: " + dag);
        logger.info("Markovianity: "+markObtained);


        return dag;
    }


    /* Steps 4 to 6 */
    private void initializeModel(SparseDirectedAcyclicGraph causalDAG) {
        //todo: set this.model
        logger.info("Initializing SCM ");

        SparseDirectedAcyclicGraph endoDAG = DAGUtil.getSubDAG(causalDAG, CausalGraphTools.getEndogenous(causalDAG));

        //
        model = CausalBuilder.of(endoDAG, 2)
                .setCausalDAG(causalDAG)
                .setEmptyFactors(false)
                .setMaxExoCardNQM(16)
                .build();

        if(mergeExoPairs.size()>0)
            model = Cofounding.mergeExoVars(model, mergeExoPairs.toArray(int[][]::new));

        // This factors will later be resampled, just for avoiding problems with null objects.
        model.fillExogenousWithRandomFactors();

        logger.info("Initialized SCM structure");
        logger.info("Exo CC: "+ model.exoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));
        logger.info("Endo CC: "+model.endoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));


    }



    /*Step 7*/
    private void sampleExoFactors(){
        logger.info("Sampling exogenous factors");
        model.fillExogenousWithRandomFactors();

        logger.debug("Sampled SCM:"+model);

    }

    private void removeRedundancies(){

        double cardBefore, cardAfter;
        cardBefore=StatisticsModel.of(model).avgExoCardinality();
        ExogenousReduction reducer = new ExogenousReduction(this.model).removeRedundant();
        model = reducer.getModel();
        cardAfter = StatisticsModel.of(model).avgExoCardinality();

        if(cardBefore!=cardAfter)
            logger.info("Redundancies found. Average exo-cardinality from "+cardBefore+" to "+cardAfter);
        else
            logger.info("Redundancies not found");

    }


    /** Step 8 */
    private Boolean checkCompatibility(){
        try {
            // todo check compatibility between this.data and this.model
            logger.info("Checking compatibility ");

            // Check compatibility degree
            ratio = model.ratioLogLikelihood(data);
            logger.info("Compatibility degree (LL ratio): " + ratio);

            if (Double.isNaN(ratio) || ratio < minCompatibilityDegree)
                return false;

            // Solve exactly
            if (!CausalGraphTools.isNonQuasiMarkovian(model.getNetwork())) {
                int[] cofoundingVars = Arrays.stream(model.getExogenousVars())
                        .filter(u->model.getEndogenousChildren(u).length>1).toArray();
                return model.isCompatible(data, cofoundingVars, FactorUtil.DEFAULT_DECIMALS);
            }
        }catch (Exception e){
            logger.debug("Exception when checking compatibility: "+e.getMessage());
            return null;
        }catch (Error e){
            logger.debug("Error when checking compatibility: "+e.getMessage());
            return null;
        }

        return true;

    }

    /** Steps 9 to 11 */
    private void sampleData(double k) {
        // todo set this.data with size of this.datasize
        int finalSize = (int)(datasize*(1 + k* dataIncrementFactor));
        logger.info("sampling "+finalSize+" instances");
        data = model.samples(finalSize, model.getEndogenousVars());
    }



    private void reduceModel(){
        if(markovianity>1)
            return;
        double cardBefore, cardAfter;
        cardBefore=StatisticsModel.of(model).avgExoCardinality();
        ExogenousReduction reducer = new ExogenousReduction(this.model,  data).removeWithZeroUpper();
        if(reductionK!=1.0) reducer.removeWithZeroLower(reductionK);
        model = reducer.getModel();
        cardAfter = StatisticsModel.of(model).avgExoCardinality();

        if(cardBefore!=cardAfter)
            logger.info("Reduced model. Average exo-cardinality from "+cardBefore+" to "+cardAfter);
        else
            logger.info("Model not reduced");

    }



    public static void main(String[] args) {
        int s = 1;
        Logger.setGlobal(new Logger().setLabel("model_gen").setToStdOutput(true).setLevel(Logger.Level.DEBUG));


        for( s = 0; s<100; s++) {
            try {
                RandomUtil.setRandomSeed(s);

                // Set the logging

                System.out.println("----" + s);
                CompatibleCausalModelGenerator gen = new CompatibleCausalModelGenerator()
                        .setMaxIndegree(3)
                        .setMaxDataResamples(20)
                        .setMaxCofoundedVars(2)
                        .setMinCompatibilityDegree(0.975)
                        .setDatasize(1000)
                        .setDataIncrementFactor(0.5)
                        .setNumNodes(8)
                        .setMarkovianity(2);

                gen.run();
                StructuralCausalModel m = gen.getModel();
            }catch (Exception e){
                Logger.getGlobal().info(e.getMessage());
                e.printStackTrace();
            }

        }
    }
}
