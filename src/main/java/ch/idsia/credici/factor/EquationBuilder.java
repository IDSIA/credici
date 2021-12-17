package ch.idsia.credici.factor;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import ch.javasoft.util.ints.IntHashMap;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
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

        return fromVector(model.getDomain(var),
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

    public HashMap withAllAssignments(int exoVar){

        // TODO: review, not working
        //TODO: CHECKS
        //this_model.getExogenousTreewidth() <= 2;

        // Check U size

        // Equations to be built
        HashMap eqs = new HashMap();
        for(int v : this.model.getEndogenousChildren(exoVar)) {

            Strides domf = this.model.getDomain
                    (Ints.concat(new int[]{v}, this.model.getParents(v)));


            BayesianFactor f = new BayesianFactor(domf);
            eqs.put(v, f);
        }



        // Build set S

        List S = new ArrayList();
        for(int v : this.model.getEndogenousChildren(exoVar)){
            List dom = IntStream.range(0, this.model.getDomain(v).getCardinality(v)).boxed().collect(Collectors.toList());
            for(int i=0; i< dom.size(); i++){
                S.add(dom);
            }
        }


        int i = 0;
        for(Object s_ : Combinatorial.cartesianProduct((List[])S.toArray(List[]::new))){
            List s = (List) s_;
            int j = 0;
            for(int v : this.model.getEndogenousChildren(exoVar)){
                BayesianFactor f = (BayesianFactor) eqs.get(v);
                Strides endoPaDom = this.model.getDomain(this.model.getEndegenousParents(v));
                IndexIterator it = endoPaDom.getIterator();
                while(it.hasNext()){
                    int idx = it.next();
                    ObservationBuilder endoPaValues = ObservationBuilder.observe(endoPaDom.getVariables(), endoPaDom.statesOf(idx));
                    ObservationBuilder exoPaValues = ObservationBuilder.observe(exoVar, j);
                    EquationOps.setValue(f, exoPaValues, endoPaValues, v, (int)s.get(j));
                    //System.out.println("f"+varNames.get(v)+"(u_"+i+","+endoPaValues+") = "+varNames.get(v)+"_"+(int)s.get(j));
                    j++;

                }
            }
            i++;
        }



    }


    public static BayesianFactor fromVector(Strides left, Strides right, int... assignments){
        return BayesianFactor.deterministic(left, right, assignments);
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
