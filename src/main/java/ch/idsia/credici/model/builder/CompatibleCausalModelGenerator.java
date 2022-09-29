package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
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

    // Global variables
    private StructuralCausalModel model = null;

    TIntIntMap[] data = null;

    private List<int[]> mergeExoPairs = new ArrayList<>();

    private Logger logger = Logger.getGlobal();

    private int numDecimals = 5;

    // Add getters and setters here
    public StructuralCausalModel getModel() {
        return model;
    }

    public TIntIntMap[] getData() {
        return data;
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

    // Generation loop
    public void run(){
        boolean compatibleFound = false;

        modelLoop:
        for(int j=0; j<maxModelResamples; j++) {
            SparseDirectedAcyclicGraph causalDAG = generateDAG();
            initializeModel(causalDAG);
            for (int k = 0; k < maxExoFactorsResamples; k++) {
                sampleExoFactors();
                for (int i = 0; i < maxDataResamples; i++) {
                    sampleData();
                    compatibleFound = checkCompatibility();

                    if (compatibleFound) {
                        logger.info("Found compatible model and data");
                        break modelLoop;
                    }
                }
            }
        }
        if(!compatibleFound)
            throw new IllegalStateException("Compatibility not found for current parameters");


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
                dag = DAGUtil.randomFromBNGenerator(numNodes, maxIndegree); // note: you might use some of the parameters in the alternative generator
            } else {
                dag = DAGUtil.random(numNodes, lambda, maxIndegree);
            }


            // step 2,3
            if (!CausalGraphTools.isValidCausalDAG(dag)) {
                logger.debug("Non-valid sampled DAG before transformation: " + dag);
                dag = CausalGraphTools.makeValidCausalDAG(dag);
            }
            markObtained = CausalGraphTools.getMarkovianity(dag);

            if(markObtained>1)
                throw new NotImplementedException("Code not implemented for markovianity "+markObtained);

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
                .build();

        if(mergeExoPairs.size()>0)
            model = Cofounding.mergeExoVars(model, mergeExoPairs.toArray(int[][]::new));

        logger.info("Initialized SCM structure");
        logger.info("Exo CC: "+ model.exoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));
        logger.info("Endo CC: "+model.endoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));






    }

    /*Step 7*/
    private void sampleExoFactors(){
        logger.info("Sampling exogenous factors");
        model.fillExogenousWithRandomFactors(5);

        logger.debug("Sampled SCM:"+model);

    }


    /** Step 8 */
    private boolean checkCompatibility(){
        // todo check compatibility between this.data and this.model
        logger.info("Checking compatibility ");

        // Check compatibility degree
        double ratio = model.ratioLogLikelihood(data);
        logger.info("Compatibility degree (LL ratio): "+ratio);

        if(Double.isNaN(ratio) || ratio<minCompatibilityDegree)
            return false;

        // Solve exactly
        if(!CausalGraphTools.isNonQuasiMarkovian(model.getNetwork()))
            return model.isCompatible(data, numDecimals);
        return true; // change
    }

    /** Steps 9 to 11 */
    private void sampleData() {
        // todo set this.data with size of this.datasize
        logger.info("sampling "+datasize+" instances");
        data = model.samples(datasize, model.getEndogenousVars());
    }

    public static void main(String[] args) {
        int s = 2;
        Logger.setGlobal(new Logger().setLabel("model_gen").setToStdOutput(true).setLevel(Logger.Level.DEBUG));


        for( s = 0; s<100; s++) {
            try {
                RandomUtil.setRandomSeed(s);

                // Set the logging

                System.out.println("----");
                CompatibleCausalModelGenerator gen = new CompatibleCausalModelGenerator()
                        .setMaxIndegree(3)
                        .setMaxCofoundedVars(2)
                        .setMinCompatibilityDegree(0.9)
                        .setNumNodes(7);
                gen.run();
                StructuralCausalModel m = gen.getModel();
            }catch (Exception e){
                Logger.getGlobal().info(e.getMessage());
                e.printStackTrace();
            }

        }
    }
}
