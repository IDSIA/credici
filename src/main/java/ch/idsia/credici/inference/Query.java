package ch.idsia.credici.inference;

import ch.idsia.crema.factor.GenericFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class Query<R extends GenericFactor> {

    private CausalInference inf;

    private boolean isCounterfactual = false;

    private int[] target;

    private TIntIntMap evidence = new TIntIntHashMap();

    private TIntIntMap intervention = new TIntIntHashMap();


    public Query(CausalInference inf){
        this.inf = inf;
    }

    public Query setCounterfactual(boolean counterfactual) {
        isCounterfactual = counterfactual;
        return this;
    }

    public Query setTarget(int... target) {
        this.target = target;
        return this;
    }

    public Query setEvidence(TIntIntMap evidence) {
        this.evidence = evidence;
        return this;
    }

    public Query setIntervention(TIntIntMap intervention) {
        this.intervention = intervention;
        return this;
    }

    public CausalInference getInf() {
        return inf;
    }

    public int[] getTarget() {
        return target;
    }

    public TIntIntMap getEvidence() {
        return evidence;
    }

    public TIntIntMap getIntervention() {
        return intervention;
    }


    public R run() throws InterruptedException {
        if(inf == null)
            throw new IllegalArgumentException("causal inference engine is not set");

        return (R) inf.run(this);

    }
}
