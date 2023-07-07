package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;

public class EmpiricalCausalVE extends CausalInference<BayesianNetwork, GenericFactor> {

    public EmpiricalCausalVE(StructuralCausalModel causalModel, TIntIntMap[] data){
        this.causalModel = causalModel;
        this.model = causalModel.getEmpiricalNet(data);
    }


    public EmpiricalCausalVE(StructuralCausalModel causalModel){
        this.causalModel = causalModel;
        this.model = causalModel.getEmpiricalNet();
    }



    @Override
    public BayesianNetwork getInferenceModel(Query q, boolean simplify) {

        target = q.getTarget();
        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        // Get the inference model (simple mutilated or twin graph)
        BayesianNetwork infModel=null;
        if(!q.isCounterfactual()) {
            infModel = (BayesianNetwork) CausalOps.applyInterventions(model, intervention);
        }else{
            throw new NotImplementedException("Counterfactual queries not implemented");
        }

        // cut arcs coming from an observed node and remove barren w.r.t the target
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = removeBarren
                    .execute(new CutObserved().execute(infModel, evidence), target, evidence);
        }
        return infModel;
    }

    private void validateQuery(Query q){
        if(!q.isIdentifiable())
            throw new IllegalArgumentException("Non-identifiable queries cannot be calculated with this method.");

        for(int x: q.getIntervention().keys()){
            int[] U = causalModel.getExogenousParents(x);
            if(U.length!=1 || causalModel.getEndogenousChildren(U[0]).length!=1)
                throw new NotImplementedException("Not implemented yet for cofounded intervened variables");
        }

    }

    @Override
    public BayesianFactor run(Query q) throws InterruptedException {

        validateQuery(q);

        //Build the inference model
        BayesianNetwork infModel = getInferenceModel(q);

        // Update the evidence
        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: q.getEvidence().keys()){
            if(ArrayUtils.contains(infModel.getVariables(), v)){
                filteredEvidence.put(v, q.getEvidence().get(v));
            }
        }
        // Get the  elimination order
        int[] elimOrder = new MinFillOrdering().apply(infModel);
        elimOrder = ArraysUtil.unique(Ints.concat(elimOrder, infModel.getVariables()));


        FactorVariableElimination ve = new FactorVariableElimination(elimOrder);
        if(filteredEvidence.size()>0)
            ve.setEvidence(filteredEvidence);
        ve.setNormalize(false);
        ve.setFactors(infModel.getFactors());
        return ((BayesianFactor) ve.run(target)).normalize();

    }

    public IntervalFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        throw new NotImplementedException("");
    }

    public IntervalFactor probNecessity(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        throw new NotImplementedException("");
    }
    public IntervalFactor probSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        throw new NotImplementedException("");
    }
}
