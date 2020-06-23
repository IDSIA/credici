package ch.idsia.credici.factor;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.javasoft.util.ints.IntHashMap;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;
import java.util.stream.IntStream;

public class EquationBuilder {

    private StructuralCausalModel model;

    /**
     * Constructor of a EquationBuilder with a model
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

        return BayesianFactor.deterministic(model.getDomain(var),
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

    public BayesianFactor withAllAssingments(int var){
        int n = model.getDomain(model.getEndegenousParents(var)).getCombinations();
        int[] states = IntStream.range(0, model.getSize(var)).toArray();
        return from2DArray(var, Combinatorial.getCombinations(n, states));
    }



}
