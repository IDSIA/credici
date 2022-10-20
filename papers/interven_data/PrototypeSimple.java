import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrototypeSimple {
    public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

        int datasize = 1000;
        CausalMultiVE inf = null;
        EMCredalBuilder builder = null;




        for(int i=1;i<10; i++) {
            System.out.println("seed: "+i);
            RandomUtil.setRandomSeed(i);

            SparseDirectedAcyclicGraph endoDAG = DAGUtil.build("(0,1),(1,2)");
            SparseDirectedAcyclicGraph causalDAG = DAGUtil.build("(0,1),(1,2),(3,0),(4,1),(5,2)");
            StructuralCausalModel model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();
            model = Cofounding.mergeExoVars(model, new int[][]{new int[]{3,5}});
            model.fillExogenousWithRandomFactors(3);

            int x = 0, y = 1, z=2; // Variables in the model;
            int cause = x, effect = z;

            /* Only with observational data */
            TIntIntMap[] obsData = model.samples(datasize, model.getEndogenousVars());


            builder = EMCredalBuilder.of(model, obsData)
                    .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                    .setThreshold(0.0)
                    .setNumTrajectories(30)
                    .setWeightedEM(true)
                    .setVerbose(false)
                    .setMaxEMIter(300);

            builder.build();

            inf = new CausalMultiVE(builder.getSelectedPoints());
            VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
            //VertexFactor res_obs = ((VertexFactor) inf.counterfactualQuery().setTarget(effect).setIntervention(cause,0).run());//;.filter(effect, 0);
            System.out.println(res_obs);

            /** Adding interventional data */
            /////////////////////////////////////

            StructuralCausalModel model_x0 = model.intervention(x, 0, false);
            TIntIntMap[] intData_x0 = model_x0.samples((int)(datasize), model_x0.getEndogenousVars());




            // Extend the model
            StructuralCausalModel counterfactualModel = model.merge(model_x0);

            // Rename the variables in the interventional data
            int[] newVars = counterfactualModel.getMap().getEquivalentVars(1, model.getEndogenousVars());
            intData_x0 = DataUtil.renameVars(intData_x0, model.getEndogenousVars(), newVars);

            // Concat datasets
            TIntIntMap[] mixedData = Stream.concat(Stream.of(obsData), Stream.of(intData_x0)).toArray(TIntIntMap[]::new);
            builder = EMCredalBuilder.of(counterfactualModel, mixedData)
                    .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                    .setThreshold(0.0)
                    .setNumTrajectories(30)
                    .setWeightedEM(true)
                    .setVerbose(false)
                    .setMaxEMIter(300).build();


            List selectedPoints = builder.getSelectedPoints().stream().map(m -> {
                for (int v : newVars) m.removeVariable(v);
                return m;
            }).collect(Collectors.toList());


            inf = new CausalMultiVE(selectedPoints);

            VertexFactor res_mixed = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
            // VertexFactor res_mixed = ((VertexFactor) inf.counterfactualQuery().setTarget(effect).setIntervention(cause,0).run());//.filter(effect, 0);
            System.out.println(res_mixed);

            System.out.println("============================");

        }
    }
}
