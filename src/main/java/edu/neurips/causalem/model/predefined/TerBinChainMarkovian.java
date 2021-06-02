package edu.neurips.causalem.model.predefined;

import ch.idsia.credici.inference.*;
import edu.neurips.causalem.inference.CausalInference;
import edu.neurips.causalem.inference.CausalVE;
import edu.neurips.causalem.inference.CredalCausalApproxLP;
import edu.neurips.causalem.inference.CredalCausalVE;
import edu.neurips.causalem.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;


public class TerBinChainMarkovian {

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
        for (int i=0; i < n; i++) {
            int u;

            if(i%2!=0)
                u = model.addVariable(3, true);
            else
                u = model.addVariable(4, true);

            model.addParent(i,u);
        }

        model.fillWithRandomFactors(PROB_DECIMALS, false, true);



        return model;

    }


    // Example of use
    public static void main(String[] args) throws InterruptedException {
        StructuralCausalModel model = buildModel(5);

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
