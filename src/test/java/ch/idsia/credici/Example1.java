package ch.idsia.credici;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.opencsv.exceptions.CsvException;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.learning.inference.AceMethod;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.model.io.dot.DotSerialize;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class Example1 {

    // 5: 5450
    // 3: 5800

    public static void main(String[] args) throws IOException, InterruptedException, NotImplementedException, CsvException {

        //String filename = "/Users/dhuber/Development/credici-dev/papers/21why/examples/consPearl.uai";
        String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai";
        String dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.csv";

        filename="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        dataname="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.csv";
        
        filename = "/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.uai";
        dataname  ="/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.csv";
        //String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        StructuralCausalModel model = new CausalUAIParser(filename).parse();
            
        //  StructuralCausalModel model = new
        //  CausalUAIParser("/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai").parse();
        Logger gl = Logger.getAnonymousLogger();
        Handler[] h = gl.getHandlers();

        gl.info("Sampling");

        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        Table table = new Table(samples);

        table = new Table(DataUtil.fromCSV(dataname));

        DotSerialize serialize = new DotSerialize();
        System.out.println(serialize.run(model));
        model.initRandom(0);

        int cause = 5;//8;
        int effect = 0;//5;
        int runs = 100;


        if (false) {
            long start = System.currentTimeMillis();
    
            EMCredalBuilder builder = EMCredalBuilder.of(model, samples)
                    .setMaxEMIter(100)
                    .setNumTrajectories(runs)
                    .setThreshold(1)
                    .setStopCriteria(StopCriteria.LLratio)
                    .setInferenceVariation(1)
                    .setInference(new AceMethod())
                    .setWeightedEM(true)
                    .build();
                    
            System.out.println((System.currentTimeMillis() - start));
            double min = 1;
            double max = 0;
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





        if (true) {
            CComponents cc = new CComponents();
            cc.initRandom(0);    

            long start = System.currentTimeMillis();
            ExecutorService executor = Executors.newFixedThreadPool(10);

            for (var x : cc.apply(model, table)) {
                
                //x.getLeft().getEmpiricalMap();
              //  EmpiricalNetwork em = new EmpiricalNetwork();
               // em.apply(model, table);
               // System.out.println("LL = " + em.getLl());
                
              //  double ll = Probability.maxLogLikelihood(x.getLeft(), x.getRight().convert());
                //System.out.println("LL " + ll);

                var runnable = new Runnable() {
                    public void run() {
                        var cmodel = x.getLeft();
                        var csamples = x.getRight();

                        try {
                            EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                                    .setMaxEMIter(300)
                                    .setNumTrajectories(runs)
                                    .setThreshold(1)
                                    .setStopCriteria(StopCriteria.LLratio)
                                    .setInferenceVariation(3)
                                    .setInference(new AceMethod())
                                    .build();

                            System.out.println(cmodel.getName());
                            cc.addResults(cmodel.getName(), builder.getSelectedPoints());
                            
                        } catch (Exception ex) {
                             ex.printStackTrace();
                        }
                    }
                };
                //executor.execute(runnable);
                runnable.run();
            }

            try { 
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch(Exception ex) { 

            }

            System.out.println((System.currentTimeMillis() - start));
            
            double min = 1;
            double max = 0;
            var iter = cc.iterator();

            StructuralCausalModel k = iter.next();
            System.out.println(serialize.run(k));

            for (int i = 0; iter.hasNext() && i < 10000; i++) {
                if (!iter.hasNext()) System.out.println("STOOOOP");
                StructuralCausalModel m = iter.next();
                CausalVE inf = new CausalVE(m);
                double pns = inf.probNecessityAndSufficiency(cause, effect).getData()[0];
                if (pns > max)
                max = pns;
                if (pns < min)
                min = pns;
            }
            
            System.out.println(min + " " + max);
        }

              
    }

}
// 0.276778846706142