import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.io.IOException;

public class PartyCounterfactuals {
    public static void main(String[] args) throws IOException, InterruptedException {


        //Load the empirical model
        BayesianNetwork bnet = (BayesianNetwork) IO.read("models/party_empirical.uai");

        // Build the causal model
        StructuralCausalModel causalModel = CausalBuilder.of(bnet).build();

        // Get the endogenous variables
        int[] x = causalModel.getEndogenousVars();

        // Convert the causal models into credal networks
        SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal = causalModel.toHCredal(bnet.getFactors());


        /////// Causal query P(X3 | do(X2 = 1))

        // Set up the exact inference engine
        CredalCausalVE infExact = new CredalCausalVE(vcredal);

        // Set up and run a causal query
        VertexFactor resExact = (VertexFactor) infExact
                .causalQuery()
                .setTarget(x[3])
                .setIntervention(x[1],1)
                .run();

        System.out.println(resExact);


        // Set up the approximate inference engine
        CredalCausalApproxLP infApprox = new CredalCausalApproxLP(hcredal);

        // Set up and run a causal query
        IntervalFactor resApprox = (IntervalFactor) infApprox
                .causalQuery()
                .setTarget(x[3])
                .setIntervention(x[1],1)
                .run();

        System.out.println(resApprox);


        /////// Counterfactual query P(X3' | do(X2 = 1), X2=0)

        //Exact inference

        resExact = (VertexFactor) infExact
                .counterfactualQuery()
                .setTarget(x[3])
                .setIntervention(x[1],1)
                .setEvidence(x[1], 0)
                .run();


        System.out.println(resExact);


        // Set up and run a counterfactual query
        resApprox = (IntervalFactor) infApprox
                .counterfactualQuery()
                .setTarget(x[3])
                .setIntervention(x[1],1)
                .setEvidence(x[1], 0)
                .run();

        System.out.println(resApprox);
    }
}
