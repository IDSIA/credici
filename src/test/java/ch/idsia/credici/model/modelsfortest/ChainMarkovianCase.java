package ch.idsia.credici.model.modelsfortest;

import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

public class ChainMarkovianCase {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(Arrays.toString(test(args)));
    }

    public static double[] test(String[] args) throws InterruptedException{


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

        TIntIntHashMap evidence = new TIntIntHashMap();

        int obsvar = X[0];
        //obsvar = -1;
        int dovar = X[N-1];

       if(obsvar!=-1) evidence.put(obsvar, 1);

        TIntIntHashMap intervention = new TIntIntHashMap();
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

        return Doubles.concat(
                result1.getData(),
                ArraysUtil.flattenDoubles(Arrays.asList(result2.getData()[0])),
                result3.getLower(0),
                result3.getUpper(0)
                );
    }
}
