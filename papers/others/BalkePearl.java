import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.util.Arrays;

public class BalkePearl {

	public static void main(String[] args) throws InterruptedException {

		StructuralCausalModel m = new StructuralCausalModel();


		int Z = m.addVariable(2, false);
		int D = m.addVariable(2, false);
		int Y = m.addVariable(2, false);
		int V = m.addVariable(2, true);
		int U = m.addVariable(16, true);

		m.fillExogenousWithRandomFactors(2);

		m.addParents(Z, V);
		m.addParents(D, Z, U);
		m.addParents(Y, D, U);


		BayesianFactor fz = EquationBuilder.of(m).withAllAssignments(Z);
		m.setFactor(Z, fz);


		BayesianFactor fd = EquationBuilder.of(m)
				.fromVector(D,
						1,1, 1,1, 1,1, 1,1,
						1,0, 1,0, 1,0, 1,0,
						0,1, 0,1, 0,1, 0,1,
						0,0, 0,0, 0,0, 0,0
				);

		m.setFactor(D, fd);

		fd.filter(D,0).filter(Z,0);
		fd.filter(D,1).filter(Z,0);
		fd.filter(D,0).filter(Z,1);
		fd.filter(D,1).filter(Z,1);



		BayesianFactor fy = EquationBuilder.of(m)
				.fromVector(Y,
						1,1, 1,0, 0,1, 0,0,
						1,1, 1,0, 0,1, 0,0,
						1,1, 1,0, 0,1, 0,0,
						1,1, 1,0, 0,1, 0,0
				);


		m.setFactor(Y, fy);



		fy.filter(Y,0).filter(D,0);
		fy.filter(Y,1).filter(D,0);
		fy.filter(Y,0).filter(D,1);
		fy.filter(Y,1).filter(D,1);

		// empirical factors
		BayesianFactor px = new BayesianFactor(m.getDomain(Z), new double[]{.9, .1});

		BayesianFactor pyz_x = new BayesianFactor(m.getDomain(D, Y, Z)
				, new double[]{.32, .32, .04, .32, .02, .17, .67, .14});

		//pyz_x.filter(X,0).filter(Z,0)
		BayesianFactor[] empFactors = new BayesianFactor[]{px, pyz_x};


		CredalCausalVE inf = new CredalCausalVE(m, empFactors);
		VertexFactor p1 = (VertexFactor)inf.causalQuery().setIntervention(D,1).setTarget(Y).run();
		VertexFactor p2 = (VertexFactor)inf.causalQuery().setIntervention(D,0).setTarget(Y).run();

		p1.filter(Y, 1);
		p2.filter(Y, 1);


		// ACE = [0.45, 0.52] - [0.67, 0.68]

		double ACEl = 0.52 - 0.67;
		double ACEu = 0.45 - 0.68;



		VariableElimination ve = null;
		SparseModel vmodel = inf.getModel();

		int n = 100;
		double[] ACEs = new double[n];
		BayesianNetwork[] bnets = vmodel.sampleVertex(n);

		int i = 0;
		for(BayesianNetwork bn : bnets) {
			BayesianNetwork bn0 = (BayesianNetwork) CausalOps.intervention(bn, D, 0, true);
			BayesianNetwork bn1 = (BayesianNetwork) CausalOps.intervention(bn, D, 1, true);

			double q0 = BayesianFactor.combineAll(bn0.getFactors(U, D, Y))
					.marginalize(U)
					.filter(Y, 1)
					.filter(D, 0)
					.getData()[0];

			double q1 = BayesianFactor.combineAll(bn1.getFactors(U, D, Y))
					.marginalize(U)
					.filter(Y, 1)
					.filter(D, 1)
					.getData()[0];

			ACEs[i]= q0 - q1;
			i++;

		}

		Arrays.stream(ACEs).max();

		Arrays.stream(ACEs).min();


	}
}
