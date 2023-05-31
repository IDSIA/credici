package ch.idsia.credici;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import com.opencsv.exceptions.CsvException;

import cern.colt.Arrays;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.DataUtil;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class Example1 {

    public static class DotSerialize {
        public String run(StructuralCausalModel gm) {
            StringBuilder builder = new StringBuilder();
            
            builder.append("digraph model {\n");
            for (int i : gm.getVariables()) {
                
                builder.append("   node" + i + " [label=\"" + i + " (" + gm.getSize(i) +")\"");
                
                if (gm.isExogenous(i)) builder.append(" shape=box");
                if (gm.isSpurious(i)) builder.append(" color=gray fontcolor=gray");
                

                builder.append("];\n");
            }
            
            for (int i : gm.getVariables()) {
                for (int child : gm.getChildren(i)) {
                    builder.append("   node").append(i).append(" -> node").append(child);
                    if (gm.isSpurious(i))
                        builder.append("[color=gray]");
                    builder.append(";\n");
                }
            }
            builder.append("}");
            return builder.toString();
        }
    }

    // 5: 5450
    // 3: 5800

    private static StructuralCausalModel generate() {
        StructuralCausalModel model = new StructuralCausalModel("test");
        int U = model.addVariable(2, 4, true, false);
        int x1 = model.addVariable(0,2,false);
        int x2 = model.addVariable(5,2,false);
        model.addParent(x1, U);
        model.addParents(x2 ,U);
        var xx = EquationBuilder.of(model).conservative(U);
        model.setFactor(x1, xx.get(x1));
        model.setFactor(x2, xx.get(x2));
        model.fillExogenousWithRandomFactors();
        return model;
    }

    
    public static void xx(String[] args) throws IOException, InterruptedException, NotImplementedException, CsvException {
        FIntIntHashMap map1 = new FIntIntHashMap();
        FIntIntHashMap map2 = new FIntIntHashMap();
        map1.put(0,1);
        map2.put(0, 0);
        System.out.println(map1.equals(map2));
    }

    public static void main(String[] args) throws Exception {    
        //String filename = "/Users/dhuber/Development/credici-dev/papers/21why/examples/consPearl.uai";
        String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai";
        String dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.csv";

        // filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.uai";
        // dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.csv";
        // filename="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        // dataname="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.csv";
        
        // filename = "/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.uai";
        // dataname  ="/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.csv";
        //String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        StructuralCausalModel// model = generate();
        model = new CausalUAIParser(filename).parse();
            
        //  StructuralCausalModel model = new
        //  CausalUAIParser("/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai").parse();
        Logger gl = Logger.getAnonymousLogger();
        Handler[] h = gl.getHandlers();

        gl.info("Sampling");

        TIntIntMap[] samples = model.samples(100, model.getEndogenousVars());
        Table table = new Table(samples);

        System.out.println(table.toString());
        table = new Table(DataUtil.fromCSV(dataname));

        model.initRandom(0);

        int cause = 5;//8;
        int effect = 0;//5;
        int runs = 2;

        StructuralCausalModel[] random = IntStream.range(0,runs).mapToObj(a->{
            var m = model.copy();
            m.fillExogenousWithRandomFactors();
            return m;
        }).toArray(StructuralCausalModel[]::new);
        int[] exo = model.getExogenousVars();
        for (int e : exo) {
            System.out.println(e+": -> " +Arrays.toString(model.getFactor(e).getData()));
        }
        System.out.println("----------- FULL --");
        if (true) {
            long start = System.currentTimeMillis();
    
            EMCredalBuilder builder = EMCredalBuilder.of(model, samples)
                    .setMaxEMIter(100)
                    .setNumTrajectories(runs)
                    .setThreshold(0)
                    .setStopCriteria(StopCriteria.MAX_ITER)
                    .setInferenceVariation(0)
                    .setWeightedEM(true)
                    .setRandomModels(model, random)
                    .build();
                    
            System.out.println((System.currentTimeMillis() - start));
            double min = 1;
            double max = 0;
            for (var m : builder.getSelectedPoints()) {
                for (int e : exo) {
                    System.out.println(e + ": -> " +Arrays.toString(m.getFactor(e).getData()));
                }
                CausalVE inf = new CausalVE(m);
                double pns = inf.probNecessityAndSufficiency(cause, effect).getData()[0];
                if (pns > max)
                    max = pns;
                if (pns < min)
                    min = pns;
            }
            System.out.println(min + " " + max);
        }

        System.out.println("-------------------------------- CC ----------");

        if (true) {
            CComponents cc = new CComponents();

            long start = System.currentTimeMillis();
            ExecutorService executor = Executors.newFixedThreadPool(10);

            for (var x : cc.apply(model, table)) {

                var runnable = new Runnable() {
                    public void run() {
                        var cmodel = x.getLeft();
                        var csamples = x.getRight();

                        try {
                            EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                                    .setMaxEMIter(5)
                                    .setNumTrajectories(runs)
                                    .setThreshold(0)
                                    .setStopCriteria(StopCriteria.MAX_ITER)
                                    .setInferenceVariation(0)
                                    //.setInference(new AceMethod())
                                    .setRandomModels(cmodel, random)
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

            //StructuralCausalModel k = iter.next();
            //System.out.println(serialize.run(k));

            for (int i = 0; iter.hasNext() && i < 10000; i++) {
                if (!iter.hasNext()) System.out.println("STOOOOP");
                StructuralCausalModel m = iter.next();
                for (int e : exo) {
                    System.out.println(e+": -> " +Arrays.toString(m.getFactor(e).getData()));
                }

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