package ch.idsia.credici.model.transform;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.Mapping;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.io.dot.Info;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.preprocess.RemoveBarren;

/**
 * Compute the probability of Necessity and Sufficiency
 */
public class PNS {

	/**
	 * Execute query assuming factual state = 1, alternative state = 0
	 * 
	 * @param model
	 * @param cause
	 * @param effect
	 * @return
	 */
	public double execute(StructuralCausalModel model, int cause, int effect) {
		return execute(model, cause, 1, 0, effect, 1, 0);
	}

	public double execute(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate) {

		Do<BayesianFactor, StructuralCausalModel> doing = new Do<>();
		StructuralCausalModel factual = doing.execute(model, cause, cause_truestate);
		StructuralCausalModel counter = doing.execute(model, cause, cause_alternativestate);

		Mapping mapping = new Mapping(model.getExogenousSet());
		mapping.add(factual);
		mapping.add(counter);

		StructuralCausalModel world = mapping.getModel();
		int fe = mapping.mapToGlobal(factual, effect);
		int ce = mapping.mapToGlobal(counter, effect);

		RemoveBarren rb = new RemoveBarren();
		var world1 = rb.execute(world, new int[] { fe, ce });

		MinFillOrdering mf = new MinFillOrdering();
		int[] order = mf.apply(world1);

		VE<BayesianFactor> ve = new VE<BayesianFactor>(order);
		ve.setFactors(world1.getFactors());
		ve.setNormalize(false);
		ve.setEvidence(ObservationBuilder.observe(fe, effect_truestate).and(ce, effect_alternativestate));

		BayesianFactor fact = ve.run(fe, ce);
		
//		var filter = ObservationBuilder.observe(fe, effect_truestate).and(ce, effect_alternativestate);
//		fact = fact.filter(filter);
		
		return fact.getData()[0]; // p(e)
	}

	
	
	public StructuralCausalModel pnsmodel(StructuralCausalModel model, int cause, int effect) {
		return pnsmodel(model, cause, 1, 0, effect, 1, 0);
	}
	
	
	public StructuralCausalModel pnsmodel(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate) {

		Do<BayesianFactor, StructuralCausalModel> doing = new Do<>();
		StructuralCausalModel factual = doing.execute(model, cause, cause_truestate);
		StructuralCausalModel counter = doing.execute(model, cause, cause_alternativestate);

		Mapping mapping = new Mapping(model.getExogenousSet());
		mapping.add(factual);
		mapping.add(counter);

		StructuralCausalModel world = mapping.getModel();
		int fe = mapping.mapToGlobal(factual, effect);
		int ce = mapping.mapToGlobal(counter, effect);

		RemoveBarren rb = new RemoveBarren();
		return rb.execute(world, new int[] { fe, ce });

	}
	
	
	public static void main(String[] args) throws InterruptedException {
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

		System.out.println(xx + " " + x + " " + (xx - x.getData()[0]));
	}
}
