package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CompatibleCausalModelGenerator {

    // Parameters
    private boolean DAGfromBNG = false; // todo: default value will be changed to true

    /** Number of nodes in the first DAG .*/
    private int numNodes = 5;

    private int markovianity = -1;  // -1 not-fixed, 0 markovian, 1 quasi-markovian, 2 non-markovian

    /** Parameter in case of DAGfromBNG=false*/
    private int lambda = 3;

    private int maxIndegree = 3;

    private int datasize = 1000;

    private int maxDataResamples = 5;

    private int maxModelResamples = Integer.MAX_VALUE;

    // Global variables
    private StructuralCausalModel model = null;

    TIntIntMap data = null;

    private Logger logger = Logger.getGlobal();

    // Add getters and setters here
    public StructuralCausalModel getModel() {
        return model;
    }

    public TIntIntMap getData() {
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

    public CompatibleCausalModelGenerator setMaxModelResamples(int maxModelResamples) {
        this.maxModelResamples = maxModelResamples;
        return this;
    }

    public CompatibleCausalModelGenerator setMarkovianity(int markovianity) {
        this.markovianity = markovianity;
        return this;
    }

    // Generation loop
    public void run(){
        boolean compatibleFound = false;

        modelLoop:
        for(int j=0; j<maxModelResamples; j++) {
            SparseDirectedAcyclicGraph causalDAG = generateDAG();
            sampleCausalModel(causalDAG);
            for (int i = 0; i < maxDataResamples; i++) {
                sampleData();
                compatibleFound = checkCompatibility();
                if (compatibleFound) break modelLoop;
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

        // todo: maybe we could give the option to specify the markovianity here or not
        SparseDirectedAcyclicGraph dag = null;
        boolean repeat;
        int markObtained;

        do {
            if (DAGfromBNG) {
                dag = DAGUtil.randomFromBNGenerator(); // note: you might use some of the parameters in the alternative generator
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
            }else{
                repeat = false;
            }


        }while(repeat);

        logger.info("Sampled DAG: " + dag);
        logger.info("Markovianity: "+markObtained);



        return dag;
    }


    /* Steps 4 to 7 */
    private void sampleCausalModel(SparseDirectedAcyclicGraph causalDAG) {
        //todo: set this.model
        logger.info("Creating SCM ");
        logger.info("Exo CC: "+ CausalGraphTools.exoConnectComponents(causalDAG).stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));
        logger.info("Endo CC: "+CausalGraphTools.endoConnectComponents(causalDAG).stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("|")));

    }


    /** Step 8 */
    private boolean checkCompatibility(){
        // todo check compatibility between this.data and this.model
        logger.info("Checking compatibility ");

        return true; // change
    }

    /** Steps 9 to 11 */
    private void sampleData() {
        // todo set this.data with size of this.datasize
        logger.info("sampling data");
    }

    public static void main(String[] args) {
        int s = 2;

            RandomUtil.setRandomSeed(s);

            // Set the logging
            Logger.setGlobal(new Logger().setLabel("model_gen").setToStdOutput(true).setLevel(Logger.Level.DEBUG));

            CompatibleCausalModelGenerator gen = new CompatibleCausalModelGenerator()
                    .setMaxIndegree(1)
                    //.setLambda(2)
                    .setMarkovianity(0)
                    .setNumNodes(20);

            gen.run();
            StructuralCausalModel m = gen.getModel();
            TIntIntMap data = gen.getData();

    }

}