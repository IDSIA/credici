package ch.idsia.credici;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import gnu.trove.map.TIntIntMap;

public class Example1 {
    public static void main(String[] args) throws IOException {
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

        StructuralCausalModel model = new CausalUAIParser(file.getAbsolutePath()).parse();
        model.fillWithRandomEquations();
        model.fillExogenousWithRandomFactors(2);
  


        TIntIntMap[] samples = model.samples(1000, model.getEndogenousVars());
        Table table = new Table(samples);
        System.out.print(table);
    }
}
