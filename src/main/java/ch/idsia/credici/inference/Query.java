package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;

public class Query<M, R extends GenericFactor> {

    private CausalInference inf;

    private boolean isCounterfactual = false;

    private WorldMapping counterfactualMapping = null;

    private int[] target;

    private TIntIntMap evidence = new TIntIntHashMap();

    private TIntIntMap intervention = new TIntIntHashMap();

    private double epsilon = 0.0;


    public Query(CausalInference inf) {
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

    public Query setEvidence(int var, int state) {
        return setEvidence(ObservationBuilder.observe(var, state));
    }

    public Query setIntervention(int var, int state) {
        return setIntervention(ObservationBuilder.observe(var, state));
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

    public boolean isCounterfactual() {
        return isCounterfactual;
    }

    public WorldMapping getCounterfactualMapping() {
        return counterfactualMapping;
    }

    public void setCounterfactualMapping(WorldMapping counterfactualMapping) {
        this.counterfactualMapping = counterfactualMapping;
    }

    public Query setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public R run() throws InterruptedException {
        if (inf == null)
            throw new IllegalArgumentException("causal inference engine is not set");
        return (R) inf.run(this);

    }

    public M getInferenceModel(boolean simplify) throws InterruptedException {
        if (inf == null)
            throw new IllegalArgumentException("causal inference engine is not set");
        return (M) inf.getInferenceModel(this, simplify);

    }

    public M getInferenceModel() throws InterruptedException {
        return getInferenceModel(true);
    }


    public boolean isIdentifiable(){
        StructuralCausalModel model = (StructuralCausalModel) this.inf.getModel();

        if (this.isCounterfactual())
            return false;

        int[] X = this.getIntervention().keys();

        if(X.length==0) return true;
        if(X.length>1)
            throw new NotImplementedException("Not implemented for more than one intervention.");

        int chX[] = model.getChildren(X[0]);

        int[] endoComp =
                model.endoConnectComponents()
                        .stream()
                        .filter(c -> ArraysUtil.contains(X[0], c)).findFirst().get();
        return ArraysUtil.intersection(chX, endoComp).length==0;

    }
}