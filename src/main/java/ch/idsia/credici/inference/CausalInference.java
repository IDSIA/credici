package ch.idsia.credici.inference;

import java.util.Arrays;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;

/**
 * Author:  Rafael Cabañas
 */
public abstract class CausalInference<M, R extends GenericFactor>{

    protected StructuralCausalModel causalModel;

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

    public StructuralCausalModel getCausalModel() {
        return causalModel;
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
                .setIntervention(cause, falseState)
                .setEvidence(ObservationBuilder.observe(new int[]{effect, cause}, new int[]{trueState, trueState}))
                .setTarget(effect);

        R res = (R) q.run();
        int var = q.getCounterfactualMapping().getEquivalentVars(1,effect);
        res = (R) FactorUtil.filter(res, var, falseState);

        return res;

    }


    public R probDisablement(int cause, int effect, int trueState, int falseState) throws InterruptedException {

        Query q = this.counterfactualQuery()
                .setIntervention(cause, falseState)
                .setEvidence(ObservationBuilder.observe(new int[]{effect}, new int[]{trueState}))
                .setTarget(effect);

        R res = (R) q.run();
        int var = q.getCounterfactualMapping().getEquivalentVars(1,effect);
        res = (R) FactorUtil.filter(res, var, falseState);

        return res;

    }
    public R probDisablement(int cause, int effect) throws InterruptedException {
        return probDisablement(cause, effect, 0, 1);
    }


    public R probEnablement(int cause, int effect, int trueState, int falseState) throws InterruptedException {

        Query q = this.counterfactualQuery()
                .setIntervention(cause, trueState)
                .setEvidence(ObservationBuilder.observe(new int[]{effect}, new int[]{falseState}))
                .setTarget(effect);

        R res = (R) q.run();
        int var = q.getCounterfactualMapping().getEquivalentVars(1,effect);
        res = (R) FactorUtil.filter(res, var, trueState);

        return res;

    }
    public R probEnablement(int cause, int effect) throws InterruptedException {
        return probEnablement(cause, effect, 0, 1);
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


    public GenericFactor averageCausalEffects(int cause, int effect) throws InterruptedException {
        return averageCausalEffects(cause,effect, 1,1,0);
    }
    public GenericFactor averageCausalEffects(int cause, int effect, int effectVal, int causeVal1, int causeVal2) throws InterruptedException {


        GenericFactor p1 = this.causalQuery().setIntervention(cause, causeVal1).setTarget(effect).run();
        GenericFactor p2 = this.causalQuery().setIntervention(cause, causeVal2).setTarget(effect).run();

        GenericFactor ace = null;

        // Check the output type
        if (!(p1 instanceof BayesianFactor) && !(p1 instanceof  VertexFactor) && !(p1 instanceof ch.idsia.crema.factor.credal.linear.IntervalFactor))
            throw new IllegalArgumentException("Wrong factor type");


        if(p1 instanceof BayesianFactor){
            double diff = ((BayesianFactor)p1).filter(effect, effectVal).getValueAt(0)
                    - ((BayesianFactor)p2).filter(effect, effectVal).getValueAt(0);
            ace = new BayesianFactor(Strides.empty(), new double[]{diff});
        }else{
            if(p1 instanceof  VertexFactor){
                p1 = new VertexToInterval().apply((VertexFactor) p1, effect);
                p2 = new VertexToInterval().apply((VertexFactor) p2, effect);
            }

            // Filter the value of the effect
            double maxp1 = ((ch.idsia.crema.factor.credal.linear.IntervalFactor)p1).getDataUpper()[0][effectVal];
            double minp1 = ((ch.idsia.crema.factor.credal.linear.IntervalFactor)p1).getDataLower()[0][effectVal];
            double maxp2 = ((ch.idsia.crema.factor.credal.linear.IntervalFactor)p2).getDataUpper()[0][effectVal];
            double minp2 = ((ch.idsia.crema.factor.credal.linear.IntervalFactor)p2).getDataLower()[0][effectVal];


            double[] vals = new double [] {maxp1 - maxp2, maxp1 - minp2, minp1-maxp2, minp1 - minp2};

            ace = new ch.idsia.crema.factor.credal.linear.IntervalFactor(Strides.empty(), Strides.empty());
            ((ch.idsia.crema.factor.credal.linear.IntervalFactor)ace).setUpper(new double[]{Arrays.stream(vals).max().getAsDouble()});
            ((IntervalFactor)ace).setLower(new double[]{Arrays.stream(vals).min().getAsDouble()});

        }

        return ace;

    }

}
