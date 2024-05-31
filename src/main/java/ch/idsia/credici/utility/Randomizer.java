package ch.idsia.credici.utility;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.distribution.DirichletSampler;
import org.apache.commons.rng.simple.RandomSource;

import com.google.common.primitives.Doubles;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class Randomizer {
	private UniformRandomProvider source;

	public Randomizer() {
		this(System.nanoTime());
	}
	
	public Randomizer(long seed) {
		source = RandomSource.JDK.create(seed);
	}

	
	
	/**
	 * Replace Endogenous variables' equations with a random ones. 
	 * The original factor will remain valid but not associated to the variable.
	 *
	 * @param model
	 */
	public void randomEndogenousEquationsInplace(StructuralCausalModel model) {
    	model.fillWithRandomEquations();
    	for (int e : model.getEndogenousVars(true)) {
    		var f = model.getFactor(e);
    		f = f.reorderDomain(f.getDomain().sort());
    		model.setFactor(e, f);
    	}
	}
	

	
	public StructuralCausalModel randomEndogenousEquations(StructuralCausalModel model) {
		StructuralCausalModel result = model.copy();
		randomEndogenousEquationsInplace(result);
		return result;
	}
	

	/**
	 * Replace exogenous variables' factor with a random one. 
	 * The original factor will remain valid but not associated to the variable.
	 *
	 * @param model
	 */
	public void randomExogenousInplace(StructuralCausalModel model) {
    	for (int i : model.getExogenousVars()) {
    		Strides dom = model.getFullDomain(i);
    		BayesianFactor bf = randomFactor(dom, i, true, true);
    		model.setFactor(i, bf);
    	}
	}
	
	/**
	 * Create a new model with random exogenous distributions
	 * The original factor will remain valid but not associated to the variable.
	 *
	 * @param model
	 */
	public StructuralCausalModel randomExogenous(StructuralCausalModel model) {
		var result = model.copy();
    	for (int i : result.getExogenousVars()) {
    		Strides dom = result.getFullDomain(i);
    		BayesianFactor bf = randomFactor(dom, i, true, true);
    		result.setFactor(i, bf);
    	}
    	return result;
	}
	
	
	/**
	 * Create a random CPT for the given variable and domain.
	 * 
	 * @param domain the full domain of the factor
	 * @param variable the target variable (rest of domain assumed to be conditioning)
	 * @param sorted whether domain should be sorted
	 * @return a new {@link BayesianFactor} CPT with random distributions
	 */
	public BayesianFactor randomFactor(Strides domain, int variable, boolean sorted, boolean log) {

		if (sorted) {
			domain = domain.sort();
		}
		
		BayesianFactor factor = new BayesianFactor(domain,log);
		randomizeInplace(factor, variable);
		return factor;
	}

	
	public void randomizeInplace(BayesianFactor factor, int variable){
		randomizeInplace(factor, variable, 1.0);
	}


	/**
	 * randomize the given Bayesian factor. This will normalize assuming the factor to
	 * define P(variable|...).
	 * 
	 * This will generate a probability sampled from a dirichlet with alpha == 1.
	 */
	public void randomizeInplace(BayesianFactor factor, int variable, double alpha){
//		boolean log = factor.isLog();
		
		Strides domain = factor.getDomain();
		Strides left = domain.retain(new int[] { variable });
		Strides right = domain.remove(variable);

        DirichletSampler x = DirichletSampler.symmetric(source, left.getCombinations(), alpha);
		double[][] data = x.samples(right.getCombinations()).toArray(len->new double[len][]);
		double[] dta = Doubles.concat(data);
		int[] order = left.concat(right).getVariables();
		
//		if (log) dta = Arrays.stream(dta).map(Math::log).toArray();
		// log if needed
		factor.setData(order, dta);
	}
	
	
	public BayesianFactor randomize(BayesianFactor factor, int variable) {
		BayesianFactor c = factor.copy();
		randomizeInplace(c, variable);
		return c;
	}

	public void randomMarkovianEquationsInplace(BayesianFactor factor, int variable, int u) {
		var domain = factor.getDomain().sort();
		int[] rem = (u > variable) ? new int[] { variable, u } : new int[] { u, variable };
		
		var conditioning = domain.remove(rem);
		int con = conditioning.getCombinations();
		int size = domain.getCardinality(variable);
		
		BigInteger canonical = BigInteger.valueOf(size).pow(con);

		int len = canonical.bitLength();
		
		
	}
	
	public void uniformInplace(BayesianFactor factor, int variable) {
		int size = factor.getDomain().getCardinality(variable);
		double p = factor.isLog() ? Math.log(1.0/size) : 1.0 / size;
		double[] data = factor.getInteralData();
		for (int i = 0; i < data.length; ++i) data[i] = p;
	}
	
	
	
	public StructuralCausalModel makeRandom(StructuralCausalModel model, int maxsize, int[] limits) {
		
		TIntIntMap sizes = new TIntIntHashMap();
		for (int exo : model.getExogenousVars()) {
			int[] ch = model.getChildren(exo);
			if (ch.length != 1) throw new IllegalStateException();
			int[] p = model.getEndogenousParents(ch[0], true);
			
			int s = model.getSize(ch[0]);
			int ps = model.getDomain(p).getCombinations();
			
			int sn = s * ps; // the size of a single mechanism
			
			// limit the maxsize
			int x = (int) Math.pow(s, ps);
			if (limits != null) {
				int xx = x;
				for (var limit : limits) {
					if (x > limit) xx = limit;
				}
				s=xx;
			} else {
				s = (int) Math.min(x, maxsize);
			}
			// at least the minimal number of mechanisms
			//s = Math.max(sn, s);
			sizes.put(exo, s);
		}
		return makeRandom(model, sizes);
	}
	
	
	public StructuralCausalModel makeRandom(StructuralCausalModel model, double ratio) {
		TIntIntMap sizes = new TIntIntHashMap();
		for (int exo : model.getExogenousVars()) {
			int[] ch = model.getChildren(exo);
			if (ch.length != 1) throw new IllegalStateException();
			int[] p = model.getEndogenousParents(ch[0], true);
			
			int s = model.getSize(ch[0]);
			int ps = model.getDomain(p).getCombinations();
			
			int sn = s * ps; // the size of a single mechanism
			
			// number of possible mechanisms * ratio
			s = (int) (Math.pow(s, ps) * ratio);
			
			// at least the minimal number of mechanisms
			s = Math.max(sn, s);
			sizes.put(exo, s);
		}
		return makeRandom(model, sizes);
	}
	
	public StructuralCausalModel makeRandom(StructuralCausalModel model, TIntIntMap sizes) {
		StructuralCausalModel random = new StructuralCausalModel(model.getName());
		random.copyData(model);
		for (int variable : model.getVariables()) {
			int size; 
			if (sizes.containsKey(variable)) {
				size = sizes.get(variable);
			} else {
				size = model.getSize(variable);
			}
			random.addVariable(variable, size, model.getVariableType(variable));
		}
		
		for (int variable : model.getEndogenousVars(true)) {
			int[] p = model.getParents(variable);
			random.addParents(variable, p);
		}
		
		randomEndogenousEquationsInplace(random);
		randomExogenousInplace(random);
		
		return random;
	}
	
	
	static record Helper(
		int offset,
		double[] data)
	{
		
		@Override
		public int hashCode() {
			return Arrays.hashCode(data);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			return Arrays.equals(data, ((Helper) obj).data);
		}
	}
	
	public StructuralCausalModel makeRandomMarkovian(StructuralCausalModel dag) {
		final var model = Canonical.LOG.apply(dag);
		
		TIntIntMap sizes = new TIntIntHashMap();
		Arrays.stream(model.getExogenousVars()).forEach(y->sizes.put(y, model.getSize(y)));
		
		var x = makeRandom(model, sizes);
		
		var factors = new TIntObjectHashMap<HashSet<Helper>>();
		StructuralCausalModel random = new StructuralCausalModel();
		
		for (var e : model.getEndogenousVars()) {
			var px = model.getExogenousParents(e);
			var pe = model.getEndogenousParents(e);
			
			var endo_vars = ArraysUtil.addToSortedArray(pe, e);
			var endo_domain = model.getDomain(endo_vars);
			var endo_size = endo_domain.getCombinations();
			
			var f = x.getFactor(e);
			var interal = f.getInteralData();
			
			var dom = f.getDomain();
			
			var iexo = dom.getFiteredIndexIterator(endo_vars, new int[endo_vars.length]);
			
			HashSet<Helper> unique = new HashSet<>();
			while(iexo.hasNext()) {
				int exooff = iexo.next();
			
				var iendo = dom.getFiteredIndexIterator(px, new int[px.length]);
				double[] data = new double[endo_size];
				int i = 0;
				while(iendo.hasNext()) {
					int endooff = iendo.next();
					data[i++] = interal[exooff + endooff];
				}
				boolean dup = unique.add(new Helper(exooff, data));
			}
			
//			sizes.put(px[0], unique.size());
			factors.put(e, unique);
			
			random.addVariable(e, x.getSize(e));
			random.addVariable(px[0], unique.size(), true);
		}
		
		for (var e : x.getVariables()) {
			random.addParents(e, x.getParents(e));
		}
		
		for (var e : random.getEndogenousVars()) {

			var px = random.getExogenousParents(e);
			var pe = random.getEndogenousParents(e);
			
			var endo_vars = ArraysUtil.addToSortedArray(pe, e);
			var endo_domain = random.getDomain(endo_vars);
			var endo_size = endo_domain.getCombinations();
			
			var dom = random.getFullDomain(e);
			double[] fulldata = new double[dom.getCombinations()];
			
			var iexo = dom.getFiteredIndexIterator(endo_vars, new int[endo_vars.length]);
			var dataiter = factors.get(e).iterator();
			
			while(iexo.hasNext()) {
				Helper h = dataiter.next();
				
				int exooff = iexo.next();
				
				var iendo = dom.getFiteredIndexIterator(px, new int[px.length]);
				double[] data = h.data();
				int i = 0;
				while(iendo.hasNext()) {
					int endooff = iendo.next();
					fulldata[exooff + endooff] = Math.log(data[i++]);
				}
			}
			
			var f = new BayesianFactor(dom, fulldata, true);
			random.setFactor(e, f);
		}
		
		for (var e : random.getExogenousVars()) {
			var f = randomFactor(random.getDomain(e), e, true, true);
			random.setFactor(e,f);
		}
		return random;
	}
	
	
	
}
