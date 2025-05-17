package ch.idsia.credici.inference;

import ch.idsia.credici.IO;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;


public class ProbabilisticVE{//<M extends GraphicalModel<GenericFactor>> implements Inference<M, GenericFactor> {

        private GenericSparseModel model;

        public ProbabilisticVE(GenericSparseModel model){
            this.model = model;
        }


        public GenericSparseModel getInferenceModel(int[] target, TIntIntMap evidence) {
            CutObserved cutObserved = new CutObserved();
            // run making a copy of the model
            GenericSparseModel infModel = cutObserved.execute(model, evidence);

            RemoveBarren removeBarren = new RemoveBarren();
            // no more need to make a copy of the model
            removeBarren.executeInline(infModel, target, evidence);


            return infModel;

        }

        /**
         * Query K(target|evidence) in the model provided to the constructor
         *
         * @param target int the target variable
         * @param evidence {@link TIntIntMap} a map of evidence in the form variable-state
         * @return
         */

        public GenericFactor query(int[] target, TIntIntMap evidence) {

            if(evidence == null) evidence =  new TIntIntHashMap();

            if(ArraysUtil.intersection(evidence.keys(), target).length>0)
                throw new IllegalArgumentException("Intersection non empty on evidence and target variables");

            GenericSparseModel infModel =  getInferenceModel(target, evidence);

            TIntIntMap filteredEvidence = new TIntIntHashMap(evidence);

            // update the evidence
            for(int v: evidence.keys()){
                if(ArrayUtils.contains(infModel.getVariables(), v)){
                    filteredEvidence.put(v, evidence.get(v));
                }
            }

            MinFillOrdering minfill = new MinFillOrdering();
            int[] order = minfill.apply(infModel);

            FactorVariableElimination ve = new FactorVariableElimination(order);
            ve.setEvidence(filteredEvidence);
            ve.setFactors(infModel.getFactors());
            //ve.setNormalize(false);

            GenericFactor output = ve.run(target);

            return output;
        }

        public GenericFactor query(int[] target, TIntIntMap evidenceSimple, TIntObjectHashMap evidenceMulti) {
            if(evidenceMulti == null) return query(target, evidenceSimple);
            if(evidenceSimple == null) evidenceSimple =  new TIntIntHashMap();


            //if(ArraysUtil.intersection(target, evidenceMulti.keys()))

            int[] newTarget = ArraysUtil.union(target, evidenceMulti.keys());

            GenericFactor p1 = query(newTarget, evidenceSimple);

            for(int v : evidenceMulti.keys())
                p1 = FactorUtil.filterMultiStates(p1, v, (int[]) evidenceMulti.get(v));


            GenericFactor p2 = ((Factor)p1).marginalize(target);

            GenericFactor pout = null;
            if(p1 instanceof VertexFactor)
                pout = FactorUtil.divisionVertexFactor((VertexFactor) p1, (VertexFactor) p2);
            else if(p1 instanceof BayesianFactor)
                pout =  ((BayesianFactor) p1).divide((BayesianFactor) p2);
            else throw new IllegalArgumentException("Wrong factor type");

            return pout;
        }



        public static void main(String[] args) throws IOException {
            BayesianNetwork bnet = (BayesianNetwork) IO.read("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/models/asia_dummy_states.uai");
            System.out.println(bnet);


            ProbabilisticVE inf = new ProbabilisticVE(bnet);

            BayesianFactor p1 = null, p2 = null;

            for(int x : bnet.getVariables()) {
                for(int y : new int[]{2,7}) {
                    if(x!=y) {
                        int[] target = new int[]{x};

                        p1 = (BayesianFactor) inf.query(target, ObservationBuilder.observe(y, 1));

                        TIntObjectHashMap obsmulti = new TIntObjectHashMap();
                        obsmulti.put(y, new int[]{1, 2});
                        p2 = (BayesianFactor) inf.query(target, new TIntIntHashMap(), obsmulti);


                        System.out.println(p1.getData()[0] + " " + p2.getData()[0]);
                    }
                }
            }

        }
}
/*
0[0] - [1, 2]
1[1] - [1, 2]
2[2] - [1, 3]
3[3] - [1, 2]
4[4] - [1, 2]
5[5] - [1, 2]
6[6] - [1, 2]
7[7] - [1, 3]
 */