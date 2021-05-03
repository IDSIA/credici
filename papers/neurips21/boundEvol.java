package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;

import java.io.IOException;
import java.util.ArrayList;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqMask;

public class boundEvol {
	public static void main(String[] args) throws InterruptedException, IOException {

		String PRJ_PATH = "./";

		// Set input of experiments
		String modelName = PRJ_PATH+"papers/neurips21/models/nonquasi_chain_n4_x2_u8_0.uai";
		int step = 1;
		int numTrajectories = 50;
		int numSequences = 1;

		EMCredalBuilder.SelectionPolicy policies[] =  new EMCredalBuilder.SelectionPolicy[]{
				EMCredalBuilder.SelectionPolicy.LAST,
				//      EMCredalBuilder.SelectionPolicy.BISECTION_BORDER_SAME_PATH,
				//      EMCredalBuilder.SelectionPolicy.BISECTION_BORDER
		};

		// Define random sequential masks
		ArrayList masks = new ArrayList();
		for(int i=0; i<numSequences; i++)
			masks.addAll(getRandomSeqMask(numTrajectories));


// Run all the experiments

		StructuralCausalModel model = (StructuralCausalModel) IO.read(modelName);


		CredalCausalVE infExact = new CredalCausalVE(model);

		// set up and run a causal query
		VertexFactor resExact = (VertexFactor) infExact
				.causalQuery()
				.setTarget(3)
				.setIntervention(0,1)
				.run();

		//System.out.println("\n"+s);
		System.out.println(resExact);



		EMCredalBuilder builder = EMCredalBuilder.of(model)
				.setNumTrajectories(numTrajectories)
				.setMaxEMIter(500)
				.buildTrajectories();

		System.out.println("build trajectories");

		builder.getConvergingTrajectories();

		System.out.print(",u0_true,l0_true,u1_true,l1_true");
		System.out.println(",u0_last,l0_last,u1_last,l1_last");


		for(int i=0; i<masks.size(); i++) {
			System.out.print(i);

			/// Exact result
			IntervalFactor ires = new VertexToInterval().apply(resExact, 3);
			String s = "";
			for(int j=0; j<ires.getDomain().getCombinations(); j++){
				s+=","+ires.getUpper(0)[j]+","+ires.getLower(0)[j];
			}
			System.out.print(s);

			for(EMCredalBuilder.SelectionPolicy pol : policies) {

				boolean[] m = (boolean[]) masks.get(i);
				builder.setMask(m).setSelPolicy(pol).selectAndMerge();

				//System.out.println(builder.isInnerApproximation());
				CredalCausalVE inf2 = new CredalCausalVE(builder.getModel());
				VertexFactor res2 =
						(VertexFactor) inf2.causalQuery()
								.setTarget(3)
								.setIntervention(0, 1)
								.run();

				ires = new VertexToInterval().apply(res2, 3);
				s = "";
				for(int j=0; j<ires.getDomain().getCombinations(); j++){
					s+=","+ires.getUpper(0)[j]+","+ires.getLower(0)[j];
				}
				System.out.print(s);
			}
			System.out.println("");
		}

	}
}
