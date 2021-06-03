package edu.neurips.causalem.inference;

import edu.neurips.causalem.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.ObservationBuilder;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;

/**
 * Author:
 */
public abstract class CausalInference<M, R extends GenericFactor>{

    protected M model;

    protected int[] target;

    public abstract R run(Query q) throws InterruptedException;

    public R query(int[] target, TIntIntMap evidence, TIntIntMap intervention) throws InterruptedException {
        Query q = causalQuery()
                    .setTarget(target)
                    .setEvidence(evidence)
                    .setIntervention(intervention);

        if(this instanceof CredalCausalApproxLP)
            q.setEpsilon(((CredalCausalApproxLP) this).getEpsilon());

        return run(q);
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

    public abstract M getInferenceModel(Query q, boolean simplify);

    public M getInferenceModel(Query q) {
        return getInferenceModel(q, true);
    }



    public Query causalQuery(){
        return new Query<M,R>(this).setCounterfactual(false);
    }

    public Query counterfactualQuery(){
        return new Query<M,R>(this).setCounterfactual(true);
    }

    public R probNecessity(int cause, int effect) throws InterruptedException {
        return probNecessity(cause, effect, 0,1);
    }
    public R probNecessity(int cause, int effect, int trueState, int falseState) throws InterruptedException {

        Query q = this.counterfactualQuery()
                .setTarget(effect)
                .setIntervention(cause, falseState)
                .setEvidence(ObservationBuilder.observe(new int[]{effect, cause}, new int[]{trueState, trueState}))
                .setTarget(effect);

        R res = (R) q.run();
        int var = q.getCounterfactualMapping().getEquivalentVars(1,effect);
        res = (R) FactorUtil.filter(res, var, falseState);

        return res;

    }

    public R probSufficiency(int cause, int effect) throws InterruptedException {
        return probSufficiency(cause, effect, 0,1);
    }
    public R probSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException {

        Query q = this.counterfactualQuery()
                .setTarget(effect)
                .setIntervention(cause, trueState)
                .setEvidence(ObservationBuilder.observe(new int[]{effect, cause}, new int[]{falseState, falseState}))
                .setTarget(effect);

        R res = (R) q.run();
        int var = q.getCounterfactualMapping().getEquivalentVars(1,effect);
        res = (R) FactorUtil.filter(res, var, trueState);

        return res;

    }

    public R probNecessityAndSufficiency(int cause, int effect) throws InterruptedException, ExecutionControl.NotImplementedException {
        return this.probNecessityAndSufficiency(cause, effect, 0, 1);
    }

    public R probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        throw new ExecutionControl.NotImplementedException("Not implemented");
    }

}
