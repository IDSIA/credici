package ch.idsia.credici.model.transform;

import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalGraphTools;
import ch.idsia.credici.utility.DomainUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Cofounding {

    public static SparseDirectedAcyclicGraph splitExoCofounders(SparseDirectedAcyclicGraph dag) {
        return splitExoCofounders(dag, new ArrayList<>());
    }
    public static SparseDirectedAcyclicGraph splitExoCofounders(SparseDirectedAcyclicGraph dag, List<int[]> splittedToFill){

        SparseDirectedAcyclicGraph outDag = dag.copy();
        int v = Arrays.stream(outDag.getVariables()).max().getAsInt() + 1;

        for (int u: CausalGraphTools.getExogenous(outDag)){
            int[] chU = CausalGraphTools.getEndogenousChildren(outDag, u);
            if(chU.length>1){
                int[] splitted = new int[chU.length];
                splitted[0] = u;
                for(int i=1; i< chU.length; i++){
                    outDag.addVariable(v);
                    outDag.removeLink(u, chU[i]);
                    outDag.addLink(v, chU[i]);
                    splitted[i] = v;
                    v++;
                }
                splittedToFill.add(splitted);
            }

        }
        return  outDag;

    }


    public static StructuralCausalModel mergeExoParents(StructuralCausalModel model, int[][] pairsX){

        // Merge the cofounded endo vars
        for (int[] p : pairsX) {
            if(model.isExogenous(p[0]) || model.isExogenous(p[1]))
                throw new IllegalArgumentException("Pair with exogenous variable: "+Arrays.toString(p));
            int u = model.getExogenousParents(p[0])[0];
            int v = model.getExogenousParents(p[1])[0];
            model = Cofounding.mergeOperation(model, u, v);
        }

        // Rename the variables so that all the variables are consecutive
        return fixVariableIDs(model);
    }


    public static StructuralCausalModel mergeExoVars(StructuralCausalModel model, int[][] pairsU){

        // Merge the cofounded endo vars
        for (int[] p : pairsU) {
            if(model.isEndogenous(p[0]) || model.isEndogenous(p[1]))
                throw new IllegalArgumentException("Pair with endogenous variable: "+Arrays.toString(p));
            model = Cofounding.mergeOperation(model, p[0], p[1]);
        }

        // Rename the variables so that all the variables are consecutive
        return fixVariableIDs(model);
    }



    private static StructuralCausalModel fixVariableIDs(StructuralCausalModel model) {
        TIntIntHashMap map = new TIntIntHashMap();
        int i = 0;
        for (int v = 0; v <= Arrays.stream(model.getVariables()).max().getAsInt(); v++) {
            if (ArraysUtil.contains(v, model.getVariables())) {
                if (v != i) map.put(v, i);
                i++;
            }
        }

        StructuralCausalModel newModel = model.copy();
        for (int v : map.keys())
            newModel.removeVariable(v);

        for (int v : map.keys()) {
            newModel.addVariable(map.get(v), model.getSize(v), model.isExogenous(v));
            // Assume that the new variable is root
            for (int ch : model.getChildren(v)) {
                newModel.addParent(ch, map.get(v));
                BayesianFactor f = model.getFactor(ch);
                int[] newVars = IntStream.of(f.getDomain().getVariables()).map(j -> {
                    if (j == v) return map.get(v);
                    return j;
                }).toArray();
                newModel.setFactor(ch, f.renameDomain(newVars));
            }
        }
        return newModel;
    }


    private static StructuralCausalModel mergeOperation(StructuralCausalModel model, int exoVar1, int exoVar2){
        if(!(model.isExogenous(exoVar1) && model.isExogenous(exoVar2) && exoVar1 != exoVar2 &&
                model.getChildren(exoVar1).length == 1 && model.getChildren(exoVar2).length==1))
            throw new IllegalArgumentException("Merge is not applicable to this model");

        int endoChild1 = model.getChildren(exoVar1)[0];
        int endoChild2 = model.getChildren(exoVar2)[0];

        Strides domU = model.getDomain(exoVar1);
        Strides domV = model.getDomain(exoVar2);

        BayesianFactor fu = model.getFactor(endoChild1);
        BayesianFactor fv = model.getFactor(endoChild2);


        int T = -1;
        for(int i=0; i<=Ints.max(model.getVariables())+1; i++) {
            if (!ArraysUtil.contains(i, model.getVariables()) || i==exoVar1 || i==exoVar2) {
                T = i;
                break;
            }
        }


        int sizeT = domU.getCombinations() * domV.getCombinations();

        // Initialize new SEs
        BayesianFactor f1 = new BayesianFactor(Strides.as(T,sizeT).concat(fu.getDomain().sort().remove(exoVar1)));
        BayesianFactor f2 = new BayesianFactor(Strides.as(T,sizeT).concat(fv.getDomain().sort().remove(exoVar2)));


        int i = 0;
        for(int[] uv : DomainUtil.getEventSpace(domU,domV)){
            TIntIntHashMap valT = ObservationBuilder.observe(T,i);
            TIntIntHashMap valU = ObservationBuilder.observe(exoVar1,uv[0]);
            TIntIntHashMap valV = ObservationBuilder.observe(exoVar2,uv[1]);

            EquationOps.setValues(f1, valT, endoChild1, EquationOps.getValue(fu, valU, new int[]{endoChild1}));
            EquationOps.setValues(f2, valT, endoChild2, EquationOps.getValue(fv, valV, new int[]{endoChild2}));

            i++;
        }


        // New model structure
        StructuralCausalModel mergedModel = model.copy();
        mergedModel.removeVariable(exoVar1);
        mergedModel.removeVariable(exoVar2);
        mergedModel.addVariable(T, sizeT, true);

        // Update the structural equations
        mergedModel.setFactor(endoChild1, f1);
        mergedModel.setFactor(endoChild2, f2);

        return mergedModel;

    }
}
