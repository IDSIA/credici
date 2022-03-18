package docs;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import java.io.IOException;
import java.util.stream.IntStream;
//16
public class EmpiricalWithDifferentTopology {

    public static void main(String[] args) throws IOException, InterruptedException {


        // Causal DAG
        DirectedAcyclicGraph causalDAG = new DirectedAcyclicGraph(DefaultEdge.class);
        IntStream.range(0,4).forEach(x -> causalDAG.addVertex(x));

        causalDAG.addEdge(0,1);
        causalDAG.addEdge(0,2);
        causalDAG.addEdge(1,3);
        causalDAG.addEdge(2,3);

        // Empirical Bayesian Network
        BayesianNetwork bnet = (BayesianNetwork) IO.read("./models/party-empirical-rev.uai") ;


        // Build causal model (with unobserved U)
        StructuralCausalModel causalModel = CausalBuilder.of(causalDAG, 2).build();

        int[] x = causalModel.getEndogenousVars();

        DAGModel<SeparateHalfspaceFactor> hcredal = ExactCredalBuilder.of(causalModel)
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
//82