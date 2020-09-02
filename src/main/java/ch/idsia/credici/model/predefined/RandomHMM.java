package ch.idsia.credici.model.predefined;

import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.hash.TIntIntHashMap;


public class RandomHMM {

    public static int PROB_DECIMALS = 2;

    public static StructuralCausalModel buildModel(boolean markovian, int n, int endoSize, int exoSize) {

        StructuralCausalModel model = new StructuralCausalModel();

        int[] X = new int[n];
        int[] Y = new int[n];

        // add endogenous
        for (int i=0; i < n; i++) {
            X[i] = model.addVariable(endoSize);
            Y[i] = model.addVariable(endoSize);

            model.addParent(Y[i], X[i]);

            if(i>0)
                model.addParent(X[i], X[i-1]);
        }

        //add exogenous

        int step = 1;
        if (!markovian) step = 2;
        for (int i = 0; i < n; i += step) {
            int u = model.addVariable(exoSize, true);
            model.addParent(X[i], u);
            if (!markovian && i + 1 < n) model.addParent(X[i + 1], u);
        }

        for(int i = 0; i<n; i++){
            int u = model.addVariable(exoSize, true);
            model.addParent(Y[i], u);
        }

        model.fillWithRandomFactors(PROB_DECIMALS, false, true);



        return model;

    }




    public static void main(String[] args) throws InterruptedException {
        int n = 3;

        RandomUtil.getRandom().setSeed(3702);
        //RandomUtil.getRandom().setSeed(1234);

        StructuralCausalModel model = buildModel(false, n, 2, 6);


        System.out.println(model.getNetwork());
        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();
        //evidence.put(X[2*n-1], 0);
        evidence.put(4, 1);


        TIntIntHashMap intervention = new TIntIntHashMap();
       // intervention.put(X[0], 0);
        intervention.put(0, 0);


        int target = X[2];
        target = 2;

   //     System.out.println("p("+target+"|"+evidence.keys()[0]+",do("+intervention.keys()[0]+"))");



        CausalInference inf = new CausalVE(model);
        BayesianFactor result = (BayesianFactor) inf.query(target, evidence, intervention);
        System.out.println(result);


        // error, this is not working
        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(0.000001);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);




    }

}
