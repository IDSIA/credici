package edu.neurips.causalem.inference;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class PearlBounds {

	double ptt, ptf, pff;

	public PearlBounds(BayesianNetwork bnet, int cause, int effect, int trueState, int falseState) {


		VariableElimination ve = new FactorVariableElimination(bnet.getVariables());
		ve.setFactors(bnet.getFactors());
		BayesianFactor pcond = (BayesianFactor) ve.conditionalQuery(effect, cause);

		ptt = pcond.filter(effect, trueState).filter(cause, trueState).getValueAt(0);
		ptf = pcond.filter(effect, trueState).filter(cause, falseState).getValueAt(0);
		pff = pcond.filter(effect, falseState).filter(cause, falseState).getValueAt(0);

	}
	public PearlBounds(BayesianNetwork bnet, int cause, int effect) {
		new PearlBounds(bnet, cause, effect, 0, 1);
	}
	public double[] propNecessity() {
		// PN
		double[] bounds = new double[2];
		bounds[0] = Math.max(0, ptt - ptf) / ptt;
		bounds[1] = Math.min(ptt, ptf) / ptt;
		return bounds;
	}
	public double[] probSufficiency(){

		// PS
		double[] bounds = new double[2];
		bounds[0] = Math.max(0, ptt - ptf)/pff;
		bounds[1] = Math.min(ptt,pff)/pff;
		return bounds;

	}
}
