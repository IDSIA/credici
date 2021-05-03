package docs;


import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.io.IOException;
import java.util.stream.IntStream;

public class EmpiricalWithDifferentTopology {

    public static void main(String[] args) throws IOException, InterruptedException {


        // Causal DAG
        SparseDirectedAcyclicGraph causalDAG = new SparseDirectedAcyclicGraph();
        IntStream.range(0,4).forEach(x -> causalDAG.addVariable(x));

        causalDAG.addLink(0,1);
        causalDAG.addLink(0,2);
        causalDAG.addLink(1,3);
        causalDAG.addLink(2,3);

        // Empirical Bayesian Network
        BayesianNetwork bnet = (BayesianNetwork) IO.read("./models/party-empirical-rev.uai") ;


        // Build causal model (with unobserved U)
        StructuralCausalModel causalModel = CausalBuilder.of(causalDAG, 2).build();

        int[] x = causalModel.getEndogenousVars();

        SparseModel hcredal = ExactCredalBuilder.of(causalModel)
                                .setEmpirical(bnet)
                                .setToHalfSpace()
                                .build().getModel();


        /////// Causal query P(X3 | do(X2 = 1))


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
