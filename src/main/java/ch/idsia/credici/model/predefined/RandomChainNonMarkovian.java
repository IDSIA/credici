package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.*;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.map.hash.TIntIntHashMap;


public class RandomChainNonMarkovian {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int n, int endoSize, int exoSize) {

        StructuralCausalModel model = new StructuralCausalModel();

        // add endogenous
        for (int i=0; i < n; i++) {
            model.addVariable(endoSize);
            if(i>0)
                model.addParent(i, i-1);
        }

        //add exogenous
        for (int i=0; i < n; i+=2) {
            int u = model.addVariable(exoSize, true);
            model.addParent(i,u);
            if(i+1<n) model.addParent(i+1, u);
        }

        model.fillWithRandomFactors(PROB_DECIMALS, false, true);



        return model;

    }

    public static StructuralCausalModel buildModel(int n, int endoSize) {
        return buildModel(n, endoSize, -1);
    }


    public static void main(String[] args) throws InterruptedException {
        int n = 5;
        StructuralCausalModel model = buildModel(n, 2, 6);

        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();
        evidence.put(X[n-1], 0);

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[1];

        CausalInference inf = new CausalVE(model);
        BayesianFactor result = (BayesianFactor) inf.query(target, evidence, intervention);
        System.out.println(result);


        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(0.001);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);




    }

}
