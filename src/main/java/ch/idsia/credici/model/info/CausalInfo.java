package ch.idsia.credici.model.info;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CredalBuilder;
import ch.idsia.credici.model.predefined.Party;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.stream.IntStream;

public class CausalInfo {

    SparseModel model;

    /**
     * Constructor of a CausalInfo with a model associated
     * @param model
     */
    public CausalInfo(SparseModel model){
        this.model = model;
    }

    private static TIntObjectHashMap<CausalInfo> builders = new TIntObjectHashMap<>();

    public static CausalInfo of(SparseModel model){
        int hash = model.hashCode();
        if(!builders.containsKey(hash))
            builders.put(hash, new CausalInfo(model)) ;
        return builders.get(hash);
    }

    public int[] getExogenousVars(){
        return IntStream.of(model.getVariables()).filter(v -> isExogenous(v)).toArray();
    }

    public int[] getEdogenousVars(){
        return IntStream.of(model.getVariables()).filter(v -> isEndogenous(v)).toArray();
    }

    public int[] getEndogenousParents(int v){
        return ArraysUtil.intersection(model.getParents(v), this.getEdogenousVars());
    }

    public int[] getEndogenousChildren(int v) {
        return ArraysUtil.intersection(model.getChildren(v), this.getEdogenousVars());
    }

    public int[] getExogenousParents(int v){
        return ArraysUtil.intersection(model.getParents(v), this.getExogenousVars());
    }


    public boolean isEndogenous(int v){
       return !isExogenous(v);
    }

    public boolean isExogenous(int v){
        return model.getParents(v).length == 0;
    }




    public static void main(String[] args) {

        StructuralCausalModel causalModel = Party.buildModel();
        SparseModel m = causalModel.toVCredal(causalModel.getEmpiricalProbs());

        System.out.println(Arrays.toString(CausalInfo.of(m).getEdogenousVars()));
        System.out.println(Arrays.toString(CausalInfo.of(m).getExogenousParents(0)));

    }


}
