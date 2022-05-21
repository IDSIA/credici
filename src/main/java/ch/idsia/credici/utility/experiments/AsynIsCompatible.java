package ch.idsia.credici.utility.experiments;

import ch.idsia.credici.model.StructuralCausalModel;
import gnu.trove.map.TIntIntMap;

public class AsynIsCompatible {

    static StructuralCausalModel model;
    static TIntIntMap[] data;

    public static void setArgs(StructuralCausalModel model, TIntIntMap[] data){
        AsynIsCompatible.model = model;
        AsynIsCompatible.data = data;
    }
    public static boolean run(){
        return model.isCompatible(data);
    }

}
