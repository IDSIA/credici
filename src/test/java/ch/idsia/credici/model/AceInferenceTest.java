package ch.idsia.credici.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class AceInferenceTest {
    @Test
    public void serializeTest() throws IOException {
        BayesianNetwork bnet = new BayesianNetwork();
		int y = bnet.addVariable(2);
		int x = bnet.addVariable(2);
        bnet.addParent(x, y);

		bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
		bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[] {0.1, 0.2, 0.3, 0.4}));
        
        Path wdir = Path.of("");
        Path modelfile = wdir.resolve("./models/synthetic/chain_twExo2_nEndo5_0.uai");
        StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(modelfile.toString());

        try {
            AceInference ai = new AceInference("src/resources/ace");
            File f = ai.setNetwork(model);
            ai.compile();
            System.out.println(Files.readString(f.toPath()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
