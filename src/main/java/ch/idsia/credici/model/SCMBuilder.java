package ch.idsia.credici.model;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.stream.Stream;

public class SCMBuilder {

    private SparseDirectedAcyclicGraph empiricalDAG;

    private SparseDirectedAcyclicGraph causalDAG;

    private BayesianFactor[] equations;


    private StructuralCausalModel model;

    private TIntIntMap endoVarSizes;

    private TIntIntMap exoVarSizes;
    private int[] exoVarSizes_;

    private int num_decimals = 0;

    private boolean fillRandomEquations = false;




    public SCMBuilder(SparseDirectedAcyclicGraph empiricalDAG, int[] endoVarSizes) {
        this.empiricalDAG = empiricalDAG;
        this.endoVarSizes = new TIntIntHashMap();
        int i = 0;
        for(int x: empiricalDAG.getVariables()){
            this.endoVarSizes.put(x, endoVarSizes[i]);
            i++;
        }
    }

    public SCMBuilder(BayesianNetwork bnet) {
        this(bnet.getNetwork(), bnet.getSizes(bnet.getVariables()));
    }

    public static SCMBuilder of(SparseDirectedAcyclicGraph empiricalDAG, int[] endoVarSizes){
        return new SCMBuilder(empiricalDAG, endoVarSizes);
    }
    public static SCMBuilder of(BayesianNetwork bnet){
        return new SCMBuilder(bnet);
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
        }else if(!isMarkovian()){
            // todo: compute in non-markovian equationless case
            throw new NotImplementedException("");
        }else{
            // compute in markovian equationless case
            exoVarSizes = new TIntIntHashMap();
            for(int x : model.getEndogenousVars()){
                int u = ArraysUtil.difference(causalDAG.getParents(x), empiricalDAG.getParents(x))[0];
                int sizeX = model.getDomain(x).getCombinations();
                int sizeEndoPaX = model.getDomain(empiricalDAG.getParents(x)).getCombinations();
                exoVarSizes.put(u, (int) Math.pow(sizeX, sizeEndoPaX));

            }

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
            for (int x : model.getEndogenousVars()) {
                    model.setFactor(x, EquationBuilder.of(model).withAllAssignments(x));
            }
        }


    }


    public SCMBuilder setExoVarSizes(TIntIntMap exoVarSizes) {
        this.exoVarSizes = exoVarSizes;
        return this;
    }

    public SCMBuilder setExoVarSizes(int[] exoVarSizes) {
        this.exoVarSizes_ = exoVarSizes;
        return this;
    }

    public SCMBuilder setFillRandomExogenousFactors(int num_decimals){
        this.num_decimals = num_decimals;
        return this;
    }

    public SCMBuilder setFillRandomEquations(boolean fillRandomEquations) {
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

    public StructuralCausalModel getModel() {
        return model;
    }

    public SparseDirectedAcyclicGraph getCausalDAG() {
        return causalDAG;
    }

    public SparseDirectedAcyclicGraph getEmpiricalDAG() {
        return empiricalDAG;
    }

    public SCMBuilder setCausalDAG(SparseDirectedAcyclicGraph causalDAG) {
        this.causalDAG = causalDAG;
        return this;
    }


    public SCMBuilder setEquations(BayesianFactor[] equations) {
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

        StructuralCausalModel model =  SCMBuilder.of(bnet).setFillRandomExogenousFactors(3).build();

        SCMBuilder b = SCMBuilder.of(bnet).setFillRandomExogenousFactors(3);
        b.build();
        b.getModel().printSummary();



    }



}
