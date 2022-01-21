package ch.idsia.credici.model.builder;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CausalBuilder {

    private SparseDirectedAcyclicGraph empiricalDAG;

    private SparseDirectedAcyclicGraph causalDAG;

    private BayesianFactor[] equations;


    private StructuralCausalModel model;

    private TIntIntMap endoVarSizes;

    private TIntIntMap exoVarSizes;
    private int[] exoVarSizes_;

    private int num_decimals = 0;

    private boolean fillRandomEquations = false;




    public CausalBuilder(SparseDirectedAcyclicGraph empiricalDAG, int[] endoVarSizes) {
        this.empiricalDAG = empiricalDAG;
        this.endoVarSizes = new TIntIntHashMap();
        int i = 0;
        for(int x: empiricalDAG.getVariables()){
            this.endoVarSizes.put(x, endoVarSizes[i]);
            i++;
        }
    }

    public CausalBuilder(BayesianNetwork bnet) {
        this(bnet.getNetwork(), bnet.getSizes(bnet.getVariables()));
    }

    public static CausalBuilder of(SparseDirectedAcyclicGraph empiricalDAG, int... endoVarSizes){
        int s = endoVarSizes[0];
        if(endoVarSizes.length == 1)
            endoVarSizes = IntStream.range(0,empiricalDAG.getVariables().length)
                    .map(x -> s).toArray();
        return new CausalBuilder(empiricalDAG, endoVarSizes);
    }
    public static CausalBuilder of(BayesianNetwork bnet){
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
            causalDAG = empiricalDAG.copy();
            for(int x : empiricalDAG.getVariables()){
                // Get the equation of X
                BayesianFactor eqx =
                        Stream.of(equations)
                                .filter(f -> f.getDomain().contains(x))
                                .toArray(BayesianFactor[]::new)[0];

                // Add links from each U to each X
                int U[] = ArraysUtil.difference(eqx.getDomain().getVariables(), empiricalDAG.getVariables());
                for(int u: U){
                    if(!causalDAG.containsVertex(u))
                        causalDAG.addVariable(u);
                    causalDAG.addLink(u,x);
                }
            }
            // check consistency
            assertDAGsConsistency();
        }else{
            // build the markovian-case DAG
            causalDAG = empiricalDAG.copy();
            for(int x: empiricalDAG.getVariables()){
                int u = causalDAG.addVariable();
                causalDAG.addLink(u,x);
            }
        }

    }


    private void initEndogenousPart(){
        model = new StructuralCausalModel();
        for(int x: empiricalDAG) {
            model.addVariable(x, endoVarSizes.get(x), false);
        }
        for(int x: empiricalDAG) {
            model.addParents(x, empiricalDAG.getParents(x));
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
        }else if(isNonQuasiMarkovian()) {
            // todo: compute in non-markovian equationless case
            throw new NotImplementedException("");
        }else{
            exoVarSizes = new TIntIntHashMap();
            for(int u : DAGUtil.getExogenous(causalDAG)){
                int[] ch = causalDAG.getChildren(u);
                Strides domCh = model.getDomain(ch);
                Strides domPa = model.getDomain(model.getEndegenousParents(ch));
                exoVarSizes.put(u, EquationOps.maxExoCardinality(domCh, domPa));
            }
   /*
            // compute in markovian equationless case
            exoVarSizes = new TIntIntHashMap();
            for(int x : model.getEndogenousVars()){
                int u = ArraysUtil.difference(causalDAG.getParents(x), empiricalDAG.getParents(x))[0];
                int sizeX = model.getDomain(x).getCombinations();
                int sizeEndoPaX = model.getDomain(empiricalDAG.getParents(x)).getCombinations();
                exoVarSizes.put(u, (int) Math.pow(sizeX, sizeEndoPaX));

            }
*/
        }

    }


    private void initEnxogenousPart(){
        int[] exogenousVars = ArraysUtil.difference(causalDAG.getVariables(), empiricalDAG.getVariables());
        for(int u: exogenousVars) {
            model.addVariable(u, exoVarSizes.get(u), true);
            for(int x: causalDAG.getChildren(u)) {
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
            for(int u: model.getExogenousVars()){
                int[] chU = model.getEndogenousChildren(u);
                if(chU.length==1){
                    model.setFactor(chU[0], EquationBuilder.of(model).withAllAssignments(chU[0]));
                }else{
                    HashMap eqs = (HashMap) EquationBuilder.of(model).withAllAssignmentsQM(u);
                    for(int x : chU){
                        model.setFactor(x, (BayesianFactor) eqs.get(x));
                    }
                }
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
        for(int v: causalDAG.getVariables()){
            if(!empiricalDAG.containsVertex(v)){
                if(causalDAG.getChildren(v).length > 1)
                    return false;
            }
        }
        return true;
    }

    private boolean isQuasiMarkovian(){
        return (!isMarkovian()) && DAGUtil.getExogenousTreewidth(causalDAG) == 1;
    }

    private boolean isNonQuasiMarkovian(){
        return  DAGUtil.getExogenousTreewidth(causalDAG) > 1;
    }


    public StructuralCausalModel getModel() {
        return model;
    }

    public SparseDirectedAcyclicGraph getCausalDAG() {
        return causalDAG;
    }

    public SparseDirectedAcyclicGraph getEmpiricalDAG() {
        return empiricalDAG;
    }

    public CausalBuilder setCausalDAG(SparseDirectedAcyclicGraph causalDAG) {
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
            if(causalDAG.getParents(u).length > 0 )
                throw new IllegalArgumentException("Exogenous nodes cannot have ingoing arcs");

    }

    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[]{0.6,0.5, 0.5,0.5}));


        //int u1=2, u2=3;

        //EquationBuilder.fromVector(Strides.as(y,2))

        StructuralCausalModel model =  CausalBuilder.of(bnet).setFillRandomExogenousFactors(3).build();

        CausalBuilder b = CausalBuilder.of(bnet).setFillRandomExogenousFactors(3);
        b.build();
        b.getModel().printSummary();



    }


    public static StructuralCausalModel transformFrom(BayesianNetwork bnet){

        for(int x: CausalInfo.of(bnet).getEndogenousVars()) {
            if (!bnet.getFactor(x).isDeterministic(bnet.getParents(x)))
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


    public static StructuralCausalModel random(SparseDirectedAcyclicGraph empDAG, int endoVarSize, int exoVarSize ){

        Random r = RandomUtil.getRandom();
        StructuralCausalModel scm = new StructuralCausalModel();

        // build endogenous part
        int[] X = empDAG.getVariables();
        for(int x: X)
            scm.addVariable(x, endoVarSize, false);

        for(int x:X)
            scm.addParents(x, empDAG.getParents(x));

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


}
