package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexAbstractFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.inference.ve.CredalVariableElimination;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.hull.ConvexHull;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class CredalCausalVE extends CausalInference<DAGModel<VertexFactor>, VertexFactor> {


    public CredalCausalVE(StructuralCausalModel model){
        this(model.toVCredal(model.getEmpiricalProbs()));
    }


    public CredalCausalVE(StructuralCausalModel model, BayesianFactor[] empirical){
        this(model.toVCredal(empirical));
    }


    public CredalCausalVE(StructuralCausalModel model, Collection empirical){
        this(model.toVCredal(empirical));
    }

    public CredalCausalVE(DAGModel<VertexFactor> model){
        CausalInfo.assertIsVCredal(model);
        this.model = model;
    }


    @Override
    public DAGModel getInferenceModel(Query q, boolean simplify) {

        target = q.getTarget();
        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        // Get the inference model (simple mutilated or twin graph)
        DAGModel infModel=null;
        if(!q.isCounterfactual()) {
            infModel = (DAGModel) CausalOps.applyInterventions(model, intervention, true);
        }else{
            infModel = (DAGModel) CausalOps.counterfactualModel(model, intervention);
            //map the target to the alternative world
            q.setCounterfactualMapping(WorldMapping.getMap(infModel));
            target = q.getCounterfactualMapping().getEquivalentVars(1, target);
        }

        // cut arcs coming from an observed node and remove barren w.r.t the target
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = (DAGModel) removeBarren
                    .execute(new CutObserved().execute(infModel, evidence), evidence, target);
        }
        return infModel;
    }

    @Override
    public VertexFactor run(Query q) throws InterruptedException {

        //Build the inference model
        DAGModel<VertexFactor> infModel = getInferenceModel(q);

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

        FactorVariableElimination<VertexFactor> ve = new FactorVariableElimination<>(elimOrder);
        if(filteredEvidence.size()>0)
            ve.setEvidence(filteredEvidence);
        ve.setFactors(infModel.getFactors());
        ve.setNormalize(false);

        // Set the convex hull method
        ConvexHull old_method = VertexAbstractFactor.getConvexHullMarg();
        VertexAbstractFactor.setConvexHullMarg(ConvexHull.DEFAULT);

        // run the query
        VertexFactor output = ve.run(target);

        // restore the previous convex hull method
        VertexAbstractFactor.setConvexHullMarg(old_method);


        // Normalize and apply convex hull to the result
        return ((VertexFactor) ve.run(target)).normalize().convexHull(ConvexHull.DEFAULT);

    }


    public VertexFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        DAGModel<VertexFactor> reality = (DAGModel) this.getModel();
        DAGModel<VertexFactor> doTrue = (DAGModel)this.causalQuery().setIntervention(cause, trueState).getInferenceModel(false);
        DAGModel<VertexFactor> doFalse = (DAGModel)this.causalQuery().setIntervention(cause, falseState).getInferenceModel(false);

        DAGModel<VertexFactor> pns_model = (DAGModel) CausalOps.merge(reality, doTrue, doFalse);

        WorldMapping map = WorldMapping.getMap(pns_model);
        int target[] = new int[] {map.getEquivalentVars(1, effect),map.getEquivalentVars(2, effect)};

        CausalInference infInternal =  new CredalCausalVE(pns_model);
        VertexFactor prob = (VertexFactor) infInternal.causalQuery().setTarget(target).run();
        return (VertexFactor) FactorUtil.filter(FactorUtil.filter(prob, target[0], trueState), target[1], falseState);

    }

}
