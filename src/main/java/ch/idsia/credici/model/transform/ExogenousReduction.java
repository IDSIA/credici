package ch.idsia.credici.model.transform;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.HashMap;

public class ExogenousReduction {

    private StructuralCausalModel model;
    private HashMap empProbs;

    public ExogenousReduction(StructuralCausalModel model, HashMap empProbs){
        this.model = model.copy();
        this.empProbs = empProbs;
    }

    public StructuralCausalModel getModel() {
        return model;
    }

    public ExogenousReduction removeWithZeroLower(double k) {
        return reduce(k, true);
    }

    public ExogenousReduction removeWithZeroUpper() {
        return reduce(0.0, false);
    }


    private ExogenousReduction reduce(double k, boolean useLower){

        TIntIntMap minDim = new TIntIntHashMap();
        for(int exoVar : model.getExogenousVars())
            minDim.put(exoVar, (int) (k* model.getDomain(exoVar).getCardinality(exoVar)));

        for(int exoVar : model.getExogenousVars()) {
            while (model.getDomain(exoVar).getCardinality(exoVar) > minDim.get(exoVar)) {
                SparseModel vmodel = model.toVCredal(empProbs.values());
                VertexFactor f = (VertexFactor) vmodel.getFactor(exoVar);
                //System.out.println(f);
                //Get lower values
                double[] bounds;
                if(useLower)
                    bounds = new VertexToInterval().apply(f, exoVar).getLower(0);
                else
                    bounds = new VertexToInterval().apply(f, exoVar).getUpper(0);

                // Identify removable dimensions
                int[] idx = CollectionTools.shuffle(ArraysUtil.where(bounds, p -> p == 0));
                if (idx.length  == 0)
                    break;

                //Update U states
                model.removeVariable(exoVar);
                model.addVariable(exoVar, vmodel.getSize(exoVar) - 1, true);

                // inverse filter and update at children factors
                for (int ch : vmodel.getChildren(exoVar)) {
                    model.addParent(ch, exoVar);
                    model.setFactor(ch, FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(ch), exoVar, idx[0]).sampleVertex());
                }
            }
        }

        return this;

    }

    public static void main(String[] args) {
        StructuralCausalModel model = new StructuralCausalModel();

        int a = model.addVariable(2, false);
        int x = model.addVariable(2, false);
        int y = model.addVariable(2, false);
        int z = model.addVariable(2, false);

        int w = model.addVariable(2, true);
        int v = model.addVariable(4, true);
        int u = model.addVariable(16, true);

        model.addParent(x,a);
        model.addParent(y,x);
        model.addParent(z,y);

        model.addParent(a,w);
        model.addParent(x,v);
        model.addParent(y,u);
        model.addParent(z,u);


        SparseDirectedAcyclicGraph endoDag = DAGUtil.getSubDAG(model.getNetwork(), model.getEndogenousVars());
        model = CausalBuilder.of(endoDag, 2).setCausalDAG(model.getNetwork()).build();
        model.fillExogenousWithRandomFactors(3);

        TIntIntMap[] dataset = model.samples(100, model.getEndogenousVars());

        //Input
        //m;
        HashMap empProbs = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, dataset),5);
        ////

        StructuralCausalModel reducedModel = new ExogenousReduction(model, empProbs).removeWithZeroLower(0.8).removeWithZeroUpper().getModel();
        //System.out.println(reducedModel);

        SparseModel redVmodel = reducedModel.toVCredal(empProbs.values());

        for(int exoVar : reducedModel.getExogenousVars()){
            System.out.println(redVmodel.getFactor(exoVar));
        }
    }
}
