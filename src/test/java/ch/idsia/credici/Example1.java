package ch.idsia.credici;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.rng.sampling.DiscreteProbabilityCollectionSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaTsangWangDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;

import com.google.common.primitives.Ints;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
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



            //double su = DoubleStream.of(p).sum();
//        new double[]{0.9, 0.05, 0.05};
        // for (int j = 0; j < 1000; j++) {
        //     int aa = RandomUtil.sampleCategorical(p);
        //     System.out.print(aa + ", ");
        // }
        // System.out.println();


                // Files.writeString(file.toPath(), "CAUSAL\n"+
        // "4\n"+
        // "2 2 2 3\n"+
        // "4\n"+
        // "2	3 0 \n"+
        // "3	3 0 1 \n"+
        // "3	3 1 2 \n"+
        // "1	3 \n"+
        // "3	0 0 0   \n"+
        // "6	0 0 0 0  0 0 \n"+
        // "6	0 0 0 0  0 0 \n"+
        // "3	0 0 0 ", Charset.defaultCharset());
        // var d = RandomSource.MWC_256.create(12);
        
        // double[] p = new double[] {0.6431745647263734, 0.1987992401218274, 0.15802619515179916};
        // DiscreteSampler s = MarsagliaTsangWangDiscreteSampler.Enumerated.of(d, p);
        // int[] a = s.samples(1000).toArray();
        // System.out.println(Arrays.toString(a));

        // var ds = new DiscreteProbabilityCollectionSampler<Integer>(d, Ints.asList(new int[]{0,1,2}), p);
        // a = ds.samples(1000).mapToInt(Integer::intValue).toArray();
        // System.out.println(Arrays.toString(a));


        
    public static void main(String[] args) throws IOException, InterruptedException, NotImplementedException {
        File file = File.createTempFile("Credici", ".uai");
        Files.writeString(file.toPath(), "CAUSAL\n"+
        "4\n"+
        "2 2 2 32\n"+
        "4\n"+
        "2	3 0 \n"+
        "3	3 0 1 \n"+
        "3	3 1 2 \n"+
        "1	3 \n"+
        "32	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  1 1 1 1  1 1 1 1  1 1 1 1  1 1 1 1 \n"+
        "64	0 0 0 0  0 0 0 0  1 1 1 1  1 1 1 1  0 0 0 0  0 0 0 0  1 1 1 1  1 1 1 1 "+
           "0 0 0 0  0 0 0 0  1 1 1 1  1 1 1 1  0 0 0 0  0 0 0 0  1 1 1 1  1 1 1 1\n"+
        "64	0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1  "+
           "0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1  0 0 0 0  1 1 1 1\n"+
        "32	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0 ", Charset.defaultCharset());

        // Files.writeString(file.toPath(), "CAUSAL\n"+
        // "4\n"+
        // "2 2 2 8\n"+
        // "4\n"+
        // "2	3 0 \n"+
        // "3	0 3 1 \n"+
        // "3	1 3 2 \n"+
        // "1	3 \n"+
        // "8	0 0 0 0  0 0 0 0  \n"+
        // "16	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0 \n"+
        // "16	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0 \n"+
        // "8	0 0 0 0  0 0 0 0 ", Charset.defaultCharset());
        
        StructuralCausalModel model = new CausalUAIParser("party_causal_0.uai").parse();//
        //StructuralCausalModel model = new CausalUAIParser("triangolo_causal_0.uai").parse();//
        //StructuralCausalModel model = new CausalUAIParser(file.getAbsolutePath()).parse();
        BayesianFactor bf0 = model.getFactor(0);
        BayesianFactor bf1 = model.getFactor(1);
        BayesianFactor bf2 = model.getFactor(2);
        BayesianFactor bf3 = model.getFactor(3);

        DotSerialize serialize = new DotSerialize();
        System.out.println(serialize.run(model));
        
       // Map<Integer,BayesianFactor> map = EquationBuilder.of(model).conservative(3);

        model.fillExogenousWithRandomFactors(9);

        BayesianFactor bf = model.getFactor(3);
        double[] dta = bf.getData();
        System.out.println(Arrays.toString(dta));

        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        Table table = new Table(samples);
        System.out.print(table);

        int[] ivs = new int[] { 6 };//0,1,2,3,4,6,5 };
        for (int iv :ivs) {
            long start = System.currentTimeMillis();
            EMCredalBuilder builder = EMCredalBuilder.of(model.copy(), table.convert())
                .setMaxEMIter(20)
                .setNumTrajectories(1)
                .setThreshold(1)
                .setStopCriteria(StopCriteria.LLratio)
                .setInferenceVariation(iv)
                .build();
                System.out.println(iv + ": " +(System.currentTimeMillis() - start));
                if (iv >= 5) { 
                    System.out.println("ACE TIME: " + builder.getAceQueryTime());
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