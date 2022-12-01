package ch.idsia.credici.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class AceInferenceTest {
    @Test
    public void serializeTest() {
        BayesianNetwork bnet = new BayesianNetwork();
		int y = bnet.addVariable(2);
		int x = bnet.addVariable(2);
        bnet.addParent(x, y);

		bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
		bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[] {0.1, 0.2, 0.3, 0.4}));

        try {
            File f = new AceInference().setNetwork(bnet);
            System.out.println(Files.readString(f.toPath()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
