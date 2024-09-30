package ch.idsia.credici.model.transform;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

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
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Compute the probability of Necessity and Sufficiency
 */
public class PNS {

	private boolean fixCBN; 
	private boolean removingVariables = true;
	private TIntIntMap evidence;
	
	public PNS() {
	}
	
	public PNS(boolean removingVariables) {
		this.removingVariables = removingVariables;
	}
	
	public void setRemovingVariables(boolean removingVariables) {
		this.removingVariables = removingVariables;
	}
	
	public boolean isRemovingVariables() {
		return removingVariables;
	}
	
	
	public void setFixCBN(boolean fix) {
		this.fixCBN = fix;
	}
	
	public boolean isFixCBN() {
		return fixCBN;
	}
	
	
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

	/**
	 * get The offset in the domain given the specified instantiation
	 * if state is missing in the instantiation it is assumed to be 0
	 * @param domain a domain
	 * @param instantiation the instantiation
	 * @return
	 */
	int offsetOf(Strides domain, TIntIntMap instantiation) {
		int[] states = Arrays.stream(domain.getVariables()).map(instantiation::get).toArray();
		return domain.getOffset(states);
	}
	
	TIntIntMap getInstantiation(int[] domain, IndexIterator iter) {
		var pos = iter.getPositions();
		if (pos.length != domain.length) return null;
		TIntIntMap inst = new TIntIntHashMap();
		for (int i = 0; i < pos.length; ++i) {
			inst.put(domain[i], pos[i]);
		}
		return inst;
	}
	
	public double execute(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate) {
		
		evidence = new TIntIntHashMap();
		
		Do<StructuralCausalModel> doing = new Do<>(removingVariables);
		StructuralCausalModel factual = doing.execute(model, cause, cause_truestate);
		evidence.putAll(doing.getNewEvidence());
		
		StructuralCausalModel counter = doing.execute(model, cause, cause_alternativestate);
		evidence.putAll(doing.getNewEvidence());
		
		Mapping mapping = new Mapping(model.getExogenousSet());
		mapping.add(factual);
		mapping.add(counter);

		StructuralCausalModel world = mapping.getModel();
		
		if (fixCBN) {
			for (int endo : model.getEndogenousVars()) {
				int y = mapping.mapToGlobal(factual, endo);
				int yp = mapping.mapToGlobal(counter, endo);
				
				int[] pay = world.getParents(y);
				int[] payp = world.getParents(yp);
				
				int[] p = ArraysUtil.unionSet(pay, payp);
				Arrays.sort(p);
				
				int d = world.addVariable(2);
				world.addParents(d, p);

				var ddom = world.getFullDomain(d);
				var ydom = world.getFullDomain(y);
				var ypdom = world.getFullDomain(yp);
				
				var yfactor = world.getFactor(y);
				var ypfactor = world.getFactor(yp);
				
				// just a convenience sequential iterator (no reordering or jumping around)
				var iter = ddom.getIterator();
				while(iter.hasNext()) {
					var inst = getInstantiation(ddom.getVariables(), iter);
					int offset = iter.next();
					// p = p(y) *p(y') *prod(sum(
					
				}
			}
		}
		
		
		int fe = mapping.mapToGlobal(factual, effect);
		int ce = mapping.mapToGlobal(counter, effect);

		RemoveBarren rb = new RemoveBarren();
		var world1 = rb.execute(world, new int[] { fe, ce }, evidence);

		MinFillOrdering mf = new MinFillOrdering();
		int[] order = mf.apply(world1);

		VE<BayesianFactor> ve = new VE<BayesianFactor>(order);
		ve.setFactors(world1.getFactors());
		ve.setNormalize(false);
		evidence.put(fe, effect_truestate);
		evidence.put(ce, effect_alternativestate);
		
		ve.setEvidence(evidence);

		BayesianFactor fact = ve.run(fe, ce);
		
//		var filter = ObservationBuilder.observe(fe, effect_truestate).and(ce, effect_alternativestate);
//		fact = fact.filter(filter);
		
		return fact.getData()[0]; // p(e)
	}

	public double executeOther(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate) {
		
		
		StructuralCausalModel factual = model.copy();
		StructuralCausalModel counter = model.copy();
		
		Mapping mapping = new Mapping(model.getExogenousSet());
		mapping.add(factual);
		mapping.add(counter);

		StructuralCausalModel world = mapping.getModel();
		int fe = mapping.mapToGlobal(factual, effect);
		int ce = mapping.mapToGlobal(counter, effect);
		
		int fc = mapping.mapToGlobal(factual, cause);
		int cc = mapping.mapToGlobal(counter, cause);

		//Do<BayesianFactor, StructuralCausalModel> doing = new Do<>();
		for (var p : world.getParents(cc))
			world.removeParent(cc, p);
		for (var p : world.getParents(fc))
			world.removeParent(fc, p);
		
		var dc = world.getFullDomain(cc);
		world.setFactor(cc, new BayesianFactor(dc, new double[dc.getCombinations()], true));

		var df = world.getFullDomain(fc);
		world.setFactor(fc, new BayesianFactor(df, new double[df.getCombinations()], true));
		
		RemoveBarren rb = new RemoveBarren();
		var world1 = rb.execute(world, new int[] { fe, ce, fc, cc });

		MinFillOrdering mf = new MinFillOrdering();
		int[] order = mf.apply(world1);

		VE<BayesianFactor> ve = new VE<BayesianFactor>(order);
		ve.setFactors(world1.getFactors());
		ve.setNormalize(false);
		ve.setEvidence(ObservationBuilder.observe(fe, effect_truestate).and(ce, effect_alternativestate).and(fc, cause_truestate).and(cc, cause_alternativestate));

		BayesianFactor fact = ve.run(fe, ce);
		
//		var filter = ObservationBuilder.observe(fe, effect_truestate).and(ce, effect_alternativestate);
//		fact = fact.filter(filter);
		
		return fact.getData()[0]; // p(e)
	}
	
	public StructuralCausalModel pnsmodel(StructuralCausalModel model, int cause, int effect) {
		return pnsmodel(model, cause, 1, 0, effect, 1, 0);
	}
	
	public StructuralCausalModel pnsmodel_nodo(StructuralCausalModel model) {
	
//		Do<BayesianFactor, StructuralCausalModel> doing = new Do<>();
//		StructuralCausalModel factual = doing.execute(model, cause, cause_truestate);
//		StructuralCausalModel counter = doing.execute(model, cause, cause_alternativestate);

		StructuralCausalModel factual = model.copy();
		StructuralCausalModel counter = model.copy();
		
		Mapping mapping = new Mapping(model.getExogenousSet());
		mapping.add(factual);
		mapping.add(counter);

		StructuralCausalModel world = mapping.getModel();


		//RemoveBarren rb = new RemoveBarren();
		return world;//.execute(world, new int[] { fe, ce });

	}
	
	
	public StructuralCausalModel pnsmodel(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate) {
		return pnsmodel(model, cause, cause_truestate, cause_alternativestate, effect, effect_truestate, effect_alternativestate, true);
	}
	
	
	public StructuralCausalModel pnsmodel(StructuralCausalModel model, int cause, int cause_truestate, int cause_alternativestate,
			int effect, int effect_truestate, int effect_alternativestate, boolean absorbeDo) {
	
		Do<StructuralCausalModel> doing = new Do<>(absorbeDo);
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
