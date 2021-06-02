package edu.neurips.causalem.model.modelsfortest;

import edu.neurips.causalem.inference.CausalInference;
import edu.neurips.causalem.inference.CausalVE;
import edu.neurips.causalem.inference.CredalCausalApproxLP;
import edu.neurips.causalem.inference.CredalCausalVE;
import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.util.Arrays;

public class ChainNonMarkovianCase {

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println(Arrays.toString(test(args)));
    }

    public static double[] test(String[] args) throws InterruptedException, IOException {


        ////////// Parameters //////////

        RandomUtil.getRandom().setSeed(1234);

        /** Number of endogenous variables in the chain (should be 3 or greater)*/
        int N = 3;

        /** Number of states in endogenous variables */
        int endoVarSize = 2;

        /** Number of states in the exogenous variables */
        int exoVarSize = 9;

        /** epsilon value for ApproxLP  */
        double eps = 0.000000;

        /////////////////////////////////
        //RandomUtil.getRandom().setSeed(123354);

        // Load the chain model
        StructuralCausalModel model = RandomChainNonMarkovian.buildModel(N, endoVarSize, exoVarSize);

        // Query: P(X[N/2] | X[N-1]=0, do(X[0])=0)

        int[] X = model.getEndogenousVars();

        TIntIntHashMap evidence = new TIntIntHashMap();
        evidence.put(X[N-1], 0);

        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(X[0], 0);

        int target = X[1];

        // Run inference

        CausalInference inf1 = new CausalVE(model);
        BayesianFactor result1 = (BayesianFactor) inf1.query(target, evidence, intervention);
        System.out.println(result1);

        CausalInference inf2 = new CredalCausalVE(model);
        VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
        System.out.println(result2);

        //model.printSummary();

        CausalInference inf3 = new CredalCausalApproxLP(model);

        IO.write(inf3.getModel(), "./models/chain3-nonmarkov.uai");
        IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
        System.out.println(result3);

        return Doubles.concat(
                result1.getData(),
                ArraysUtil.flattenDoubles(Arrays.asList(result2.getData()[0])),
                result3.getLower(0),
                result3.getUpper(0)
        );    }
}
