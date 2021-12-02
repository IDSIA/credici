package examples;

import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;

import java.util.Arrays;

public class slides_code {
	public static void main(String[] args) throws InterruptedException {


		// Define the SCM structure
		StructuralCausalModel m = new StructuralCausalModel();

		//Nodes
		int X = m.addVariable(2, false);
		int Y = m.addVariable(2, false);
		int U = m.addVariable(5, true);

		// Arcs
		m.addParents(Y, U, X);
		m.addParents(X, U);

		// Define the equation P(X|U)
		m.setFactor(X,
				BayesianFactor.deterministic(
						m.getDomain(X), 		// children domain
						m.getDomain(U),			// parents domain
						0, 1, 1, 0, 0 // assignments
				)
		);

		// Define the equation P(Y|X,U)
		m.setFactor(Y,
				BayesianFactor.deterministic(
						m.getDomain(Y),
						m.getDomain(m.getParents(Y)),
						1, 1, 1, 1, 0, 0, 0, 1, 0, 0
				)
		);


		// Define the joint P(X,Y)
		BayesianFactor empProb = new BayesianFactor(
				m.getDomain(X,Y),
				new double[]{0.85, 0.02, 0.06, 0.07}
				);


		// Obtain the equivalent credal model with vertices
		SparseModel vmodel = m.toVCredal(empProb);

		// V-factor for U
		VertexFactor vf = (VertexFactor) vmodel.getFactor(U);
		System.out.println(vf);

		// Obtain the equivalent credal model with vertices
		SparseModel hmodel = m.toHCredal(empProb);

		// H-factor for U
		SeparateHalfspaceFactor hf = (SeparateHalfspaceFactor) hmodel.getFactor(U);
		hf.printLinearProblem();




		// Intialize exact inference algorithm
		CredalCausalVE cve = new CredalCausalVE(m, Arrays.asList(empProb));

		// Run an interventional  query
		VertexFactor interventionalQuery = (VertexFactor) cve.causalQuery()
				.setIntervention(X, 1)
				.setTarget(Y)
				.run();

		System.out.println(interventionalQuery.filter(Y,0));

		// Run a counterfactual query
		VertexFactor counterfactualQuery = (VertexFactor) cve.counterfactualQuery()
				.setIntervention(X, 1)
				.setEvidence(X, 0)
				.setTarget(Y)
				.run();

		System.out.println(counterfactualQuery.filter(4,0));



	}
}
