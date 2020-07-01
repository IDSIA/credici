package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class CredalCausalVE extends CausalInference<SparseModel, VertexFactor> {

    private int[] elimOrder;

    public CredalCausalVE(StructuralCausalModel model){
        assertTrueMarginals(model);
        this.model = model.toVCredal(model.getEmpiricalProbs());
        this.elimOrder = this.model.getVariables();
    }


    public CredalCausalVE(StructuralCausalModel model, BayesianFactor[] empirical){
        this.model = model.toVCredal(empirical);
        this.elimOrder = this.model.getVariables();

    }


    public CredalCausalVE(StructuralCausalModel model, Collection empirical){
        this.model = model.toVCredal(empirical);
        this.elimOrder = this.model.getVariables();

    }


    @Override
    public VertexFactor query(int[] target, TIntIntMap evidence, TIntIntMap intervention) {

        SparseModel do_csmodel = applyInterventions(intervention);

    // cut arcs coming from an observed node and remove barren w.r.t the target
        RemoveBarren removeBarren = new RemoveBarren();
        do_csmodel = removeBarren
                .execute(new CutObserved().execute(do_csmodel, evidence), target, evidence);

        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: evidence.keys()){
            if(ArrayUtils.contains(do_csmodel.getVariables(), v)){
                filteredEvidence.put(v, evidence.get(v));
            }
        }
        // Get the new elimination order
        int[] newElimOrder = ArraysUtil.intersection(elimOrder, do_csmodel.getVariables());

        System.out.println("deleted = "+ Arrays.toString(removeBarren.getDeleted()));
        FactorVariableElimination ve = new FactorVariableElimination(newElimOrder);
        if(filteredEvidence.size()>0)
            ve.setEvidence(filteredEvidence);
        ve.setNormalize(false);
        VertexFactor.CONVEX_HULL_MARG = true;
        ve.setFactors(do_csmodel.getFactors());
        return ((VertexFactor) ve.run(target)).normalize().convexHull(true);

    }


    public CredalCausalVE setElimOrder(int[] elimOrder) {
        this.elimOrder = elimOrder;
        return this;
    }

    public int[] getElimOrder() {
        return elimOrder;
    }
}
