package pgm20.examples;

import ch.idsia.credici.inference.*;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.utility.RandomUtil;
import ch.idsia.credici.collections.FIntIntHashMap;

public class ChainMarkovianCase {
    public static void main(String[] args) throws InterruptedException {

        /** Number of endogenous variables in the chain (should be 3 or greater)*/
        int N = 4;

        /** Number of states in endogenous variables */
        int endoVarSize = 3;

        /** Number of states in the exogenous variables */
        int exoVarSize = 9;

        /** epsilon value for ApproxLP  */
        double eps = 0.00001;


        long seed = 1234;




        /////////////////////////////////





        RandomUtil.getRandom().setSeed(seed);
        // Load the chain model
        StructuralCausalModel model = RandomChainMarkovian.buildModel(N, endoVarSize, exoVarSize);


        // Query: P(X[N/2] | X[N-1]=0, do(X[0])=0)

        int[] X = model.getEndogenousVars();

        FIntIntHashMap evidence = new FIntIntHashMap();

        int obsvar = X[0];
        //obsvar = -1;
        int dovar = X[N-1];

       if(obsvar!=-1) evidence.put(obsvar, 1);

        FIntIntHashMap intervention = new FIntIntHashMap();
        if(dovar!=-1) intervention.put(X[0], 0);


        int target = X[N/2];
        target = X[1];



        System.out.println("\nChainMarkovian\n   N=" + N + " endovarsize=" + endoVarSize + " exovarsize=" + exoVarSize + " target=" + target + " obsvar=" + obsvar + " dovar=" + dovar + " seed=" + seed);
        System.out.println("=================================================================");

        model.printSummary();

        // Run inference

        CausalInference inf1 = new CausalVE(model);
        BayesianFactor result1 = (BayesianFactor) inf1.query(target, evidence, intervention);
        System.out.println(result1);

        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);


        CausalInference inf3 = new CredalCausalApproxLP(model).setEpsilon(eps);
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);


    }
}
