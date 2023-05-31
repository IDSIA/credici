package pgm20.examples;

import ch.idsia.credici.inference.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.collections.FIntIntHashMap;

public class ChainNonMarkovianCase {
    public static void main(String[] args) throws InterruptedException {

        ////////// Parameters //////////

        //RandomUtil.getRandom().setSeed(123442234);

        /** Number of endogenous variables in the chain (should be 3 or greater)*/
        int N = 4;

        /** Number of states in endogenous variables */
        int endoVarSize = 2;

        /** Number of states in the exogenous variables */
        int exoVarSize = 9;

        /** epsilon value for ApproxLP  */
        double eps = 0.00001;

        /////////////////////////////////
        //RandomUtil.getRandom().setSeed(123354);

        // Load the chain model
        StructuralCausalModel model = RandomChainNonMarkovian.buildModel(N, endoVarSize, exoVarSize);

        // Query: P(X[N/2] | X[N-1]=0, do(X[0])=0)

        int[] X = model.getEndogenousVars();

        FIntIntHashMap evidence = new FIntIntHashMap();
        evidence.put(X[N-1], 0);

        FIntIntHashMap intervention = new FIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[2];




        // Run inference

        CausalInference inf1 = new CausalVE(model);
        BayesianFactor result1 = (BayesianFactor) inf1.query(target, evidence, intervention);
        System.out.println(result1);

        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);

        //model.printSummary();

        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(eps);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);


    }
}
