package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.ConstraintsOps;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToHalfSpace;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.convert.HalfspaceToVertex;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ExactCredalBuilder extends CredalBuilder {

    /** should bayesian factor be kept or transformed */
    private boolean keepBayesian = false;

    private boolean vertex = true;

    private boolean nonnegative = true;

    private boolean raiseNoFeasible = true;

    protected TIntObjectMap<BayesianFactor> empiricalFactors;




    public ExactCredalBuilder(StructuralCausalModel causalModel){
        this.causalmodel = causalModel;
    }

    public static ExactCredalBuilder of(StructuralCausalModel causalModel){
        return new ExactCredalBuilder(causalModel);
    }

    public ExactCredalBuilder setEmpirical(TIntObjectMap<BayesianFactor> factors) {
        this.empiricalFactors = factors;
        return this;
    }

    public ExactCredalBuilder setEmpirical(BayesianFactor[] factors){
        empiricalFactors = new TIntObjectHashMap();
        for(int u: causalmodel.getExogenousVars()){
            empiricalFactors.put(u, getAssociatedEmpirical(u, factors));
        }
        return this;
    }

    public ExactCredalBuilder setEmpirical(Collection factors){
        return setEmpirical((BayesianFactor[]) factors.toArray(BayesianFactor[]::new));
    }

    public ExactCredalBuilder setEmpirical(BayesianNetwork bnet){
        if(!ArraysUtil.equals(bnet.getVariables(),
                causalmodel.getEndogenousVars(), true, false))
            throw new IllegalArgumentException("Uncompatible empirical network");

        List<BayesianFactor> factors = new ArrayList<>();
        FactorVariableElimination inf = new FactorVariableElimination(new MinFillOrdering().apply(bnet));
        inf.setFactors(bnet.getFactors());

        for(int x: causalmodel.getEndogenousVars()){
            BayesianFactor f = (BayesianFactor) inf.conditionalQuery(x, causalmodel.getEndegenousParents(x));
            f = f.fixPrecission(10, x);
            factors.add(f);
        }

        return setEmpirical(factors);

    }

    public ExactCredalBuilder setToVertex() {
        vertex = true;
        return this;
    }
    public ExactCredalBuilder setToHalfSpace() {
        vertex = false;
        return this;
    }


    public ExactCredalBuilder setKeepBayesian() {
        keepBayesian = true;
        return this;
    }


    public ExactCredalBuilder setNonnegative(boolean nonnegative) {
        this.nonnegative = nonnegative;
        return this;
    }

    public ExactCredalBuilder setRaiseNoFeasible(boolean raiseNoFeasible) {
        this.raiseNoFeasible = raiseNoFeasible;
        return this;
    }

    public ExactCredalBuilder build(int... exoVars){
        initModel();

        // Set the credal sets for the endogenous variables X (structural eqs.)
        for(int x: causalmodel.getEndogenousVars()) {
            buildEndoFactor(x);
        }

        if (exoVars.length==0)
            exoVars = causalmodel.getExogenousVars();

        // Get the credal sets for the exogenous variables U
        for(int u: exoVars) {
            try {
                buildExoFactor(u);
            }catch (NoFeasibleSolutionException e){
                if(raiseNoFeasible) {
                    System.out.println("Error in variable: "+u);
                    throw new NoFeasibleSolutionException();
                }

            }
        }

        return this;

    }

    private void initModel() {
        // Check that P(U) is in the model
        if(empiricalFactors == null || empiricalFactors.size() == 0 )
            assertTrueMarginals();

        // Check structure
        assertMarkovianity();

        // Copy the structure of the causal model
        model = new SparseModel();
        model.addVariables(causalmodel.getSizes(causalmodel.getVariables()));
        for (int v : model.getVariables()){
            model.addParents(v, causalmodel.getParents(v));
        }
    }

    private void buildEndoFactor(int x) {
        // Variable on the left should be the first
        BayesianFactor eqx = causalmodel.getFactor(x).reorderDomain(x);
        if (keepBayesian) { 
            model.setFactor(x, eqx);
        } else if (vertex) {
            model.setFactor(x, new BayesianToVertex().apply(eqx, x));
        } else{
            SeparateHalfspaceFactor fx = new BayesianToHalfSpace().apply(eqx, x);
            // Simplify the linear constraints
            fx = fx.removeNormConstraints();
            if(!this.nonnegative) fx = fx.removeNonNegativeConstraints();
            model.setFactor(x, fx);
        }
    }

    public void buildExoFactor(int u) {
        // Define the constraints in matrix form
        double[][] coeff = getCoeff(u);
        double[] vals = empiricalFactors.get(u).getData();

        SeparateHalfspaceFactor constFactor =
                new SeparateHalfspaceFactor(false, this.nonnegative, model.getDomain(u), coeff, vals);

        // remove constraints with all their coefficients equal to zero
        constFactor = ConstraintsOps.removeZeroConstraints(constFactor);
        if(constFactor==null)
            throw new NoFeasibleSolutionException();

        // Transforms the factor if needed and set it to the model
        if(vertex){
        	System.out.println(u);
        	constFactor.printLinearProblem();
        	
            VertexFactor fu = new HalfspaceToVertex().apply(constFactor);
            if(fu.getData()[0]==null)
                throw new NoFeasibleSolutionException();
            model.setFactor(u, fu);
        }else{
            model.setFactor(u, constFactor);
        }
    }


    private BayesianFactor getAssociatedEmpirical(int u, BayesianFactor[] factors){

        int[] ch_u = causalmodel.getChildren(u);
        BayesianFactor[] possibleFactors =
                Stream.of(factors).filter(f ->
                        ImmutableSet.copyOf(Ints.asList(f.getDomain().getVariables()))
                        .equals(ImmutableSet.copyOf(
                                Ints.asList(Ints.concat(ch_u, causalmodel.getEndegenousParents(ch_u))))
                        ))
                        .toArray(BayesianFactor[]::new);

        if(possibleFactors.length != 1)
            throw new IllegalArgumentException("Cannot determine the associated empirical to "+u);

        return possibleFactors[0];

    }

    /**
     * Transforms the structural equations associated to the children of a given U into a coefficient matrix.
     * @param u
     * @return
     */
    private double[][] getCoeff(int u){
        if(!causalmodel.isExogenous(u))
            throw new IllegalArgumentException("Variable "+u+" is not exogenous");

        int[] children = causalmodel.getEndogenousChildren(u);

        BayesianFactor joinEQ = IntStream.of(children).mapToObj(i -> causalmodel.getFactor(i)).reduce((f1, f2) -> f1.combine(f2)).get();
        // reorder coeff domain
        joinEQ = joinEQ.reorderDomain(Ints.concat(empiricalFactors.get(u).getDomain().getVariables(), new int[]{u}));

        // Get the coefficients by combining all the EQs of the children
        double[][] coeff = ArraysUtil.transpose(ArraysUtil.reshape2d(
                joinEQ.getData(), causalmodel.getSizes(u)
        ));
        return coeff;
    }



    public boolean isSolvable(){
        try{
            // Check that P(U) is in the model
            if(empiricalFactors == null || empiricalFactors.size() == 0 )
                assertTrueMarginals();
            // Check structure
            assertMarkovianity();
        }catch (Exception e){
            return false;
        }
        return true;
    }


}
