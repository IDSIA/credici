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


public class TerBinChainNonMarkovian {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(int n) {

        StructuralCausalModel model = new StructuralCausalModel();



        // add endogenous
        for (int i=0; i < n; i++) {
            if(i%2!=0)
                model.addVariable(2);
            else
                model.addVariable(3);
            if(i>0)
                model.addParent(i, i-1);
        }

        //add exogenous
        for (int i=0; i < n; i+=2) {
            int u = model.addVariable(6, true);
            model.addParent(i,u);
            if(i+1<n) model.addParent(i+1, u);
        }

        model.fillWithRandomFactors(PROB_DECIMALS, false, true);



        return model;

    }



    public static void main(String[] args) throws InterruptedException {
        int n = 5;
        StructuralCausalModel model = buildModel(n);
        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();
        evidence.put(X[n-1], 0);

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[3];

        CausalInference inf = new CausalVE(model);
        BayesianFactor result = (BayesianFactor) inf.query(target, evidence, intervention);
        System.out.println(result);



        // error, this is not working
        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(0.00001);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);




    }

}
