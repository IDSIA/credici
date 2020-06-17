package ch.idsia.credici.model.predefined;
// models/ch.idsia.crema.models.causal/NonMarkovian2VarRandom.java

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalAproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.map.hash.TIntIntHashMap;


public class NonMarkovian2VarRandom {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int[] endoVarSizes, int exoVarSize) {

        StructuralCausalModel model = new StructuralCausalModel();
        int x = model.addVariable(endoVarSizes[0]);
        int y = model.addVariable(endoVarSizes[1]);
        int u = model.addVariable(exoVarSize,true);

        model.addParent(x,u);
        model.addParent(y,u);
        model.addParent(y,x);

        model.fillWithRandomFactors(PROB_DECIMALS);


        return model;

    }

    public static StructuralCausalModel buildModel() {
        return buildModel(new int[]{2,2});
    }
    public static StructuralCausalModel buildModel(int[] endoVarSizes) {
        return buildModel(endoVarSizes, 5 );
    }
    public static StructuralCausalModel buildModel(int endoVarSizes, int exoVarSizes) {
        return buildModel(new int[]{endoVarSizes, endoVarSizes}, exoVarSizes );
    }

    public static void main(String[] args) throws InterruptedException {



        StructuralCausalModel model = buildModel(new int[]{2,2});
        int x = 0, y = 1;

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(x,0);


        CausalVE inf1 = new CausalVE(model);
        BayesianFactor res1 = inf1.doQuery(y, intervention);
        System.out.println(res1);

        CredalCausalVE inf2 = new CredalCausalVE(model);
        VertexFactor res2 = inf2.doQuery(y, intervention);
        System.out.println(res2);

        CredalCausalAproxLP inf3 = new CredalCausalAproxLP(model);
        IntervalFactor res3 = inf3.doQuery(y, intervention);
        System.out.println(res3);


    }

}
