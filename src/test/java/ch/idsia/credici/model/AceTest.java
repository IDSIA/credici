package ch.idsia.credici.model;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.function.Tanh;
import org.junit.Test;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import cern.colt.Arrays;
import ch.idsia.credici.Table;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.inference.AceMethod;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.io.uai.UAIParser;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class AceTest {


    private void evaluate(StructuralCausalModel scm, Table data, int i, int iters, int runs) throws InterruptedException, NotImplementedException {
        EMCredalBuilder builder = EMCredalBuilder.of(scm, data.convert())
        .setInference(new AceMethod())
        .setInferenceVariation(i)
        .setNumTrajectories(runs)
        .setMaxEMIter(iters)
        .setWeightedEM(true)
        .build();

        var tr = builder.getSelectedPoints();

        System.out.println("Results " + tr.size());

        for (StructuralCausalModel m : tr) 
            for (int u : m.getExogenousVars()) 
                    System.out.println("P(U"+u+") = " + Arrays.toString(m.getFactor(u).getData()));   

        double min=2, max=0;
        int cause = 30;
        int effect = 40;
        for (var m : builder.getSelectedPoints()) {
            CausalVE inf = new CausalVE(m);
            double pns = inf.probNecessityAndSufficiency(cause, effect).getData()[0];
            if (pns > max)
                max = pns;
            if (pns < min)
                min = pns;
        }
        System.out.println(min + " " + max);
    }


    private StructuralCausalModel makeNetwork(int u1, int u2) {

        int U1 = u1; 
        int X1 = 30;
        int U2 = u2; 
        int X2 = 40;

        StructuralCausalModel model = new StructuralCausalModel("pizza");
        model.addVariable(U1, 2, true);
        model.addVariable(X1, 2, false);
        model.addParent(X1, U1);

        model.addVariable(U2, 4, true);
        model.addVariable(X2, 2, false);
        model.addParents(X2, X1, U2);

        BayesianFactor pU1 = new BayesianFactor(model.getDomain(U1));
        BayesianFactor pX1 = new BayesianFactor(model.getDomain(X1, U1), new double[]{
            1, 0, // X1=0 & U=0, X1=1 & U=0,
            1, 0  // X1=0 & U=1, X1=1 & U=1,
        });

        System.out.println("P(X1=0 | U=1) = " + pX1.getValue(0,1));
        System.out.println("P(X1=1 | U=0) = " +pX1.getValue(1,0));

        BayesianFactor pU2 = new BayesianFactor(model.getDomain(U2));
        BayesianFactor pX2 = new BayesianFactor(model.getDomain(X2, X1, U2), new double[]{
            0,1, // X2=F(X1)=X2 
            0,1,
            1,0, // X2=F(X1)=!X2 
            0,1,
            0,1, // X2=F(X1)=0 
            0,1,
            1,0, // X2=F(X1)=1 
            1,0
        });


        model.setFactor(U1, pU1);
        model.setFactor(X1, pX1);

        model.setFactor(U2, pU2);
        model.setFactor(X2, pX2);

        model.initRandom(9);
        model.fillExogenousWithRandomFactors();
        return model;
    }


    @Test
    public void testSimple() throws InterruptedException, NotImplementedException {
        
        var model = makeNetwork(1, 2);

        TIntIntMap[] data = model.samples(100, model.getEndogenousVars());
        Table table = new Table(data);

        CComponents cc = new CComponents();
    
        for (Pair<StructuralCausalModel, Table> item : cc.apply(model, table)) {

            StructuralCausalModel scm = item.getKey();
            scm.initRandom(100);
            //System.out.println("Zero Iter");
           // evaluate(scm, item.getRight(), 2, 0, 10);
           System.out.println("-------------------------------------------");
           System.out.println("------------------------------------------");
           System.out.println("-----------------------------------------");

            scm.initRandom(100);
            System.out.println("VE");
            evaluate(scm, item.getRight(), 2, 100, 2);

            System.out.println("-------------------------------------------");
            scm.initRandom(100);
            System.out.println("ACE");
            evaluate(scm, item.getRight(), 5, 100, 2);

            
        }


     
        
    }



    @Test
    public void testSimple1() throws InterruptedException, NotImplementedException {
        
        var model = makeNetwork(1, 2);

        TIntIntMap[] data = model.samples(100, model.getEndogenousVars());
        Table table = new Table(data);
        model.initRandom(100);
        evaluate(model, table, 2, 100, 2);
        evaluate(model, table, 5, 100, 2);

        
        
    }


    @Test
    public void testFile() throws FileNotFoundException, IOException, CsvException, InterruptedException, NotImplementedException {
        String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        StructuralCausalModel model = new CausalUAIParser(filename).parse();

        Table table = new Table(DataUtil.fromCSV("/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.csv"));
        CComponents cc = new CComponents();
    
        long mm = System.currentTimeMillis();
        for (Pair<StructuralCausalModel, Table> item : cc.apply(model, table)) {

            StructuralCausalModel scm = item.getKey();
            scm.initRandom(100);
          
            System.out.println("beep");
            evaluate(scm, item.getRight(), 4, 1000, 100);
        }
        mm = System.currentTimeMillis() - mm;
        System.out.println(mm);
    }
}
