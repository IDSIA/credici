package ch.idsia.credici.inference;

import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

public class CausalEMVE extends CausalMultiVE{

	public CausalEMVE(StructuralCausalModel model, TIntIntMap[] data, int runs, int maxiter) throws InterruptedException {
		super(EMCredalBuilder.of(model, data).setWeightedEM(true).setNumTrajectories(runs)
				.setTrainableVars(model.getExogenousVars())
				.setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
				.setThreshold(0.0)
				.setMaxEMIter(maxiter).build().getSelectedPoints());
	}


}
