package ch.idsia.credici.model;

import java.io.IOException;
import java.util.Arrays;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import org.junit.Test;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;

public class UnfeasibleBug {
    @Test
    public void test() throws IOException, InterruptedException {
        // Load the empirical model
        String fileName = "./models/simple-bayes.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        //Get the Credal Netoworks
        SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal =  causalModel.toHCredal(bnet.getFactors());


        // Set query
        TIntIntMap intervention = new FIntIntHashMap();
        intervention.put(0,1);
        int target = 1;


        //Exact inference
        CredalCausalVE inf2 = new CredalCausalVE(vcredal);
        VertexFactor res2 = inf2.doQuery(target, intervention);
        System.out.println(res2);

        // Approx inference
        CredalCausalApproxLP inf = new CredalCausalApproxLP(hcredal);
        IntervalFactor res = inf.doQuery(target, intervention);
        System.out.println(res);
    }

    @Test
    public void nantest() throws InterruptedException {

        BayesianNetwork bn = new BayesianNetwork();

        int A=bn.addVariable(2);
        int B=bn.addVariable(2);
        int C=bn.addVariable(2);
        
        bn.addParent(B, A);
        bn.addParent(C, B);
        

        Strides dA = bn.getDomain(A);
        BayesianFactor pA = new BayesianFactor(dA, new double[]{1,0});
        bn.setFactor(A, pA);

        Strides dB_A = Strides.var(0, 2).and(1, 2);
        BayesianFactor pB_A = new BayesianFactor(dB_A, new double[]{1,0,0,1});
        bn.setFactor(B, pB_A);

        Strides dC_B = Strides.var(1, 2).and(2, 2);
        BayesianFactor pC_B = new BayesianFactor(dC_B, new double[]{0,1,1,0});
        bn.setFactor(C, pC_B);
        
        FactorVariableElimination fve = new FactorVariableElimination<>(new int[]{C});
        BayesianFactor gf = (BayesianFactor) fve.apply(bn, C);
        
        Strides dom = bn.getDomain(A,B,C);
        double[] data = new double[dom.getCombinations()];

        int i =0; 
        var iter = gf.getDomain().getIterator(dom);
        while (iter.hasNext()){
            data[i++] = gf.getValueAt(iter.next());
        }

        System.out.println(Arrays.toString(data));
    }
}
