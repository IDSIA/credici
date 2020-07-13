import ch.idsia.credici.inference.CredalCausalAproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;

public class Queries {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Load the empirical model
        String fileName = "./models/simple-bayes.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        //Get the Credal Netoworks
        SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal =  causalModel.toHCredal(bnet.getFactors());


        //Get variables
        int[] x = causalModel.getEndogenousVars();

        //Exact inference
        CredalCausalVE inf2 = new CredalCausalVE(vcredal);

        VertexFactor res2 = (VertexFactor) inf2.causalQuery()
                                                .setTarget(x[1])
                                                .setIntervention(x[0],1)
                                                .run();
        System.out.println(res2);



        // Approx inference
        CredalCausalAproxLP inf = new CredalCausalAproxLP(hcredal);
        IntervalFactor res = (IntervalFactor) inf.causalQuery()
                                                .setTarget(x[1])
                                                .setIntervention(x[0],1)
                                                .run();
        System.out.println(res);



        ////// Counterfactual queries //////

        //Exact inference
        res2 = (VertexFactor) inf2.counterfactualQuery()
                                .setTarget(x[1])
                                .setIntervention(x[0],1)
                                .setEvidence(x[1], 0)
                                .run();
        System.out.println(res2);



        // Approximate
        IntervalFactor res3 = (IntervalFactor) inf.counterfactualQuery()
                                                    .setTarget(x[1])
                                                    .setIntervention(x[0],1)
                                                    .setEvidence(x[1], 0)
                                                    .run();
        System.out.println(res3);




    }
}
