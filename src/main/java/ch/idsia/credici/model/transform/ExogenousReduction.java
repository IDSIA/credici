package ch.idsia.credici.model.transform;

import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.IntStream;

public class ExogenousReduction {

    private StructuralCausalModel model;
    private HashMap empProbs;

    public ExogenousReduction(StructuralCausalModel model, HashMap empProbs){
        this.model = model.copy();
        this.empProbs = empProbs;
    }

    public ExogenousReduction(StructuralCausalModel model, TIntIntMap[] data){
        this.model = model.copy();
        this.empProbs = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, data), FactorUtil.DEFAULT_DECIMALS);
    }


    public ExogenousReduction(StructuralCausalModel model){
        this(model, (HashMap) null);
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

    public ExogenousReduction removeRedundant() {

        TIntObjectMap toRemove = new TIntObjectHashMap();
        for(int exoVar : model.getExogenousVars()) {
            int[] leftVars = model.getEndogenousChildren(exoVar);
            BayesianFactor f = BayesianFactor.combineAll(model.getFactors(leftVars));
            int[] toRemove_u = EquationOps.getRedundancies(f, exoVar, leftVars)
                    .stream()
                    .map(idx -> ArraysUtil.remove(idx, 0))
                    .flatMapToInt(Arrays::stream)
                    .boxed().sorted(Collections.reverseOrder())
                    .mapToInt(i -> i.intValue()).toArray();
            if(toRemove_u.length>0)
                toRemove.put(exoVar, toRemove_u);
        }

        for(int exoVar : toRemove.keys()) {
            model = model.dropExoState(exoVar, (int[]) toRemove.get(exoVar));
        }


        return this;
    }

    private ExogenousReduction reduce(double k, boolean useLower){

        if(this.empProbs==null)
            throw new IllegalArgumentException("Empirical probabilities are required");

        TIntIntMap minDim = new TIntIntHashMap();
        for(int exoVar : model.getExogenousVars())
            minDim.put(exoVar, (int) (k* model.getDomain(exoVar).getCardinality(exoVar)));

        SparseModel vmodel = null;
        boolean recalculate = true;

        for(int exoVar : model.getExogenousVars()) {
            while (model.getDomain(exoVar).getCardinality(exoVar) > minDim.get(exoVar)) {

                if(recalculate)
                    vmodel = model.toVCredal(empProbs.values());

                VertexFactor f = (VertexFactor) vmodel.getFactor(exoVar);
                //Get lower values
                double[] bounds;
                if(useLower)
                    bounds = new VertexToInterval().apply(f, exoVar).getLower(0);
                else
                    bounds = new VertexToInterval().apply(f, exoVar).getUpper(0);

                // Identify removable dimensions
                int[] idx = CollectionTools.shuffle(ArraysUtil.where(bounds, p -> p == 0));
                if (idx.length==0) {
                    recalculate = false;
                    break;
                }
                model = model.dropExoState(exoVar, idx[0]);
                recalculate = true;

            }
        }

        return this;

    }

    private static BayesianFactor sampleVertex(VertexFactor this_) {

        int left_comb = this_.getSeparatingDomain().getCombinations();

        int idx[] = IntStream.range(0,left_comb)
                .map(i-> RandomUtil.getRandom().nextInt(this_.getVerticesAt(i).length))
                .toArray();

        double[] data =
                Doubles.concat(
                        IntStream.range(0,left_comb)
                                .mapToObj(i -> this_.getVerticesAt(i)[RandomUtil.getRandom().nextInt(this_.getVerticesAt(i).length)])
                                .toArray(double[][]::new)
                );


        Strides newDomain = this_.getDataDomain().concat(this_.getSeparatingDomain());
        return new BayesianFactor(newDomain, data);
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

        StructuralCausalModel reducedModel =
                new ExogenousReduction(model, empProbs)
                        .removeRedundant()
        //.removeWithZeroLower(0.8)
                        //.removeWithZeroUpper()
                        .getModel();
        //System.out.println(reducedModel);

        SparseModel redVmodel = reducedModel.toVCredal(empProbs.values());

        for(int exoVar : reducedModel.getExogenousVars()){
            System.out.println(redVmodel.getFactor(exoVar));
        }
    }
}
