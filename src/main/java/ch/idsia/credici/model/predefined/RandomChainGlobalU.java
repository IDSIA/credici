package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.map.hash.TIntIntHashMap;


public class RandomChainGlobalU {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int n, int endoSize, int exoSize) {

        StructuralCausalModel model = new StructuralCausalModel();

        // add endogenous
        for (int i=0; i < n; i++) {
            model.addVariable(endoSize);
            if(i>0)
                model.addParent(i, i-1);
        }

        int u = model.addVariable(exoSize, true);

        //add exogenous
        for (int i=0; i < n; i++) {
            model.addParent(i,u);
        }

        model.fillWithRandomFactors(PROB_DECIMALS, false, true);



        return model;

    }

    public static StructuralCausalModel buildModel(int n, int endoSize) {
        return buildModel(n, endoSize, -1);
    }


    public static void main(String[] args) throws InterruptedException {
        int n = 4;
        int endoSize = 2;
        int exoSize = (int) (Math.pow(endoSize,n)+1);
        StructuralCausalModel model = buildModel(n, endoSize,exoSize);

        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[n-1];

        CausalInference inf = new CausalVE(model);
        BayesianFactor result = (BayesianFactor) inf.query(target, evidence, intervention);
        System.out.println(result);



        // error, this is not working
        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(0.001);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);




    }

}
