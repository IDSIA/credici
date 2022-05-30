package ch.idsia.credici.utility.experiments;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.ExogenousReduction;
import gnu.trove.map.TIntIntMap;

public class AsynReduce {

    static StructuralCausalModel model;
    static TIntIntMap[] data;
    static double reductionK;

    public static void setArgs(StructuralCausalModel model, TIntIntMap[] data, double reductionK){
        AsynReduce.model = model;
        AsynReduce.data = data;
        AsynReduce.reductionK = reductionK;
    }
    public static StructuralCausalModel run(){
        ExogenousReduction reducer = new ExogenousReduction(AsynReduce.model, AsynReduce.data)
                .removeRedundant()
                .removeWithZeroUpper();
        if(reductionK<1.0)
            reducer = reducer.removeWithZeroLower(reductionK);

        return reducer.getModel();

    }

}
