package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Author:  Rafael Cabañas
 */
public abstract class CausalInference<M extends GenericSparseModel, R extends GenericFactor>{

    protected M model;

    public abstract R query(int[] target, TIntIntMap evidence, TIntIntMap intervention) throws InterruptedException;

    public R query(int target) throws InterruptedException {
        return query(new int[]{target}, new TIntIntHashMap(), new TIntIntHashMap());
    }
    public R query(int[] target) throws InterruptedException {
        return query(target, new TIntIntHashMap(), new TIntIntHashMap());
    }
    public R query(int target,  TIntIntMap evidence) throws InterruptedException {
        return query(new int[]{target}, evidence, new TIntIntHashMap());
    }
    public R query(int[] target,  TIntIntMap evidence) throws InterruptedException {
        return query(target, evidence, new TIntIntHashMap());
    }
    public R query(int target,  TIntIntMap evidence, TIntIntMap intervention) throws InterruptedException {
        return query(new int[]{target}, evidence, intervention);
    }


    public R doQuery(int target, TIntIntMap intervention) throws InterruptedException {
        return query(new int[]{target}, new TIntIntHashMap(), intervention);
    }

    public R doQuery(int[] target, TIntIntMap intervention) throws InterruptedException {
        return query(target, new TIntIntHashMap(), intervention);
    }

    public M getModel() {
        return model;
    }

    public  M applyInterventions(TIntIntMap intervention){
        GenericSparseModel do_model = model;
        for(int i=0; i<intervention.size(); i++) {
            do_model = do_model.intervention(intervention.keys()[i], intervention.values()[i]);
        }
        return (M) do_model;
    }



}
