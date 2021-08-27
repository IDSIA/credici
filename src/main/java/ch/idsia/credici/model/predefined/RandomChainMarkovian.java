package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.*;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import gnu.trove.map.hash.TIntIntHashMap;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Arrays;


public class RandomChainMarkovian {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int n, int endoSize, int exoSize) {

        DirectedAcyclicGraph graph = new DirectedAcyclicGraph(DefaultEdge.class);
        int[] endo = new int[n];
        int[] endo_sizes = new int[n];

        for (int i = 0; i < n; i++) {
            endo[i] = i;
            endo_sizes[i] = endoSize;
            graph.addVertex(endo[i]);
            if (i > 0) {
                graph.addEdge(endo[i - 1], endo[i]);
            }
        }

        StructuralCausalModel model = null;

        if (exoSize > 0)
            model = new StructuralCausalModel(graph, endo_sizes, exoSize);
        else
            model = new StructuralCausalModel(graph, endo_sizes);


        model.fillWithRandomFactors(PROB_DECIMALS);
        return model;

    }

    public static StructuralCausalModel buildModel(int n, int endoSize) {
        return buildModel(n, endoSize, -1);
    }

    // Example of use
    public static void main(String[] args) throws InterruptedException {
        StructuralCausalModel model = buildModel(5, 2, 5);

        int[] X = model.getEndogenousVars();

        // without evidence this is not working
        TIntIntHashMap evidence = new TIntIntHashMap();
        evidence.put(X[X.length-1], 0);

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[1];

        CausalInference inf = new CausalVE(model);
        BayesianFactor result = (BayesianFactor) inf.query(target, evidence, intervention);
        System.out.println(result);

        // with n>3, heap space error
        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(0.001);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(Arrays.toString(result3.getUpper()));
        System.out.println(Arrays.toString(result3.getLower()));

    }


}
