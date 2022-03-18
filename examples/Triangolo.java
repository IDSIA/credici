import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;

public class Triangolo {
    public static void main(String[] args) throws IOException, InterruptedException {

        // print heap memory (change with -Xmx8g)
        System.out.println(Runtime.getRuntime().maxMemory()/Math.pow(2,20)+" MB.");

        // Load the empirical model
        String fileName = "./models/empirical_triangolo.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        System.out.println("Reading");

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        // Get the endogenous variables
        int[] x = causalModel.getEndogenousVars();

        System.out.println("Building (Causal)");

        //Get the Credal Netoworks
        DAGModel<SeparateHalfspaceFactor> hcredal = ExactCredalBuilder.of(causalModel)
                                .setToHalfSpace()
                                .setNonnegative(false)
                                .setEmpirical(bnet.getFactors())
                                .build().getModel();




        System.out.println("Building (Credal)");


        // Set query
        TIntIntMap intervention = new TIntIntHashMap();
        intervention.put(0,1);
        int target = 1;


        System.out.println("Computing (Credal)");

        // Approx inference
        CredalCausalApproxLP inf = new CredalCausalApproxLP(hcredal);
        IntervalFactor res = inf.doQuery(target, intervention);
        System.out.println(res);

        System.out.println("Computing (Credal2)");


        IntervalFactor resApprox = (IntervalFactor) inf
                .counterfactualQuery()
                .setTarget(x[6])
                .setIntervention(x[1],1)
                .setEvidence(x[1], 0)
                .run();

        System.out.println(resApprox);




    }
}
