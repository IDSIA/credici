package ch.idsia.credici.model.transform;

import java.util.function.Function;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;

public class Canonical implements Function<StructuralCausalModel, StructuralCausalModel> {

	public static Canonical LOG = new Canonical(true);
	public static Canonical NORMAL = new Canonical(false);

	private boolean log = false;

	private Canonical(boolean log) {
		this.log = log;
	}

	@Override
	public StructuralCausalModel apply(StructuralCausalModel cm) {
		StructuralCausalModel canonical = new StructuralCausalModel(cm.getName());
		Randomizer r = new Randomizer();

		for (int endo : cm.getEndogenousVars(true)) {
			canonical.addVariable(endo, cm.getSize(endo), cm.getVariableType(endo));
		}

		for (int exo : cm.getExogenousVars()) {
			int c = EquationOps.maxExoCardinality(exo, cm);
			canonical.addVariable(exo, c, VarType.EXOGENOUS);
		}

		for (int v : cm.getVariables()) {
			int[] parents = cm.getParents(v);
			canonical.addParents(v, parents);
		}

		for (int endo : cm.getEndogenousVars()) {
			int[] exo = cm.getExogenousParents(endo);

			if (exo.length > 1) {
				throw new UnsupportedOperationException("Currently only Markovian network are supported");
			}
			int exovar = exo[0];
			int exosize = canonical.getSize(exo[0]);

			Strides domain = canonical.getFullDomain(endo);
			Strides cond = domain.remove(endo, exovar);
			int exostride = domain.getStride(exovar);

			int endostride = domain.getStride(endo);

			int endosize = canonical.getSize(endo);
			if (endosize != 2)
				throw new UnsupportedOperationException("Only binary");

			BayesianFactor f = new BayesianFactor(domain, log);
			double[] data = f.getInteralData();
			
			double zero = log ? Double.NEGATIVE_INFINITY : 0;
			double one = log ? 0 : 1;

			if (log) { // otherwise java guarantees double vectors to be zero initialied
				for (int i = 0; i < domain.getCombinations(); ++i) {
					data[i] = zero;
				}
			}

			for (int exostate = 0; exostate < exosize; ++exostate) {
				// decompose
				int offset = exostate * exostride;

				int left = exostate;
				var iter = domain.getIterator(cond);
				
				for (int c = 0; c < cond.getCombinations(); ++c) {
					int poffset = iter.next();
					
					int state = left % endosize;
					left = left / endosize;
					int fulloffset = state * endostride + offset + poffset;
					data[fulloffset] = one;
				}
			}
			canonical.setFactor(endo, f);
			int [] order = new int[]{endo};
			var ff = f.reorderDomain(ArraysUtil.append(ArraysUtil.append(order,cond.getVariables()), new int[]{exovar})); 
			ff.toString();
		}

		for (int exo : cm.getExogenousVars()) {
			var m = new BayesianFactor(canonical.getFullDomain(exo), true);
			r.randomizeInplace(m, exo);
			canonical.setFactor(exo, m);
		}
		return canonical;
	}
}
