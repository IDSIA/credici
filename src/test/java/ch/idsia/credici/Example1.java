package ch.idsia.credici;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.opencsv.exceptions.CsvException;


import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.learning.inference.AceMethod;
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

    private static NumberFormat format;
    private static NumberFormat getFormat() {
        if (format == null) {
            format = new DecimalFormat("0.000");
        }
        return format;
    };
    
    private static String toString(double[] data){
        NumberFormat format = getFormat();
        return DoubleStream.of(data).<String>mapToObj(format::format).collect(Collectors.joining(", "));
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

    public static void main1(String[] args) throws Exception {    
        //String filename = "/Users/dhuber/Development/credici-dev/papers/21why/examples/consPearl.uai";
        String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai";
        String dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.csv";
        System.setProperty("java.util.logging.SimpleFormatter.format", 
        "%4$s %2$s %5$s%6$s%n");
        filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.uai";
        dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.csv";
        // filename="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        // dataname="/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.csv";
        
        // filename = "/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.uai";
        // dataname  ="/Users/dhuber/Development/credici-dev/papers/JoCI/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_27.csv";
        //String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n10_mid3_d1000_05_mr098_r10_4.uai";
        //filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai";
        StructuralCausalModel// model = generate();
        model = new CausalUAIParser(filename).parse();



        try { 
            ExecutorService executor = Executors.newFixedThreadPool(10);  
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch(Exception ex) { 

        }
        
    }


    public static List<StructuralCausalModel> inferenceCC(int method, int maxIter, int runs, Table table, StructuralCausalModel model, StructuralCausalModel[] random) {
        CComponents cc = new CComponents();
        for (var x : cc.apply(model, table)) {
            var cmodel = x.getLeft();
            var csamples = x.getRight();

            try {
                EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                        .setMaxEMIter(maxIter)
                        .setNumTrajectories(runs)
                        .setThreshold(0)
                        .setStopCriteria(StopCriteria.MAX_ITER)
                        .setInferenceVariation(method)
                        .setInference(new AceMethod())
                        .setRandomModels(cmodel, random)
                        .build();

                cc.addResults(cmodel.getName(), builder.getSelectedPoints());
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return IteratorUtils.toList(cc.iterator(), runs);
    }

    public static List<StructuralCausalModel> inferenceFull(int method, int maxIter, int runs, Table table, StructuralCausalModel model, StructuralCausalModel[] random) {
            var cmodel = model;
            var csamples = table;

            try {
                EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                        .setMaxEMIter(maxIter)
                        .setNumTrajectories(runs)
                        .setThreshold(0)
                        .setStopCriteria(StopCriteria.MAX_ITER)
                        .setInferenceVariation(method)
                        .setInference(new AceMethod())
                        .setRandomModels(cmodel, random)
                        .build();

                return builder.getSelectedPoints();
                
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
    }


    public static void main(String[] args) throws FileNotFoundException, IOException, CsvException {
        System.setProperty("java.util.logging.SimpleFormatter.format", 
        "%4$s %2$s %5$s%6$s%n");

        String filename = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.uai";
        String dataname = "/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n5_mid3_d1000_05_mr098_r10_0.csv";
        
        StructuralCausalModel// model = generate();
        model = new CausalUAIParser(filename).parse();

        Table table = new Table(DataUtil.fromCSV(dataname));
        model.initRandom(0);

        int runs = 100;

        StructuralCausalModel[] random = IntStream.range(0,runs).mapToObj(a->{
            var m = model.copy();
            m.fillExogenousWithRandomFactors();
            return m;
        }).toArray(StructuralCausalModel[]::new);

        
        int[] exo = model.getExogenousVars();
        for (int e : exo) {
            System.out.println(e + ": -> " + toString(model.getFactor(e).getData()));
        }
        for (boolean full : new boolean[] { false, true }) {
            String name = full ? "FULL" : "CC";
            
            for (int method : new int[] { 0, 5}) {
                System.out.println("------------------- "+name+" " + method + " ----------");
                for (int maxIter : new int[] {10, 20, 50, 100, 150, 200, 300, 400, 500, 800, 1000}) {
                    
                    long start = System.currentTimeMillis();
                    List<StructuralCausalModel> models;
                    if (full)
                        models = inferenceFull(method, maxIter, runs, table, model, random);
                     else 
                        models = inferenceCC(method, maxIter, runs, table, model, random);
                        
                    start = (System.currentTimeMillis() - start);
                
                    double d = 0;
                    for (StructuralCausalModel m : models) {
                        d += diff(model, m);    
                    }
                    
                    System.out.println(maxIter + " Time: " + start + " Diff: " + d);
                }
            }
        }

    }

    static interface BiDoubleFunc {
        public double exec(double a1, double a2);
    }

    private static double elem(double[] d1, double[] d2, BiDoubleFunc f) {
        double d =0;
        for (int i = 0; i < d1.length; i++){
            d += f.exec(d1[i], d2[i]);
        }
        return d;
    }
    private static double diff(StructuralCausalModel m1, StructuralCausalModel m2) {
        double d = 0;
        for (int exo : m1.getExogenousVars()) {
            d += elem(m1.getFactor(exo).getData(), m2.getFactor(exo).getData(), (a,b)->Math.abs(a-b));
        }
        return d;
    }
}
// 0.276778846706142