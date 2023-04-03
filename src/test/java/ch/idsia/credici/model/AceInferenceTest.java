package ch.idsia.credici.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Test;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.credici.learning.ve.VE;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.Instantiation;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.model.math.FactorOperation;
import gnu.trove.map.hash.TIntIntHashMap;

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
            assertTrue(true);
            ai.update(model);
            ai.query(3, ObservationBuilder.observe(1, 1));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Test
    public void serializeTest2() throws IOException, InterruptedException {
        BayesianNetwork bnet = new BayesianNetwork();
		int y = bnet.addVariable(2);
		int x = bnet.addVariable(2);
        bnet.addParent(x, y);

		bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
		bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[] {0.1, 0.2, 0.3, 0.4}));
        
       
        StructuralCausalModel model = new StructuralCausalModel(){
            @Override
            public BayesianNetwork toBnet() {
                return bnet;
            }
            @Override
            public int[] getExogenousVars() {
                return new int[]{y};
            }

            @Override
            public BayesianFactor getFactor(int variable) {
                return bnet.getFactor(variable);
            }
        };    

        try {
            AceInference ai = new AceInference("src/resources/ace");
    
            File f = ai.setNetwork(model);
            ai.compile();
            System.out.println(Files.readString(f.toPath()));
            assertTrue(true);

            System.out.println(Files.readString(f.toPath()));
            assertTrue(true);
            ai.update(model);
            
            double[] xx = ai.query(x, new TIntIntHashMap());//new int[]{x}, new int[]{0}));
            System.out.println(Arrays.toString(xx));

            double xxx = ai.pevidence(new TIntIntHashMap(new int[]{x}, new int[]{0}));
            System.out.println(xxx);


            VE<BayesianFactor> ve = new VE<BayesianFactor>(new MinFillOrdering().apply(bnet));
        
            BayesianFactor fac = ve.apply(bnet, x);
            System.out.println("caio");
            System.out.println(Arrays.toString(fac.getData()));

            //ve.setInstantiation(
            ve.setNormalize(false);
            fac = ve.apply(bnet, new int[0], new TIntIntHashMap(new int[]{x}, new int[]{0}));
            System.out.println("caio");
            System.out.println(Arrays.toString(fac.getData()));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    @Test
    public void x() {
        System.out.println()
        ;
    }
}
