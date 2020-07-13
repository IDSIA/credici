package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Author:  Rafael Cabañas
 */
public abstract class CausalInference<M extends GenericSparseModel, R extends GenericFactor>{

    protected M model;

    protected int[] target;

    public abstract R run(Query q) throws InterruptedException;

    public R query(int[] target, TIntIntMap evidence, TIntIntMap intervention) throws InterruptedException {
        return run(causalQuery()
                .setTarget(target)
                .setEvidence(evidence)
                .setIntervention(intervention));
    }

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

    public abstract M getInferenceModel(Query q);


    public Query causalQuery(){
        return new Query<M,R>(this).setCounterfactual(false);
    }

    public Query counterfactualQuery(){
        return new Query<M,R>(this).setCounterfactual(true);
    }


}
