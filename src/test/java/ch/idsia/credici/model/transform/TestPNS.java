package ch.idsia.credici.model.transform;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;

/**
 * Compute the probability of Necessity and Sufficiency
 */
public class TestPNS {
	public static void main(String[] args) throws InterruptedException {
		new TestPNS().verify();
	}
	@Test
	public void verify() throws InterruptedException {
		StructuralCausalModel one = new StructuralCausalModel();

		int A = one.addVariable(2);
		int B = one.addVariable(2);
		int C = one.addVariable(4);
		int U = one.addVariable(10, true);
		int U2 = one.addVariable(10, true);

		one.addParents(A, B, U, U2);
		one.addParents(B, C, U2);
		one.addParents(C, U);

		var fA = BayesianFactor.random(one.getDomain(A), one.getDomain(B, U, U2), 4, false);
		one.setFactor(A, fA);
		var fB = BayesianFactor.random(one.getDomain(B), one.getDomain(C, U2), 4, false);
		one.setFactor(B, fB);
		var fC = BayesianFactor.random(one.getDomain(C), one.getDomain(U), 4, false);
		one.setFactor(C, fC);

		var fU = BayesianFactor.random(one.getDomain(U), Strides.EMPTY, 4, false);
		one.setFactor(U, fU);
		var fU2 = BayesianFactor.random(one.getDomain(U2), Strides.EMPTY, 4, false);
		one.setFactor(U2, fU2);

		PNS pns = new PNS();
		double xx = pns.execute(one, C, 0, 1, A, 0, 1);

		CausalVE cve = new CausalVE(one);
		BayesianFactor x = cve.probNecessityAndSufficiency(C, A);

		assertEquals(xx, x.getValueAt(0), 0.00000001);
	}
}
