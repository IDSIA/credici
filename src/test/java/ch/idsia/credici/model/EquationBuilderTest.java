package ch.idsia.credici.model;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import jdk.jshell.spi.ExecutionControl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class EquationBuilderTest {


	@Test
	public void conservative(){
		//todo: uncomment when conservative is fixed

		/*
		// Define the model
		StructuralCausalModel m = new StructuralCausalModel();

		int x = m.addVariable(2, false);
		int y = m.addVariable(2, false);
		int z = m.addVariable(2, false);
		int a = m.addVariable(2, false);

		int u = m.addVariable(16, true);
		int w = m.addVariable(2, true);
		int v = m.addVariable(4, true);

		m.addParents(a, w);
		m.addParents(x, a,v);
		m.addParents(y,x,u);
		m.addParents(z,y,u);

		// For each exogenous variable, calculate the SEs of the children
		for(int exoVar : m.getExogenousVars()) {

			// Calculate the joint SE
			BayesianFactor f = BayesianFactor.combineAll(
					EquationBuilder.of(m).withAllAssignmentsQM(exoVar).values()
			);

			double data[] = f.marginalize(exoVar).getData();
			double unique = Arrays.stream(data).distinct().count();

			// The result of marginalizing the exogenous from the joint SE must be a factor with the same value
			// for each position
			Assert.assertEquals(1, unique, 0.0);
		}
	*/
	}
}
