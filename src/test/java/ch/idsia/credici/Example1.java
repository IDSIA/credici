package ch.idsia.credici;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.rng.sampling.distribution.DirichletSampler;
import org.apache.commons.rng.simple.RandomSource;

import ch.idsia.credici.learning.WeightedCausalEM;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

public class Example1 {
    public static void main(String[] args) throws IOException, InterruptedException {
        File file = File.createTempFile("Credici", ".uai");
        Files.writeString(file.toPath(), "CAUSAL\n"+
        "4\n"+
        "2 2 2 32\n"+
        "4\n"+
        "2	3 0 \n"+
        "3	3 0 1 \n"+
        "3	3 1 2 \n"+
        "1	3 \n"+
        "32	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0 \n"+
        "64	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0\n"+
        "64	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0\n"+
        "32	0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0  0 0 0 0 ", Charset.defaultCharset());
        Files.writeString(file.toPath(), "CAUSAL\n"+
        "4\n"+
        "2 2 2 3\n"+
        "4\n"+
        "2	3 0 \n"+
        "3	3 0 1 \n"+
        "3	3 1 2 \n"+
        "1	3 \n"+
        "3	0 0 0   \n"+
        "6	0 0 0 0  0 0 \n"+
        "6	0 0 0 0  0 0 \n"+
        "3	0 0 0 ", Charset.defaultCharset());

        StructuralCausalModel model = new CausalUAIParser(file.getAbsolutePath()).parse();
        RandomUtil.getRandom().setSeed(0xfeedbeaf);
        model.fillWithRandomEquations();
        
        try(FileWriter out = new FileWriter("out.csv")){
        for (int i = 0; i < 100; ++i) {
            model.fillExogenousWithRandomFactors(9);
            BayesianFactor bf = model.getFactor(3);
            out.write(Arrays.stream(bf.getData()).mapToObj(x->Double.toString(x)).collect(Collectors.joining(",")));
            out.write("\n");
            
        }
        } 

       
        EMCredalBuilder builder = EMCredalBuilder.of(model, null)
        .setMaxEMIter(1000)
        .setNumTrajectories(500)
        .setWeightedEM(true)
        .setThreshold(1)
        .setStopCriteria(StopCriteria.LLratio)
        .build();
        

        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        Table table = new Table(samples);
        System.out.print(table);
    }
}
