package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.ConstraintsOps;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToHalfSpace;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.convert.HalfspaceToVertex;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CredalBuilder {

    private final StructuralCausalModel causalmodel;

    private TIntObjectMap<BayesianFactor> empiricalFactors;

    private boolean vertex = true;

    SparseModel model;


    public CredalBuilder(StructuralCausalModel causalModel){
        this.causalmodel = causalModel;
    }

    public static CredalBuilder of(StructuralCausalModel causalModel){
        return new CredalBuilder(causalModel);
    }

    public CredalBuilder setEmpirical(TIntObjectMap factors) {
        this.empiricalFactors = factors;
        return this;
    }

    public CredalBuilder setEmpirical(BayesianFactor[] factors){
        empiricalFactors = new TIntObjectHashMap();
        for(int u: causalmodel.getExogenousVars()){
            empiricalFactors.put(u, getAssociatedEmpirical(u, factors));
        }
        return this;
    }

    public CredalBuilder setEmpirical(Collection factors){
        return setEmpirical((BayesianFactor[]) factors.toArray(BayesianFactor[]::new));
    }

    public CredalBuilder setToVertex() {
        vertex = true;
        return this;
    }
    public CredalBuilder setToHalfSpace() {
        vertex = false;
        return this;
    }

    public SparseModel getModel() {
        return model;
    }

    public SparseModel build(){

        // Check that P(U) is in the model
        if(empiricalFactors == null || empiricalFactors.size() == 0 )
            assertTrueMarginals(causalmodel);

        // Copy the structure of the causal model
        model = new SparseModel();
        model.addVariables(causalmodel.getSizes(causalmodel.getVariables()));
        for (int v : model.getVariables()){
            model.addParents(v, causalmodel.getParents(v));
        }

        // Set the credal sets for the endogenous variables X (structural eqs.)
        for(int x: causalmodel.getEndogenousVars()) {
            // Variable on the left should be the first
            BayesianFactor eqx  =causalmodel.getFactor(x).reorderDomain(x);

            if(vertex)
                model.setFactor(x, new BayesianToVertex().apply(eqx, x));
            else
                model.setFactor(x, new BayesianToHalfSpace().apply(eqx, x));
        }

        // Get the credal sets for the exogenous variables U
        for(int u: causalmodel.getExogenousVars()) {

            // Define the constraints in matrix form
            double[][] coeff = getCoeff(u);
            double[] vals = empiricalFactors.get(u).getData();

            SeparateHalfspaceFactor constFactor =
                    new SeparateHalfspaceFactor(false, true, model.getDomain(u), coeff, vals);

            // Remove unnecesary constraints
            //constFactor = constFactor.removeNormConstraints();
            constFactor = ConstraintsOps.removeZeroConstraints(constFactor);

            if(constFactor==null)
                throw new NoFeasibleSolutionException();

            // Transforms the factor if needed and set it to the model
            if(vertex){
                VertexFactor fu = new HalfspaceToVertex().apply(constFactor);
                if(fu.getData()[0]==null)
                    throw new NoFeasibleSolutionException();
                model.setFactor(u, fu);
            }else{
                model.setFactor(u, constFactor);
            }

        }

        return model;

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



    public static void assertTrueMarginals(StructuralCausalModel causalModel){
        for(int u: causalModel.getExogenousVars()){
            if(causalModel.getFactor(u) == null)
                throw new IllegalArgumentException("Empirical factors should be provided if true marginals are not in the SCM");
        }
    }




}
