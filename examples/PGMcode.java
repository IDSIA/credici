import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CredalCausalAproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.io.IOException;

public class PGMcode {
    public static void main(String[] args) throws IOException, InterruptedException {

        ///// Code 1

        //Load the empirical model
        BayesianNetwork bnet = (BayesianNetwork) IO.read("models/party-empirical.uai");

        // Build the causal model
        StructuralCausalModel causalModel = CausalBuilder.of(bnet).build();

        // Get the endogenous and exogenous variables
        int[] x = causalModel.getEndogenousVars();
        int[] u = causalModel.getEndogenousVars();


        //// Code 2

        // Convert the causal models into credal networks
        SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal = causalModel.toHCredal(bnet.getFactors());


        // Access to the equations
        VertexFactor fx0 = (VertexFactor) vcredal.getFactor(x[0]);
        SeparateHalfspaceFactor fx0_ = (SeparateHalfspaceFactor) hcredal.getFactor(x[0]);

        // Access to the credal sets of the exogenous variables
        VertexFactor pu0 = (VertexFactor) vcredal.getFactor(u[0]);
        SeparateHalfspaceFactor pu0_ = (SeparateHalfspaceFactor) hcredal.getFactor(u[0]);



        // Code 3

        // Exact inference engine
        CausalInference infExact =
                new CredalCausalVE(causalModel, bnet.getFactors());

        // Approximate inference engine
        CausalInference infApprox =
                new CredalCausalAproxLP(causalModel, bnet.getFactors());


        /////// Causal query P(X3 | do(X2 = 1))

        // Code 4

        // Set up and run a causal query
        VertexFactor resExact = (VertexFactor) infExact
                .causalQuery()
                .setTarget(x[3])
                .setIntervention(x[2],1)
                .run();

        // Set up an run a counterfactual query
        IntervalFactor resApprox = (IntervalFactor) infApprox
                .counterfactualQuery()
                .setTarget(x[3])
                .setIntervention(x[2],1)
                .setEvidence(x[2], 0)
                .run();


    }
}
