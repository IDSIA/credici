package ch.idsia.credici.factor;

import ch.idsia.credici.collections.FIntIntHashMap;
import ch.idsia.credici.collections.FIntObjectHashMap;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.credici.utility.DomainUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import ch.javasoft.util.ints.IntHashMap;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static TIntObjectMap<EquationBuilder> builders = new FIntObjectHashMap<>();

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

    public Map<Integer, BayesianFactor> conservative(int... exoVars) {
        if(model.exoConnectComponents().stream().filter(c -> ArraysUtil.equals(exoVars, c, true, true)).count() != 1)
            throw new IllegalArgumentException("Wrong exogenous variables.");

        int[] chU = model.getEndogenousChildren(exoVars);
        Map<Integer, BayesianFactor> eqs = null;
        if(chU.length==1 && exoVars.length==1){
            eqs = new HashMap<Integer, BayesianFactor>();
            eqs.put(chU[0], EquationBuilder.of(model).withAllAssignments(chU[0]));
        }
        if(chU.length>1 && exoVars.length==1) {
            eqs = EquationBuilder.of(model).withAllAssignmentsQM(exoVars[0]);
        } else{
            throw new NotImplementedException("Not implemented conservative specification for non-quaisi markovian");
        }

        return eqs;
    }


    public BayesianFactor withAllAssignments(int var){

        int[] U = model.getExogenousParents(var);
        if(U.length>1 || model.getChildren(U[0]).length>1){
            throw new IllegalArgumentException("Only marvokvian case is allowed");
        }

        int n = model.getDomain(model.getEndogenousParents(var)).getCombinations();
        int[] states = IntStream.range(0, model.getSize(var)).toArray();
        return from2DArray(var, Combinatorial.getCombinations(n, states));
    }

    public Map<Integer, BayesianFactor> withAllAssignmentsQM(int exoVar) {

        // Check topology
        if (this.model.getExogenousParents(this.model.getEndogenousChildren(exoVar)).length != 1)
            throw new IllegalArgumentException("Wrong topology: endogenous children cannot have more than 1 exogenous parent");

        // Check U-size
        int expected = EquationOps.maxExoCardinality(exoVar, this.model);
        int actual = this.model.getDomain(exoVar).getCombinations();
        if(expected != actual)
            throw new IllegalArgumentException("Exepected cardinality for exogenous variable is "+expected+", found "+actual);


        // Equations to be built
        HashMap<Integer, BayesianFactor> eqs = new HashMap<>();
        for(int v : this.model.getEndogenousChildren(exoVar)) {
            Strides domf = this.model.getDomain
                    (Ints.concat(new int[]{v}, this.model.getParents(v)));
            BayesianFactor f = new BayesianFactor(domf);
            eqs.put(v, f);
        }



        // Build set S

        List<List<Integer>> S = new ArrayList<>();
        for(int v : this.model.getEndogenousChildren(exoVar)){
            List<Integer> dom = IntStream.range(0, this.model.getDomain(v).getCardinality(v)).boxed().collect(Collectors.toList());
            int endoPaCard = this.model.getDomain(this.model.getEndogenousParents(v)).getCombinations();
            for(int i=0; i< endoPaCard; i++){
                S.add(dom);
            }
        }


        int i = 0;
        for(List<Integer> s : Combinatorial.cartesianProduct(S)){
            int j = 0;
            for(int v : this.model.getEndogenousChildren(exoVar)){
                BayesianFactor f = eqs.get(v);
                Strides endoPaDom = this.model.getDomain(this.model.getEndogenousParents(v));
                IndexIterator it = endoPaDom.getIterator();
                while(it.hasNext()){
                    int idx = it.next();
                    ObservationBuilder endoPaValues = ObservationBuilder.observe(endoPaDom.getVariables(), endoPaDom.statesOf(idx));
                    ObservationBuilder exoPaValues = ObservationBuilder.observe(exoVar, i);
                    EquationOps.setValue(f, exoPaValues, endoPaValues, v, s.get(j));
                    j++;
                }
            }
            i++;
        }

        return eqs;

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


    public static BayesianFactor conservative(int Uvar, Strides endoDom, int[] vars, int... endoParents){

        Strides domY = DomainUtil.subDomain(endoDom, endoParents);
        Strides domX = DomainUtil.subDomain(endoDom, vars);
        int m = domY.getCombinations();
        int Usize = (int)Math.pow(domX.getCombinations(), m);

        Strides domf = Strides.as(Uvar, Usize).concat(domX).concat(domY);
        BayesianFactor f = new BayesianFactor(domf);

        List Yspace = DomainUtil.getEventSpace(domY);

        int i = 0;
        for(int[] confX : DomainUtil.getEventSpace(IntStream.range(0,m).mapToObj(k->domX).toArray(Strides[]::new))){
        
            for(int k=0; k<m; k++) {
                int finalK = k;
                int[] xval = IntStream.range(0, domX.getSize()).map(j ->  confX[finalK *domX.getSize() + j]).toArray();
                EquationOps.setValue(
                        f,
                        new FIntIntHashMap(ObservationBuilder.observe(Uvar, i)),
                        new FIntIntHashMap(ObservationBuilder.observe(endoParents, (int[]) Yspace.get(k))),
                        vars,
                        xval
                );
            }
            i++;
        }
        return f;
    }

}
