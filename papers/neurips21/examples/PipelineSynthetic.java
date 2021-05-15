package neurips21.examples;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.ModelGenerator;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.Arrays;
import java.util.HashMap;

public class PipelineSynthetic {
	public static void main(String[] args) throws InterruptedException {
		RandomUtil.setRandomSeed(1);
		StructuralCausalModel model = ModelGenerator.RandomChain(5, 3);

		System.out.println(model);
		System.out.println("Exo tw: "+model.getExogenousTreewidth());
		System.out.println("Exogenous DAG:");
		System.out.println(model.getExogenousDAG());

		System.out.println("True empirical:");
		HashMap empTrue = 	model.getEmpiricalMap();
		System.out.println(empTrue);

		System.out.println("Data empirical:");
		TIntIntMap[] data = model.samples(500, model.getEndogenousVars());
		HashMap empData = DataUtil.getEmpiricalMap(model, data);
		System.out.println(empData);



		RandomUtil.setRandomSeed(0);
		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				//.setNumDecimalsRound(-1)
				.build();


		System.out.println("\tIs Inner approximation = " + builder.isInnerApproximation());
		System.out.println("\tConverging Trajectories = " +builder.getConvergingTrajectories().size());
		System.out.println("\tSelected points = " + builder.getSelectedPoints().size());

		System.out.println("trajectories sizes:");
		System.out.println(Arrays.toString(builder.getTrajectories().stream().map(t -> t.size()-1).toArray()));




	}
}
