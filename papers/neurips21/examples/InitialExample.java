package neurips21.examples;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.DoubleStream;

public class InitialExample
{
	public static void main(String[] args) throws InterruptedException {

		StructuralCausalModel m = new StructuralCausalModel();
		int X = m.addVariable(2);
		int Y = m.addVariable(2);
		int V = m.addVariable(2,true);
		int U = m.addVariable(4,true);



	}
}
