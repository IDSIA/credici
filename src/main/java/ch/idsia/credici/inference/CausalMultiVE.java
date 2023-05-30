package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import jdk.jshell.spi.ExecutionControl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

public class CausalMultiVE extends CausalInference<List<StructuralCausalModel>, GenericFactor>{

	private List<CausalVE> inf;

	boolean toInterval = false;

	List<StructuralCausalModel> inputModels = null;


	private CausalMultiVE(){

	}


	public CausalMultiVE(List<StructuralCausalModel> model){
		this.model=model;
		inputModels = model;
		inf = model.stream().map(m-> new CausalVE(m)).collect(Collectors.toList());

	}

	public static CausalMultiVE as(List<CausalVE> infList){
		CausalMultiVE obj = new CausalMultiVE();
		obj.model= infList.stream().map(i -> i.getModel()).collect(Collectors.toList());
		obj.inf = infList;
		return obj;
	}

	@Override
	public GenericFactor run(Query q) throws InterruptedException {
		// due to the conversion
		if(q.getTarget().length>1)
			throw new IllegalArgumentException("Only queries over a single variable are allowed.");

		// Run each query independently
		List<BayesianFactor> factors = inf.stream().map(i->i.run(q)).collect(Collectors.toList());


		// Merge the results
		VertexFactor vf = FactorUtil.mergeFactors(factors, inf.get(0).target[0], !toInterval);

		if(toInterval)
			return new VertexToInterval().apply(vf, q.getTarget()[0]);
		return vf;

	}

	@Override
	public List<StructuralCausalModel> getInferenceModel(Query q, boolean simplify) {
		return inf.stream().map(i->i.getInferenceModel(q, simplify)).collect(Collectors.toList());
	}

	public CausalMultiVE setToInterval(boolean toInterval) {
		this.toInterval = toInterval;
		return this;
	}

	public List<CausalVE> getInferenceList() {
		return inf;
	}

	@Override
	public GenericFactor averageCausalEffects(int cause, int effect, int effectVal, int causeVal1, int causeVal2) throws InterruptedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;

		for (CausalVE i : this.getInferenceList()) {
			double ace = ((BayesianFactor)i.averageCausalEffects(cause, effect, effectVal, causeVal1, causeVal2)).getValueAt(0);
			if (ace > max) max = ace;
			if (ace < min) min = ace;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}

	public double[] getIndividualPNS(int cause, int effect) {
		return getIndividualPNS(cause, effect,0,1);
	}

	public double[] getIndividualPNS(int cause, int effect, int trueState, int falseState){
		return this.getInferenceList()
				.stream()
				.mapToDouble( i -> {
					try {
						return i.probNecessityAndSufficiency(cause, effect, trueState, falseState).getValue(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return Double.NaN;
					} catch (ExecutionControl.NotImplementedException e) {
						e.printStackTrace();
						return Double.NaN;
					}
				})
				.toArray();
	}

	public double[] getIndividualPNS(int cause, int effect, int trueState, int falseState, int effectTrue, int effectFalse){
		return this.getInferenceList()
				.stream()
				.mapToDouble( i -> {
					try {
						return i.probNecessityAndSufficiency(cause, effect, trueState, falseState, effectTrue, effectFalse).getValue(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return Double.NaN;
					} catch (ExecutionControl.NotImplementedException e) {
						e.printStackTrace();
						return Double.NaN;
					}
				})
				.toArray();
	}

	@Override
	public GenericFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;


		for (CausalVE i : this.getInferenceList()) {
			double psn = i.probNecessityAndSufficiency(cause, effect, trueState, falseState).getValue(0);
			if (psn > max) max = psn;
			if (psn < min) min = psn;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}

	public GenericFactor probNecessityAndSufficiency(int cause, int effect, int causeTrue, int causeFalse, int effectTrue, int effectFalse) throws InterruptedException, ExecutionControl.NotImplementedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;


		for (CausalVE i : this.getInferenceList()) {
			double psn = i.probNecessityAndSufficiency(cause, effect, causeTrue, causeFalse, effectTrue, effectFalse).getValue(0);
			if (psn > max) max = psn;
			if (psn < min) min = psn;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}


	public GenericFactor probNecessity(int cause, int effect, int trueState, int falseState) throws InterruptedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;


		for (CausalVE i : this.getInferenceList()) {
			double p = i.probNecessity(cause, effect, trueState, falseState).getValue(0);
			if (p > max) max = p;
			if (p < min) min = p;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}

	public GenericFactor probSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;


		for (CausalVE i : this.getInferenceList()) {
			double p = i.probSufficiency(cause, effect, trueState, falseState).getValue(0);
			if (p > max) max = p;
			if (p < min) min = p;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}


	public CausalMultiVE filter(int[] mask){
		List newInfList = Arrays
				.stream(ArraysUtil.where(mask, i -> i > 0))
				.mapToObj(i -> this.inf.get(i))
				.collect(Collectors.toList());
		return as(newInfList);

	}

	public int pointsForConvergingPNS(double ratioMin, int cause, int effect) throws InterruptedException, ExecutionControl.NotImplementedException {
		return pointsForConvergingPNS(ratioMin, cause, effect, 0, 1);
	}

	public int pointsForConvergingPNS(double ratioMin, int cause, int effect,  int trueState, int falseState) throws ExecutionControl.NotImplementedException, InterruptedException {

		IntervalFactor pns = new VertexToInterval().apply((VertexFactor) this.probNecessityAndSufficiency(cause, effect, trueState, falseState));
		List masks = getRandomSeqIntMask(this.getInferenceList().size());

		for (int npoints = masks.size(); npoints > 0; npoints--) {
			int[] mask = (int[]) masks.get(npoints - 1);
			IntervalFactor pns_i = new VertexToInterval().apply((VertexFactor) this.filter(mask).probNecessityAndSufficiency(cause, effect, trueState, falseState));
			double ratio_i = (pns_i.getUpper()[0] - pns_i.getLower()[0]) / (pns.getUpper()[0] - pns.getLower()[0]);
			//System.out.println("" + npoints + ": " + ratio_i);

			if (ratio_i < ratioMin) {
				return npoints + 1;
			}
		}
		return 1;
	}

	public List<StructuralCausalModel> getInputModels() {
		return inputModels;
	}
}
