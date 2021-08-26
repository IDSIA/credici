package ch.idsia.credici.model.builder;

import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.RandomUtilities;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CausalBuilder {

    private DirectedAcyclicGraph empiricalDAG;

    private DirectedAcyclicGraph causalDAG;

    private BayesianFactor[] equations;


    private StructuralCausalModel model;

    private TIntIntMap endoVarSizes;

    private TIntIntMap exoVarSizes;
    private int[] exoVarSizes_;

    private int num_decimals = 0;

    private boolean fillRandomEquations = false;




    public CausalBuilder(DirectedAcyclicGraph empiricalDAG, int[] endoVarSizes) {
        this.empiricalDAG = empiricalDAG;
        this.endoVarSizes = new TIntIntHashMap();
        int i = 0;
        for(int x: DAGUtil.getVariables(empiricalDAG)){
            this.endoVarSizes.put(x, endoVarSizes[i]);
            i++;
        }
    }

    public CausalBuilder(DAGModel<BayesianFactor> bnet) {
        this(bnet.getNetwork(), bnet.getSizes(bnet.getVariables()));
    }

    public static CausalBuilder of(DirectedAcyclicGraph empiricalDAG, int... endoVarSizes){
        int s = endoVarSizes[0];
        if(endoVarSizes.length == 1)
            endoVarSizes = IntStream.range(0, DAGUtil.getVariables(empiricalDAG).length)
                    .map(x -> s).toArray();
        return new CausalBuilder(empiricalDAG, endoVarSizes);
    }
    public static CausalBuilder of(DAGModel<BayesianFactor> bnet){
        return new CausalBuilder(bnet);
    }


    public StructuralCausalModel build(){
        buildCausalDAG();
        initEndogenousPart();
        calculateExoVarSizes();
        initEnxogenousPart();
        fillFactors();
        return model;
    }



    private void buildCausalDAG(){
        if(causalDAG != null){
            assertDAGsConsistency();
        }else if(equations != null){
            //build a DAG consistent with the equations
            causalDAG = (DirectedAcyclicGraph) empiricalDAG.clone();

            int[] empiricalVars = DAGUtil.getVariables(empiricalDAG);

            for(int x : empiricalVars){
                // Get the equation of X
                BayesianFactor eqx =
                        Stream.of(equations)
                                .filter(f -> f.getDomain().contains(x))
                                .toArray(BayesianFactor[]::new)[0];

                // Add links from each U to each X
                int U[] = ArraysUtil.difference(eqx.getDomain().getVariables(), empiricalVars);
                for(int u: U){
                    if(!causalDAG.containsVertex(u))
                        causalDAG.addVertex(u);
                    causalDAG.addEdge(u,x);
                }
            }
            // check consistency
            assertDAGsConsistency();
        }else{

            int[] empiricalVars = DAGUtil.getVariables(empiricalDAG);
            int u = IntStream.of(empiricalVars).max().getAsInt();

            // build the markovian-case DAG
            causalDAG = (DirectedAcyclicGraph) empiricalDAG.clone();
            for(int x: DAGUtil.getVariables(empiricalDAG)){
                causalDAG.addVertex(u);
                causalDAG.addEdge(u,x);
            }
        }

    }


    private void initEndogenousPart(){
        int[] empiricalVars = DAGUtil.getVariables(empiricalDAG);
        model = new StructuralCausalModel();
        for(int x: empiricalVars) {
            model.addVariable(x, endoVarSizes.get(x), false);
        }
        for(int x: empiricalVars) {
            model.addParents(x, DAGUtil.getParents(empiricalDAG,x));
        }

    }


    private void calculateExoVarSizes(){
        if(exoVarSizes_ != null) {
            if(exoVarSizes != null) throw new IllegalArgumentException("exoVarSizes set twice");
            exoVarSizes = new TIntIntHashMap();
            int i = 0;
            for(int u : DAGUtil.nodesDifference(causalDAG, empiricalDAG)){
                exoVarSizes.put(u, exoVarSizes_[i]);
                i++;
            }
        }
        else if(equations != null) {
            //Compute U sizes form equations
            exoVarSizes = new TIntIntHashMap();
            // set of exogenous variabels
            int[] U = DAGUtil.nodesDifference(causalDAG, empiricalDAG);
            // for each u find an equation where present and determine the size
            for(int u : U){
               BayesianFactor eq = Stream.of(equations)
                       .filter(f -> f.getDomain().contains(u))
                       .toArray(BayesianFactor[]::new)[0];
               exoVarSizes.put(u,eq.getDomain().getCardinality(u));
            }
        }else if(!isMarkovian()){
            // todo: compute in non-markovian equationless case
            throw new NotImplementedException("");
        }else{
            // compute in markovian equationless case
            exoVarSizes = new TIntIntHashMap();
            for(int x : model.getEndogenousVars()){

                int u = ArraysUtil.difference(DAGUtil.getParents(causalDAG,x), DAGUtil.getParents(empiricalDAG,x))[0];
                int sizeX = model.getDomain(x).getCombinations();
                int sizeEndoPaX = model.getDomain(DAGUtil.getParents(empiricalDAG,x)).getCombinations();
                exoVarSizes.put(u, (int) Math.pow(sizeX, sizeEndoPaX));

            }

        }

    }


    private void initEnxogenousPart(){
        int[] exogenousVars = ArraysUtil.difference(DAGUtil.getVariables(causalDAG),DAGUtil.getVariables(empiricalDAG));
        for(int u: exogenousVars) {
            model.addVariable(u, exoVarSizes.get(u), true);
            for(int x: DAGUtil.getChildren(causalDAG, u)) {
                model.addParent(x, u);
            }
        }
    }

    private void fillFactors(){

        if(num_decimals > 0) {
            model.fillExogenousWithRandomFactors(num_decimals);
        }
        if(fillRandomEquations) {
            model.fillWithRandomEquations();
        }
        else if (equations != null) {
            for(int x : model.getEndogenousVars()){

                int[] expectedVars = model.getVariableAndParents(x);

                BayesianFactor eqx =
                        Stream.of(equations)
                                .filter(f ->
                                    ArraysUtil.difference(f.getDomain().getVariables(), expectedVars).length == 0 &&
                                    ArraysUtil.difference(expectedVars, f.getDomain().getVariables()).length==0)
                                .toArray(BayesianFactor[]::new)[0];

                model.setFactor(x,eqx);

                Strides eqDomain = eqx.getDomain();

                for(int v: expectedVars){
                    if(eqDomain.getCardinality(v) != model.getDomain(v).getCardinality(v))
                        throw new IllegalArgumentException("Equations are not consistent with sizes");

                }

            }
        } else {
            for (int x : model.getEndogenousVars()) {
                    model.setFactor(x, EquationBuilder.of(model).withAllAssignments(x));
            }
        }


    }


    public CausalBuilder setExoVarSizes(TIntIntMap exoVarSizes) {
        this.exoVarSizes = exoVarSizes;
        return this;
    }

    public CausalBuilder setExoVarSizes(int[] exoVarSizes) {
        this.exoVarSizes_ = exoVarSizes;
        return this;
    }

    public CausalBuilder setFillRandomExogenousFactors(int num_decimals){
        this.num_decimals = num_decimals;
        return this;
    }

    public CausalBuilder setFillRandomEquations(boolean fillRandomEquations) {
        this.fillRandomEquations = fillRandomEquations;
        return this;
    }

    private boolean isMarkovian(){
        for(int v: DAGUtil.getVariables(causalDAG)){
            if(!empiricalDAG.containsVertex(v)){
                if(DAGUtil.getChildren(causalDAG,v).length > 1)
                    return false;
            }
        }
        return true;

    }

    public StructuralCausalModel getModel() {
        return model;
    }

    public DirectedAcyclicGraph getCausalDAG() {
        return causalDAG;
    }

    public DirectedAcyclicGraph getEmpiricalDAG() {
        return empiricalDAG;
    }

    public CausalBuilder setCausalDAG(DirectedAcyclicGraph causalDAG) {
        this.causalDAG = causalDAG;
        return this;
    }


    public CausalBuilder setEquations(BayesianFactor[] equations) {
        this.equations = equations;
        return this;
    }



    private void assertDAGsConsistency(){
        // check that causalDag and empirical one are consistent
        if(!DAGUtil.isContained(empiricalDAG, causalDAG))
            throw new IllegalArgumentException("Causal and empirical DAGs are not consistent");

        for(int u: DAGUtil.nodesDifference(causalDAG, empiricalDAG))
            if(DAGUtil.getParents(causalDAG,u).length > 0 )
                throw new IllegalArgumentException("Exogenous nodes cannot have ingoing arcs");

    }



    public static StructuralCausalModel transformFrom(DAGModel<BayesianFactor> bnet){

        for(int x: CausalInfo.of(bnet).getEndogenousVars()) {
            if (!FactorUtil.isDeterministic(bnet.getFactor(x),bnet.getParents(x)))
                throw new IllegalArgumentException("Variable " + x + " does not contain a deterministic function as factor");
        }

        StructuralCausalModel model = new StructuralCausalModel();

        for(int v: bnet.getVariables()){
            model.addVariable(v, bnet.getSize(v), CausalInfo.of(bnet).isExogenous(v));
        }

        for(int v: bnet.getVariables()) {
            model.addParents(v, bnet.getParents(v));
            model.setFactor(v, bnet.getFactor(v));
        }

        return model;
    }


    public static StructuralCausalModel random(DirectedAcyclicGraph empDAG, int endoVarSize, int exoVarSize ){

        Random r = RandomUtil.getRandom();
        StructuralCausalModel scm = new StructuralCausalModel();

        // build endogenous part
        int[] X = DAGUtil.getVariables(empDAG);
        for(int x: X)
            scm.addVariable(x, endoVarSize, false);

        for(int x:X)
            scm.addParents(x, DAGUtil.getParents(empDAG,x));

        // get a random number of co-founders (U variables)
        // between 1 and the number of endog. vars



        int Usize = r.nextInt(X.length)+1;
        int[] U = new int[Usize];
        for(int i = 0; i<Usize; i++){
            U[i] = i + X.length;
            scm.addVariable(U[i], exoVarSize, true);
        }


        // each X node should have at least a U parent
        for(int x : X){
            int u = U[r.nextInt(U.length)];
            scm.addParent(x,u);
        }

        //each U node should have at least a child
        for(int u : U){
            if(scm.getChildren(u).length==0){
                int x = X[r.nextInt(X.length)];
                scm.addParent(x,u);
            }

        }

        scm.fillWithRandomFactors(3);
        return scm;

    }


    /**
     * Creates a new model with the same structure but with random probability values
     * @param model
     * @param num_decimals
     * @param zero_allowed
     * @param variables
     * @return
     */
    public static DAGModel<BayesianFactor> random(DAGModel<BayesianFactor> model, int num_decimals,
                                                             boolean zero_allowed, int... variables){
        DAGModel<BayesianFactor> rmodel = model.copy();

        if(variables.length == 0)
            variables = rmodel.getVariables();

        for(int v: variables){
            BayesianFactor f = RandomUtilities.BayesianFactorRandom(
                                        rmodel.getDomain(v),
                                        rmodel.getDomain(rmodel.getParents(v)),
                                        num_decimals,
                                        zero_allowed);
            rmodel.setFactor(v, f);
        }

        return rmodel;
    }


}
