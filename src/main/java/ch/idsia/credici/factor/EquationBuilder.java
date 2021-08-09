package ch.idsia.credici.factor;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.stream.IntStream;

public class EquationBuilder {

    private StructuralCausalModel model;

    /**
     * Constructor of a EquationBuilder with a model associated
     * @param model
     */
    public EquationBuilder(StructuralCausalModel model){
        this.model = model;
    }

    private static TIntObjectHashMap<EquationBuilder> builders = new TIntObjectHashMap<>();

    public static EquationBuilder of(StructuralCausalModel model){
        int hash = model.hashCode();
        if(!builders.containsKey(hash))
            builders.put(hash, new EquationBuilder(model)) ;
        return builders.get(hash);
    }

    private void assertEndogenous(int var){
        if(model.isExogenous(var))
            throw new IllegalArgumentException("Variable "+var+" is not endogenous");
    }


    /**
     * Builds a structural equation (i.e., deterministic bayesian factor) with
     * an endogenous variable and its parent in the domain.
     * @param var
     * @param assignments
     * @return
     */
    public BayesianFactor fromVector(int var, int... assignments){
        assertEndogenous(var);

        return fromVector(
                model.getDomain(var),
                model.getDomain(model.getParents(var)),
                assignments);

    }

    public BayesianFactor from2DArray(int var, int[][] assignments) {
        return this.fromVector(var, ArraysUtil.flattenInts(List.of(assignments)));
    }

    public BayesianFactor from3DArray(int var, int[][][] assignments) {
        return this.fromVector(var, ArraysUtil.flattenInts(List.of(assignments)));
    }

    public BayesianFactor from4DArray(int var, int[][][][] assignments) {
        return this.fromVector(var, ArraysUtil.flattenInts(List.of(assignments)));
    }
    public BayesianFactor fromList(int var, List assignments) {
        return this.fromVector(var, ArraysUtil.flattenInts(assignments));
    }

    public BayesianFactor withAllAssignments(int var){

        int[] U = model.getExogenousParents(var);
        if(U.length>1 || model.getChildren(U[0]).length>1){
            throw new IllegalArgumentException("Only marvokvian case is allowed");
        }

        int n = model.getDomain(model.getEndegenousParents(var)).getCombinations();
        int[] states = IntStream.range(0, model.getSize(var)).toArray();
        return from2DArray(var, Combinatorial.getCombinations(n, states));
    }


    public static BayesianFactor fromVector(Strides left, Strides right, int... assignments){
        return BayesianFactorBuilder.deterministic(left, right, assignments);
    }

    public static BayesianFactor from2DArray(Strides left, Strides right, int[][] assignments) {
        return fromVector(left, right, ArraysUtil.flattenInts(List.of(assignments)));
    }

    public static BayesianFactor from3DArray(Strides left, Strides right, int[][][] assignments) {
        return fromVector(left, right, ArraysUtil.flattenInts(List.of(assignments)));
    }

    public static BayesianFactor from4DArray(Strides left, Strides right, int[][][][] assignments) {
        return fromVector(left, right, ArraysUtil.flattenInts(List.of(assignments)));
    }
    public static BayesianFactor fromList(Strides left, Strides right, List assignments) {
        return fromVector(left, right, ArraysUtil.flattenInts(assignments));
    }

    public BayesianFactor withAllAssignments(Strides left, Strides endoParents, Strides exoParent){
        if(exoParent.getVariables().length>1)
            throw new IllegalArgumentException("Only markovian case is allowed");
        int n = endoParents.getCombinations();
        int[] states = IntStream.range(0, left.getCombinations()).toArray();
        return from2DArray(left, endoParents.concat(exoParent), Combinatorial.getCombinations(n, states));
    }


}
