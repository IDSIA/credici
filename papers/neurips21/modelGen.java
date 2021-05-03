package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;

import java.io.IOException;

public class modelGen {
	public static void main(String[] args) throws IOException, InterruptedException {

		String PRJ_PATH = "./";

		//for(int s=0; s<100; s++){
		RandomUtil.setRandomSeed(46);

		StructuralCausalModel model = RandomChainNonMarkovian.buildModel(4,2, 8);

		CredalCausalVE infExact = new CredalCausalVE(model);

		// set up and run a causal query
		VertexFactor resExact = (VertexFactor) infExact
				.causalQuery()
				.setTarget(3)
				.setIntervention(0,1)
				.run();

		//System.out.println("\n"+s);
		System.out.println(resExact);
		//}

		// save to disk
		IO.write(model, PRJ_PATH+"papers/neurips21/models/nonquasi_chain_n4_x2_u8_0.uai");

	}
}
