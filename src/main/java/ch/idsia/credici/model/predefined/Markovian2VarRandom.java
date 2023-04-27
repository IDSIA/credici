package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import gnu.trove.map.hash.TIntIntHashMap;


public class Markovian2VarRandom {

    public static int PROB_DECIMALS = 1;

    public static StructuralCausalModel buildModel(int[] endoVarSizes, int[] exoVarSizes) {

        int x1=0, x2=1;

        SparseDirectedAcyclicGraph dag = new SparseDirectedAcyclicGraph();
        dag.addVariable(x1);
        dag.addVariable(x2);

        dag.addLink(x1, x2);

        StructuralCausalModel model = new StructuralCausalModel(dag, endoVarSizes, exoVarSizes);

        model.fillWithRandomFactors(PROB_DECIMALS);
        return model;

    }

    public static StructuralCausalModel buildModel() {
        return buildModel(new int[]{2,2}, new int[]{3,5} );
    }


    public static void main(String[] args) throws InterruptedException {

        StructuralCausalModel model = buildModel();
        int x = 0, y = 1;

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(x,0);

        CausalVE inf1 = new CausalVE(model);
        BayesianFactor res1 = inf1.doQuery(y, intervention);
        System.out.println(res1);

        CredalCausalVE inf2 = new CredalCausalVE(model);
        VertexFactor res2 = inf2.doQuery(y, intervention);
        System.out.println(res2);

        CredalCausalApproxLP inf3 = new CredalCausalApproxLP(model);
        IntervalFactor res3 = inf3.doQuery(y, intervention);
        System.out.println(res3);


    }
}
