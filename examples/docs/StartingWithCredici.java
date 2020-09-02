package docs;

import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.io.IOException;

public class StartingWithCredici {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Load the empirical model
        BayesianNetwork bnet = (BayesianNetwork) IO.read("models/simple-chain.uai");

        // Build the causal model
        StructuralCausalModel causalModel = CausalBuilder.of(bnet).build();

        // Set up the inference engine
        CredalCausalApproxLP inf = new CredalCausalApproxLP(causalModel, bnet.getFactors());

        // Run the query
        IntervalFactor res = (IntervalFactor) inf.counterfactualQuery()
                .setTarget(2)
                .setIntervention(0,0)
                .setEvidence(2, 1)
                .run();

    }
}
