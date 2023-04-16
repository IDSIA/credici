package ch.idsia.credici;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.primitives.Ints;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.learning.inference.AceLocalMethod;
import ch.idsia.credici.learning.inference.DirectOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.io.dot.DotSerialize;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class Example1 {



        
    public static void main(String[] args) throws IOException, InterruptedException, NotImplementedException {

        StructuralCausalModel model = new CausalUAIParser("/Users/dhuber/Development/credici-dev/papers/clear23/models/synthetic/s1/random_mc2_n13_mid3_d1000_05_mr098_r10_53.uai").parse();
        Logger gl = Logger.getAnonymousLogger();
        Handler[] h =  gl.getHandlers();

        gl.info("Sampling");
        Filter f = gl.getFilter();

        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        boolean comp = model.isCompatible(samples);
        if (!comp) {
            System.out.println("Incompat");
            return; // skip
        } else {
            System.out.println("compatible!!");
        }
        
        gl.setLevel(Level.ALL);
        gl.setFilter(f);
        Arrays.stream(h).forEach(gl::addHandler);

        // for VM startup
        gl.info("Warming UP");
        EMCredalBuilder builder = EMCredalBuilder.of(model.copy(), samples)
            .setMaxEMIter(100)
            .setNumTrajectories(1)
            .setThreshold(1)
            .setStopCriteria(StopCriteria.LLratio)
            .setInferenceVariation(4)
            .build();

       gl.info("Done warming up");

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


        int[] ivs = new int[] { 0,1,2,3,4 };
        for (int iv :ivs) {
            gl.info("Inference using " + iv);
        
            long start = System.currentTimeMillis();
            builder = EMCredalBuilder.of(model.copy(), samples)
                .setMaxEMIter(10)
                .setNumTrajectories(1)
                .setThreshold(1)
                .setStopCriteria(StopCriteria.LLratio)
                .setInferenceVariation(iv)
                .build();

                System.out.println(iv + ": " +(System.currentTimeMillis() - start));
                if (iv >= 5) { 
                    System.out.println("ACE TIME: " + builder.getAceQueryTime() + " -> setup:" +   builder.getAceSetupTime());
                }
            for (var m : builder.getSelectedPoints()){
                CausalVE inf = new CausalVE(m);
                double pns = inf.probNecessityAndSufficiency(0, 2).getData()[0];
                System.out.println(pns);
            }
        }
    }
}
//0.276778846706142