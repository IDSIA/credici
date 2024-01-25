package ch.idsia.credici.utility;

import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DirichletSampler;
import org.apache.commons.rng.simple.RandomSource;

import com.google.common.primitives.Doubles;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;

public class Randomizer {
	private UniformRandomProvider source;

	public Randomizer() {
		this(System.nanoTime());
	}
	
	public Randomizer(long seed) {
		source = RandomSource.JDK.create(seed);
	}

	
	
	/**
	 * Create a random CPT for the given variable and domain.
	 * 
	 * @param domain the full domain of the factor
	 * @param variable the target variable (rest of domain assumed to be conditioning)
	 * @param sorted whether domain should be sorted
	 * @return a new {@link BayesianFactor} CPT with random distributions
	 */
	public BayesianFactor randomFactor(Strides domain, int variable, boolean sorted) {

		if (sorted) {
			domain = domain.sort();
		}
		
		BayesianFactor factor = new BayesianFactor(domain);
		randomizeInplace(factor, variable);
		return factor;
	}
	
	/**
	 * randomize the given Bayesian factor. This will normalize assuming the factor to
	 * define P(variable|...).
	 * 
	 * This will generate a probability sampled from a dirichlet with alpha == 1.
	 */
	public void randomizeInplace(BayesianFactor factor, int variable){
		Strides domain = factor.getDomain();
		Strides left = domain.retain(new int[] { variable });
		Strides right = domain.remove(variable);

        DirichletSampler x = DirichletSampler.symmetric(source, left.getCombinations(), 1);
		double[][] data = x.samples(right.getCombinations()).toArray(len->new double[len][]);
		double[] dta = Doubles.concat(data);
		int[] order = left.concat(right).getVariables();
		
		factor.setData(order, dta);
	}
	
	
	public BayesianFactor randomize(BayesianFactor factor, int variable) {
		BayesianFactor c = factor.copy();
		randomizeInplace(c, variable);
		return c;
	}
}
