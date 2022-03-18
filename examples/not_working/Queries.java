package not_working;

import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;


import java.io.IOException;

public class Queries {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Load the empirical model
        String fileName = "./models/simple-bayes.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        System.out.println(bnet.getNetwork());

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        //Get the Credal Netoworks
        DAGModel<VertexFactor> vcredal = causalModel.toVCredal(bnet.getFactors());
        DAGModel<SeparateHalfspaceFactor> hcredal =  causalModel.toHCredal(bnet.getFactors());


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
        CredalCausalApproxLP inf = new CredalCausalApproxLP(hcredal);
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
