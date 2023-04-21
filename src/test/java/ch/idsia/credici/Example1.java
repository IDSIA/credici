package ch.idsia.credici;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.learning.inference.AceMethod;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.crema.model.io.dot.DotSerialize;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class Example1 {

    public static void main(String[] args) throws IOException, InterruptedException, NotImplementedException {

        StructuralCausalModel model = new CausalUAIParser("/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai").parse();
        Logger gl = Logger.getAnonymousLogger();
        Handler[] h =  gl.getHandlers();

        gl.info("Sampling");


        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        Table table = new Table(samples);

        //boolean comp = model.isCompatible(samples);
        

        // for VM startup
    //     gl.info("Warming UP");
    //     EMCredalBuilder.of(model.copy(), samples)
    //         .setMaxEMIter(100)
    //         .setNumTrajectories(1)
    //         .setThreshold(1)
    //         .setStopCriteria(StopCriteria.LLratio)
    //         .setInferenceVariation(5)
    //         .setInference(new AceMethod())
    //         .build();

    //    gl.info("Done warming up");

        DotSerialize serialize = new DotSerialize();
        System.out.println(serialize.run(model));
    

        //Table table = new Table(samples);
        System.out.println("Waiting");
        try(Scanner input = new Scanner(System.in)) {
            String cont = input.nextLine();
            while(!cont.equals(" ")) {
                cont = input.nextLine();
            }
        }


   
            
            long start = System.currentTimeMillis();
            
            CComponents cc = new CComponents();
            int runs = 30;
            
            Map<Integer, List<StructuralCausalModel>> trajectories = new HashMap<>();
            for (int r = 0; r < runs; ++r) 
                trajectories.put(r, new ArrayList<StructuralCausalModel>());

            Executor executor = Executors.newFixedThreadPool(10);
            
            for (var x : cc.apply(model, table)) {
                executor.execute(()->{
                    var cmodel = x.getLeft();
                    var csamples = x.getRight();
                    try {
                        EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                            .setMaxEMIter(500)
                            .setNumTrajectories(runs)
                            .setThreshold(0)
                            .setStopCriteria(StopCriteria.KL)
                            .setInferenceVariation(5)
                            .setInference(new AceMethod())
                            .build();

                        List<StructuralCausalModel> t = builder.getSelectedPoints();
                        for(int i = 0; i < runs; ++i) {
                            trajectories.get(i).add(t.get(i));
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
            
            List<StructuralCausalModel> full = trajectories.values().stream().map(cc::revert).toList();

                System.out.println((System.currentTimeMillis() - start));
                for (var m : full){
                    CausalVE inf = new CausalVE(m);
                    double pns = inf.probNecessityAndSufficiency(0, 2).getData()[0];
                    System.out.println(pns);
                }
        }
    
}
//0.276778846706142