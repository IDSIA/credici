package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.*;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.credici.collections.FIntIntHashMap;

import java.util.Arrays;


public class RandomChainMarkovian {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int n, int endoSize, int exoSize) {

        SparseDirectedAcyclicGraph graph = new SparseDirectedAcyclicGraph();
        int[] endo = new int[n];
        int[] endo_sizes = new int[n];

        for (int i = 0; i < n; i++) {
            endo[i] = i;
            endo_sizes[i] = endoSize;
            graph.addVariable(endo[i]);
            if (i > 0) {
                graph.addLink(endo[i - 1], endo[i]);
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
        FIntIntHashMap evidence = new FIntIntHashMap();
        evidence.put(X[X.length-1], 0);

        FIntIntHashMap intervention = new FIntIntHashMap();
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
