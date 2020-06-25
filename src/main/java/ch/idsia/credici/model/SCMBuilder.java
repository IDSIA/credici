package ch.idsia.credici.model;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;

public class SCMBuilder {

    private SparseDirectedAcyclicGraph empiricalDAG;

    private SparseDirectedAcyclicGraph causalDAG;

    private BayesianFactor[] equations;


    private StructuralCausalModel model;

    private TIntIntMap endoVarSizes;

    private TIntIntMap exoVarSizes;



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
        buildEquations();
        return model;
    }



    private void buildCausalDAG(){
        if(causalDAG != null){
            // todo: check that causalDag and empirical one are consistent
        }else if(equations != null){
            //todo: build a DAG consistent with the equations
            throw new NotImplementedException("");
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
        if(equations != null) {
            //todo: compute from here
            throw new NotImplementedException("");
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

    private void buildEquations(){
        if(equations != null){
            // todo check that equations are consistent with causal dag and sizes
        } else {
            for (int x : model.getEndogenousVars()) {
                model.setFactor(x, EquationBuilder.of(model).withAllAssignments(x));
            }
        }
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

    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[]{0.6,0.5, 0.5,0.5}));


        //int u1=2, u2=3;

        //EquationBuilder.fromVector(Strides.as(y,2))

        //StructuralCausalModel model =  SCMBuilder.of(bnet).build();





    }



}
