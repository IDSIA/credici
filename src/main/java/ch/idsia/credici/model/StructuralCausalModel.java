package ch.idsia.credici.model;

import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToHalfSpace;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.convert.HalfspaceToVertex;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Domain;
import ch.idsia.crema.model.Strides;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import ch.idsia.credici.collections.FIntIntHashMap;
import ch.idsia.credici.collections.FIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.DiscreteProbabilityCollectionSampler;
import org.apache.commons.rng.sampling.distribution.DirichletSampler;
import org.apache.commons.rng.sampling.distribution.DiscreteSampler;
import org.apache.commons.rng.sampling.distribution.MarsagliaTsangWangDiscreteSampler;
import org.apache.commons.rng.simple.RandomSource;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.ChordalGraphMaxCliqueFinder;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

/**
 * Author:  Rafael Cabañas
 * Date:    04.02.2020
 * <p>
 * A Structural Causal Model is a special type of {@link GenericSparseModel}, composed with {@link BayesianFactor} and
 * constructed on a {@link SparseDirectedAcyclicGraph}. Differs from Bayesian networks on having 2 different
 * kind of variables: exogenous and endogenous
 */
public class StructuralCausalModel extends GenericSparseModel<BayesianFactor, SparseDirectedAcyclicGraph> {


	private UniformRandomProvider randomSource;

	private String name="";

	/** set of variables that are exogenous. The rest are considered to be endogenous */
	private Set<Integer> exogenousVars = new HashSet<Integer>();



	/**
	 * Create the directed model using the specified network implementation.
	 */
	public StructuralCausalModel() {
		super(new SparseDirectedAcyclicGraph());
		initRandom();
	}

	public StructuralCausalModel(String name) {
		super(new SparseDirectedAcyclicGraph());
		this.name=name;
		initRandom();
	}

	/**
	 * Builds a simple SCM from a empirical DAG such that each each endogenous variable (i.e., those from in the DAG)
	 * has a single exogenous variable.
	 * @param empiricalDAG
	 * @param endoVarSizes
	 * @param exoVarSizes
	 */
	public StructuralCausalModel(SparseDirectedAcyclicGraph empiricalDAG, int[] endoVarSizes, int... exoVarSizes){
		super(new SparseDirectedAcyclicGraph());
		initRandom();
	
		if(endoVarSizes.length != empiricalDAG.getVariables().length)
			throw new IllegalArgumentException("endoVarSizes vector should as long as the number of vertices in the dag");

		Strides dagDomain = new Strides(empiricalDAG.getVariables(), endoVarSizes);

		if(exoVarSizes.length==0){
			exoVarSizes =  IntStream.of(empiricalDAG.getVariables())
					.map(v -> dagDomain.sort().intersection(ArrayUtils.add(empiricalDAG.getParents(v), v)).getCombinations()+1)
					.toArray();
		}else if(exoVarSizes.length==1){
			int s = exoVarSizes[0];
			exoVarSizes = IntStream.range(0,empiricalDAG.getVariables().length).map(i-> s).toArray();
		}


		this.addVariables(dagDomain.getSizes());

		for (int i = 0; i<empiricalDAG.getVariables().length; i++){
			int v = empiricalDAG.getVariables()[i];
			this.addParents(v, empiricalDAG.getParents(v));
			if(exoVarSizes.length==1)
				this.addParent(v, this.addVariable(exoVarSizes[0], true));
			else
				this.addParent(v, this.addVariable(exoVarSizes[i], true));
		}
	}
	public void initRandom() {
		randomSource = RandomSource.JDK.create();
	}

	/** 
	 * Initialize random generator with given seed
	 */
	public void initRandom(long seed) {
		randomSource = RandomSource.JDK.create(seed);
	}
	
	public void setRandomSource(UniformRandomProvider randomSource) {
		this.randomSource = randomSource;
	}

	public UniformRandomProvider getRandomSource() {
		return randomSource;
	}

	/**
	 * Constructs a makovian equationless SCM from a bayesian network
	 * @param bnet
	 * @return
	 */
	public static StructuralCausalModel of(BayesianNetwork bnet){
		return ch.idsia.credici.model.builder.CausalBuilder.of(bnet).build();
	}

	/**
	 * Constructs a makovian equationless SCM from a DAG a vector of cardinalities
	 * @param dag
	 * @param endoVarSizes
	 * @return
	 */
	public static StructuralCausalModel of(SparseDirectedAcyclicGraph dag, int[] endoVarSizes){
		return CausalBuilder.of(dag,endoVarSizes).build();
	}



	/**
	 * Create a copy of this model (i.e. dag and factors are copied)
	 * @return
	 */
	public StructuralCausalModel copy(){

		StructuralCausalModel copy = new StructuralCausalModel();
		copy.randomSource = this.randomSource;
		for (int v: this.getVariables()){
			copy.addVariable(v, this.getSize(v), this.isExogenous(v), this.isSpurious(v));
		}

		for (int v : copy.getVariables()){
			copy.addParents(v, this.getParents(v));
			if(this.getFactor(v)!=null)
				copy.setFactor(v, this.getFactor(v).copy());
		}


		return copy;
	}



	/**
	 * Add a new variable to the model. Added variables will be appended to the model and the index of the added
	 * variable will be returned. Adding a variable will always return a value that is greater than any other variable in
	 * the model.
	 *
	 * @param size - int the number of states in the variable
	 * @param exogenous - boolean indicating if the variable is exogenous.
	 * @return int - the label/index/id assigned to the variable
	 */
	public int addVariable(int size, boolean exogenous) {
		int vid = super.addVariable(size);
		if(exogenous)
			this.exogenousVars.add(vid);
		return vid;
	}

	public int addVariable(int vid, int size, boolean exogenous){
		return addVariable(vid, size, exogenous, false);
	}


	private TIntSet spuriousVars = new TIntHashSet();
	
	/**
	 * check whether variable is to be considered an endogenous variable
	 * @param variable
	 * @return
	 */
	public boolean isSpurious(int variable) {
		return spuriousVars.contains(variable);
	}

	/**
	 * Add a variable to the network. Flags can be set to 
	 * @param vid
	 * @param size
	 * @param exogenous
	 * @param spuriousendo true if the variable is not to be considered part of the model
	 * @return
	 */
	public int addVariable(int vid, int size, boolean exogenous, boolean spuriousendo){
		if(vid>max) max = vid;
		max++;
		this.cardinalities.put(vid, size);
		network.addVariable(vid, size);
		if(exogenous)
			this.exogenousVars.add(vid);

		if (spuriousendo) 
			this.spuriousVars.add(vid);

		return vid;
	}


	/**
	 * Removes a variable from the model
	 * @param variable
	 */
	@Override
	public void removeVariable(int variable) {
		super.removeVariable(variable);
		if(exogenousVars.contains(variable))
			exogenousVars.remove(variable);

	}

	/**
	 * Array with IDs of the exogenous variables
	 * @return
	 */
	public int[] getExogenousVars() {
		return Ints.toArray(exogenousVars);
	}

	/**
	 * Allows to know if a variable is exogenous
	 * @param variable
	 * @return
	 */
	public boolean isExogenous(int variable){
		return exogenousVars.contains(variable);
	}

	/**
	 * Allows to know if a variable is endogenous
	 * @param variable
	 * @return
	 */
	public boolean isEndogenous(int variable) {
		return !isExogenous(variable);
	}

	/**
	 * Retruns an array with IDs of the endogenous variables
	 * @return
	 */
	public int[] getEndogenousVars() {
		Set<Integer> endogenousVars = new HashSet<>();

		for(int v : this.getVariables())
			if(!this.exogenousVars.contains(v))
				endogenousVars.add(v);
		return Ints.toArray(endogenousVars);
	}



	/**
	 * Retruns an array with the IDs of parents that are exogenous variables
	 * @param vars
	 * @return
	 */

	public int[] getExogenousParents(int... vars){
		return ArraysUtil.unique(Ints.concat(
				IntStream.of(vars)
						.mapToObj(v -> ArraysUtil.intersection(this.getExogenousVars(), this.getParents(v)))
						.map(v -> IntStream.of(v).filter(x -> !ArraysUtil.contains(x, vars)).toArray())
						.toArray(int[][]::new)));

	}

	/**
	 * Retruns an array with the IDs of parents that are endogenous variables
	 * @param vars
	 * @return
	 */

	public int[] getEndogenousParents(int... vars){
		return ArraysUtil.unique(Ints.concat(
				IntStream.of(vars)
						.mapToObj(v -> ArraysUtil.intersection(this.getEndogenousVars(), this.getParents(v)))
						.map(v -> IntStream.of(v).filter(x -> !ArraysUtil.contains(x, vars)).toArray())
						.toArray(int[][]::new)));

	}

	/**
	 * Retruns an array with the IDs of children that are exogenous variables
	 * @param v
	 * @return
	 */
	public int[] getExogenousChildren(int v){
		return ArraysUtil.intersection(
				this.getExogenousVars(),
				this.getChildren(v)
		);
	}


	/**
	 * Retruns an array with the IDs of children that are endogenous variables
	 * @param vars
	 * @return
	 */
	public int[] getEndogenousChildren(int... vars){

		if (vars.length==1)
			return ArraysUtil.intersection(
					this.getEndogenousVars(),
					this.getChildren(vars[0])
			);

		return ArraysUtil.unique(Ints.concat(
				IntStream.of(vars)
						.mapToObj(v -> ArraysUtil.intersection(this.getEndogenousVars(), this.getChildren(v)))
						.map(v -> IntStream.of(v).filter(x -> !ArraysUtil.contains(x, vars)).toArray())
						.toArray(int[][]::new)));

	}




	/**
	 * Attach to each variable (endogenous or exogenous) a random factor.
	 * Structural Equations are not checked.
	 * @param prob_decimals
	 */
	public void fillWithRandomFactors(int prob_decimals){
		this.fillWithRandomFactors(prob_decimals, false, true);
	}

	/**
	 * Attach to each variable (endogenous or exogenous) a random factor.
	 * @param prob_decimals
	 * @param EqCheck
	 */
	public void fillWithRandomFactors(int prob_decimals, boolean EqCheck, boolean fillEqs){

		for(int u : this.getExogenousVars()){
			randomizeExoFactor(u);
			if(fillEqs) {
				do {
					randomizeEndoChildren(u);
				} while (EqCheck && !areValidSE(u));
			}
		}
	}

	/**
	 * Attach to each variable (endogenous or exogenous) a random factor.
	 * @param prob_decimals
	 */
	public void fillExogenousWithRandomFactors(){

		for(int u : this.getExogenousVars()){
			randomizeExoFactor(u);
		}
	}

	
	/**
	 * Randomize the distribution associated to the specified exogenous variable u. 
	 * This will assing a probability sampled from a dirichlet with alpha == 1.
	 * 
	 * The decimals parameter is unused.
	 * 
	 * @param u the variable whos distribution is to be randomized
	 * @param prob_decimals unused
	 */
	public void randomizeExoFactor(int u){
		Strides left = this.getDomain(u);
		Strides right = this.getDomain(this.getParents(u));

        DirichletSampler x = DirichletSampler.symmetric(randomSource, left.getCombinations(), 1);
		double[][] data = new double[right.getCombinations()][];
		data = x.samples(data.length).toArray(len->new double[len][]);
		
		this.setFactor(u, new BayesianFactor(left.concat(right), Doubles.concat(data), false));
	}

	public void randomizeEndoFactor(int x){
		try {
			Strides pa_x = this.getDomain(this.getParents(x));
			int[] assignments = RandomUtil.sampleUniform(pa_x.getCombinations(), this.getSize(x), true);
			this.setFactor(x,
					BayesianFactor.deterministic(
							this.getDomain(x),
							pa_x,
							assignments)
			);
		}catch (Exception e){
			System.out.println();

		}
	}

	public void randomizeEndoChildren(int u){
		for (int x : getEndogenousChildren(u)) {
			randomizeEndoFactor(x);
		}
	}


	/**
	 * Attach to each variable (endogenous) a random factor.
	 * 
 	 */
	public void fillWithRandomEquations() {
		for (int x : getEndogenousVars()) {
			Strides pa_x = this.getDomain(this.getParents(x));
			int[] assignments = RandomUtil.sampleUniform(pa_x.getCombinations(), this.getSize(x), true);

			this.setFactor(x,
					BayesianFactor.deterministic(
							this.getDomain(x),
							pa_x,
							assignments)
			);
		}
	}



	/**
	 * Get valid random SCM specification (i.e., empirical probabilities + equations)
	 * @param prob_decimals
	 * @return
	 */
	public TIntObjectMap[] getRandomFactors(int prob_decimals) {

		StructuralCausalModel model = this.copy();

		model.fillWithRandomFactors(prob_decimals);


		TIntObjectMap<BayesianFactor> equations  = new FIntObjectHashMap<>();
		TIntObjectMap<BayesianFactor> empirical  = new FIntObjectHashMap<>();

		for(int v : model.getEndogenousVars()){
			equations.put(v, model.getFactor(v));
			empirical.put(v, model.getProb(v).fixPrecission(5,v));
		}

		return new TIntObjectMap[] {empirical, equations};

	}

	public boolean isMarkovian() {
		return IntStream.of(getExogenousVars())
                .allMatch(u -> this.getChildren(u).length <= 1);
	}

	public boolean isQuasiMarkovian() {
		return IntStream.of(getEndogenousVars())
			.allMatch(x -> getExogenousParents(x).length == 1);
	}

	
	/**
	 * Gets the empirical probability of a endogenous variable by marginalizing out
	 * all its exogenous parents. In case of more than one input variable, returns
	 * the combination of the probability of each variable.
	 * @param vars
	 * @return
	 */
	public BayesianFactor getProb(int... vars) {

		BayesianFactor pvar = null;


		if(CausalInfo.of(this).isMarkovian() || (vars.length==1 && this.getEndogenousParents(vars).length==0)) {
			for (int var : vars) {
				BayesianFactor p = this.getFactor(var);
				if (pvar == null) pvar = p;
				else pvar = pvar.combine(p);
			}

			for (int u : this.getExogenousParents(vars)) {
				pvar = pvar.combine(this.getFactor(u));
			}

			for (int u : this.getExogenousParents(vars)) {
				pvar = pvar.marginalize(u);
			}
		}else{
			VariableElimination inf = new FactorVariableElimination(new MinFillOrdering().apply(this));
			inf.setFactors(this.getFactors());
			pvar = (BayesianFactor) inf.conditionalQuery(vars, this.getEndogenousParents(vars));

		}

		return pvar;
	}

	public BayesianFactor conditionalProb(int[] left, int... right){
		VariableElimination inf = new FactorVariableElimination(new MinFillOrdering().apply(this));
		inf.setFactors(this.getFactors());
		return (BayesianFactor) inf.conditionalQuery(left, right);
	}

	public BayesianFactor conditionalProb(int left, int... right){
		return this.conditionalProb(new int[]{left}, right);
	}


	/**
	 * Returns a new SCM with the do operation done over a given variable.
	 * @param var - target variable.
	 * @param state - state to fix.
	 * @return
	 */
	public StructuralCausalModel intervention(int var, int state){
		return ch.idsia.credici.model.tools.CausalOps.intervention(this, var, state, true);
	}

	public StructuralCausalModel intervention(int var, int state, boolean removeDisconnected){
		return ch.idsia.credici.model.tools.CausalOps.intervention(this, var, state, removeDisconnected);
	}

	public StructuralCausalModel intervention(TIntIntMap obs) {
		StructuralCausalModel out = this.copy();
		for(int v : obs.keys())
			out = out.intervention(v, obs.get(v));
		return out;
	}
	public StructuralCausalModel intervention(TIntIntMap obs, boolean removeDisconnected) {
		StructuralCausalModel out = this.copy();
		for(int v : obs.keys())
			out = out.intervention(v, obs.get(v), removeDisconnected);
		return out;
	}

	/**
	 * Prints a summary of the SCM
	 */
	public void printSummary(){

		for(int x : this.getEndogenousVars()){

			System.out.println("\nEndogenous var "+x+" with "+ Arrays.toString(this.getSizes(x))+" states");
			System.out.println("Exogenous parents: "+Arrays.toString(this.getSizes(this.getExogenousParents(x)))+" states");

			try {
				BayesianFactor p = this.getProb(x).fixPrecission(5, x).reorderDomain(x);
				System.out.println(p + " = " + Arrays.toString(p.getData()));
			}catch (Exception e){}

			BayesianFactor f = this.getFactor(x).reorderDomain(this.getExogenousParents(x));




			double[][] fdata = ArraysUtil.reshape2d(f.getData(), f.getDomain().getCombinations()/f.getDomain().getCardinality(this.getExogenousParents(x)[0]));
			System.out.println(f+" = ");
			Stream.of(fdata).forEach(d -> System.out.println("\t"+Arrays.toString(d)));

//			System.out.println(f+" = "+Arrays.toString(f.getData()));

		}

	}


	/**
	 * Converts the current SCM into an equivalent credal network consistent
	 * with the empirical probabilities (of the endogenous variables).
	 * This is the simple case where each endogenous variable has a single
	 * and non-shared exogenous parent.
	 * @param empiricalProbs - for each exogenous variable U, the empirical probability of the children given the endogenous parents.
	 * @return
	 */
	@Deprecated
	public SparseModel toVertexSimple(BayesianFactor... empiricalProbs){

		// Copy the structure of the this
		SparseModel cmodel = new SparseModel();
		cmodel.addVariables(this.getSizes(this.getVariables()));
		for (int v : cmodel.getVariables()){
			cmodel.addParents(v, this.getParents(v));
		}


		// Set the credal sets for the endogenous variables X
		for(int v: this.getEndogenousVars()) {
			VertexFactor kv = new BayesianToVertex().apply(this.getFactor(v), v);
			cmodel.setFactor(v, kv);
		}

		// Get the credal sets for the exogenous variables U
		for(int v: this.getExogenousVars()) {
			System.out.println("Calculating credal set for "+v);
			double [] vector = this.getFactor(this.getChildren(v)[0]).getData();


			double[][] coeff = ArraysUtil.transpose(ArraysUtil.reshape2d(
					this.getFactor(this.getChildren(v)[0]).getData(), this.getSizes(v)
			));

			int x = this.getChildren(v)[0];
			BayesianFactor pv = (BayesianFactor) Stream.of(empiricalProbs).filter(f ->
					ImmutableSet.copyOf(Ints.asList(f.getDomain().getVariables()))
							.equals(ImmutableSet.copyOf(
									Ints.asList(Ints.concat(new int[]{x}, this.getEndogenousParents(x))))))
					.toArray()[0];

			double[] vals = pv.getData();

			SeparateHalfspaceFactor constFactor = new SeparateHalfspaceFactor(cmodel.getDomain(v), coeff, vals);
			VertexFactor vertexFactor = new VertexFactor(constFactor);
			cmodel.setFactor(v, vertexFactor);
		}

		return cmodel;
	}

	/**
	 * Converts the current SCM into an equivalent credal network consistent
	 * with the empirical probabilities (of the endogenous variables).
	 * In this case exogenous parentes might have more than one endogenous child.
	 * Resulting factors are of class VertexFactor.
	 * @param empiricalProbs - for each exogenous variable U, the empirical probability of the children given the endogenous parents.
	 * @return
	 */
	@Deprecated
	public SparseModel toCredalNetwork(BayesianFactor... empiricalProbs){
		return this.toCredalNetwork(true, empiricalProbs);
	}


	/**
	 * Converts the current SCM into an equivalent credal network consistent
	 * with the empirical probabilities (of the endogenous variables).
	 * In this case exogenous parentes might have more than one endogenous child.
	 * @param vertex - flag indicating if the resulting factors are of class VertexFactor (true) or SeparateHalfspaceFactor (false)
	 * @param empiricalProbs - for each exogenous variable U, the empirical probability of the children given the endogenous parents.
	 * @return
	 */
	@Deprecated
	public SparseModel toCredalNetwork(boolean vertex, BayesianFactor... empiricalProbs){


		// Copy the structure of the this
		SparseModel cmodel = new SparseModel();
		cmodel.addVariables(this.getSizes(this.getVariables()));
		for (int v : cmodel.getVariables()){
			cmodel.addParents(v, this.getParents(v));
		}

		// Set the credal sets for the endogenous variables X (structural eqs.)
		for(int v: this.getEndogenousVars()) {

			// Variable on the left should be the first
			BayesianFactor cpt_v =this.getFactor(v).reorderDomain(v);

			if(vertex)
				cmodel.setFactor(v, new BayesianToVertex().apply(cpt_v, v));
			else
				cmodel.setFactor(v, new BayesianToHalfSpace().apply(cpt_v, v));
		}

		// Get the credal sets for the exogenous variables U
		for(int u: this.getExogenousVars()) {

			double [] vector = this.getFactor(this.getChildren(u)[0]).getData();
			int[] children = this.getChildren(u);

			// Get the coefficients by combining all the EQs of the children
			double[][] coeff = this.getCoeff(u);

			// Get the P(ch(U)|endogenous_pa(ch(U)))
			int[] ch_u = this.getChildren(u);

			BayesianFactor pv = (BayesianFactor) Stream.of(empiricalProbs).filter(f ->
					ImmutableSet.copyOf(Ints.asList(f.getDomain().getVariables()))
							.equals(ImmutableSet.copyOf(
									Ints.asList(Ints.concat(ch_u, this.getEndogenousParents(ch_u))))
							))
					.toArray()[0];

			double[] vals = pv.getData();


			SeparateHalfspaceFactor constFactor = new SeparateHalfspaceFactor(cmodel.getDomain(u), coeff, vals);

			if(constFactor==null)
				throw new NoFeasibleSolutionException();


			if(vertex){
				VertexFactor fu = new HalfspaceToVertex().apply(constFactor);
				if(fu.getData()[0]==null)
					throw new NoFeasibleSolutionException();
				cmodel.setFactor(u, fu);
			}else{
				cmodel.setFactor(u, constFactor);
			}


		}

		return cmodel;
	}

	public SparseModel toHCredal(BayesianFactor... empiricalProbs){
		return ExactCredalBuilder.of(this)
				.setEmpirical(empiricalProbs)
				.setNonnegative(false)
				.setToHalfSpace()
				.build().getModel();
	}

	public SparseModel toHCredal(Collection empiricalProbs){
		return ExactCredalBuilder.of(this)
				.setEmpirical(empiricalProbs)
				.setNonnegative(false)
				.setToHalfSpace()
				.setKeepBayesian()
				.build().getModel();
	}


	public SparseModel toVCredal(BayesianFactor... empiricalProbs){
		return ExactCredalBuilder.of(this)
				.setEmpirical(empiricalProbs)
				.setToVertex()
				.build().getModel();
	}

	public SparseModel toVCredal(Collection empiricalProbs){
		return ExactCredalBuilder.of(this)
				.setEmpirical(empiricalProbs)
				.setToVertex()
				.build().getModel();
	}


	public BayesianNetwork toBnet(){
		BayesianNetwork bnet = new BayesianNetwork();
		IntStream.of(getVariables()).forEach(v -> bnet.addVariable(v, getSize(v)));
		IntStream.of(getVariables()).forEach(v -> {
			bnet.addParents(v, this.getParents(v));
			bnet.setFactor(v, this.getFactor(v).copy());
		});
		return bnet;
	}

	/**
	 * Transforms the structural equations associated to the childrens of a given U into a coefficient matrix.
	 * @param u
	 * @return
	 */
	public double[][] getCoeff(int u){

		if(!this.isExogenous(u))
			throw new IllegalArgumentException("Variable "+u+" is not exogenous");

		int[] children = this.getEndogenousChildren(u);

		// Get the coefficients by combining all the EQs of the children
		double[][] coeff = ArraysUtil.transpose(ArraysUtil.reshape2d(
				IntStream.of(children).mapToObj(i-> this.getFactor(i)).reduce((f1,f2) -> f1.combine(f2)).get()
						.getData(), this.getSizes(u)
		));


		return coeff;
	}


	private boolean areValidSE(int u){
		double[][] coeff = getCoeff(u);
		for(double[] c : coeff){
			if(DoubleStream.of(c).reduce(0, (a, b) -> a + b) <= 0)
				return false;
		}

		int ch_comb = this.getDomain(getEndogenousChildren(u)).getCombinations();

		for(int i=0; i<coeff.length; i=i+ch_comb) {
			for (double[] c : ArraysUtil.transpose(Arrays.copyOfRange(coeff, i, i+ch_comb))) {
				if (DoubleStream.of(c).reduce(0, (a, b) -> a + b) != 1)
					return false;
			}
		}

		return true;
	}

	public boolean areValidSE(){
		for(int u : this.getExogenousVars())
			if(!this.areValidSE(u))
				return false;
		return true;
	}


	/**
	 * Assuming that this SCM is a counterfactual model, this object
	 * associates the variables across the worlds.
	 * @return
	 */
	public WorldMapping getMap() {
		return WorldMapping.getMap(this);
	}


	/**
	 * Merge the current SCM with other equivalent ones to create a counterfactual model.
	 * @param models
	 * @return
	 */
	public StructuralCausalModel merge(StructuralCausalModel... models) {
		return CausalOps.merge(this, models);
	}

	/**
	 * String summarizing this SCM.
	 * @return
	 */
	public String toString(){

		StringBuilder str = new StringBuilder("");

		if(name != "") str.append(name+":");

		if(this.getMap()==null)
			str.append("\n"+this.getFactors());
		else{
			for(int w: this.getMap().getWorlds()){

				if (w == WorldMapping.ALL) {
					str.append("\nGlobal factors: ");
				} else {
					str.append("\n"+"World " + w + " factors: ");
				}

				for(int v: this.getMap().getVariablesIn(w)) {
					if (w == WorldMapping.ALL || this.getMap().getWorld(v) != WorldMapping.ALL)
						str.append("\n"+this.getFactor(v));
				}


			}
		}
		

		return str.toString()+"\n";
	}

	/**
	 * Returns the nane of the SCM.
	 * @return
	 */
	public String getName() {
		return name;
	}


	public List getEmpiricalDomains(int exoVar){

		if(!this.isExogenous(exoVar))
			throw new IllegalArgumentException(exoVar+" is not exogenous");

		List domains = new ArrayList();
		int[] ch = DAGUtil.getTopologicalOrder(this.getNetwork(), this.getEndogenousChildren(exoVar));

		for(int k = 0; k<ch.length; k++) {
			HashMap dom = new HashMap();
			int x = ch[k];
			int[] previous = IntStream.range(0, k).map(i -> ch[i]).toArray();
			int[] right = ArraysUtil.unionSet(previous, this.getEndogenousParents(ArraysUtil.append(previous, x)));
			dom.put("left", x);
			dom.put("right", right);
			domains.add(dom);
		}

		return domains;
	}


	public BayesianFactor[] getEmpiricalProbs(){
		return this.getEmpiricalMap(true).values().toArray(BayesianFactor[]::new);
	}

	public HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap(boolean fix){

		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();


		for(int u: this.getExogenousVars()) {

			BayesianFactor fu = null;

			int[] chU = this.getEndogenousChildren(u);
			if(chU.length==1) {
				fu = BayesianFactor.combineAll(this.getFactors(ArraysUtil.append(chU, u))).marginalize(u);
			}else {

				for (Object dom_ : this.getEmpiricalDomains(u)) {
					HashMap dom = (HashMap) dom_;
					int left = (int) dom.get("left");
					int[] right = (int[]) dom.get("right");
					StructuralCausalModel infModel = new RemoveBarren().execute(this, ArraysUtil.append(right, left));
					VariableElimination inf = new FactorVariableElimination(infModel.getVariables());
					inf.setFactors(infModel.getFactors());

					BayesianFactor f = null;
					f = (BayesianFactor) inf.conditionalQuery(left, right);

					if (fu == null)
						fu = f;
					else
						fu = fu.combine(f);
				}
			}

			if(fix)
				fu = fu.fixPrecission(FactorUtil.DEFAULT_DECIMALS, this.getEndogenousChildren(u));
			empirical.put(Arrays.stream(this.getEndogenousChildren(u)).boxed().collect(Collectors.toSet()), fu);
		}

		return empirical;
	}

	public HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap() {
		return this.getEmpiricalMap(true);
	}

	@Deprecated
	public BayesianNetwork getEmpiricalNet(){

		if(this.getExogenousTreewidth()>1)
			throw new IllegalArgumentException("Non quasi markovian model");

		BayesianNetwork bnet = new BayesianNetwork();

		// Copy the endogenous variables
		IntStream.of(getEndogenousVars()).forEach( v -> bnet.addVariable(v, this.getSize(v)));

		// Set the factors
		for(int v: getEndogenousVars()){
			bnet.addParents(v, getEndogenousParents(v));
			bnet.setFactor(v, getProb(v).fixPrecission(5, v));
		}
		return bnet;
	}


	/// Based on Tian&Pearl 2003

	/**
	 * 
	 * @param exoVars
	 * @return
	 */
	public List<Conditional> getCFactorsSplittedDomains(int...exoVars){
		if(this.exoConnectComponents().stream().filter(c -> ArraysUtil.equals(exoVars, c, true, true)).count() != 1)
			throw new IllegalArgumentException("Wrong exogenous variables.");

		List<Conditional> domains = new ArrayList<>();

		int[] chU = this.getEndogenousChildren(exoVars);

		if(chU.length>0) {
			chU = DAGUtil.getTopologicalOrder(this.getNetwork(), chU);

			for (int k = 0; k < chU.length; k++) {
				//HashMap dom = new HashMap();
				int x = chU[k];
				int[] finalChU = chU;
				int[] previous = IntStream.range(0, k).map(i -> finalChU[i]).toArray();
				int[] right = ArraysUtil.unionSet(previous, this.getEndogenousParents(ArraysUtil.append(previous, x)));
				//dom.put("left", x);
				//dom.put("right", right);
				//System.out.println(x+"|"+ Arrays.toString(right));
				domains.add(new Conditional(x, right));
			}
		}
		return domains;
	}

	public List<BayesianFactor> getCFactorsSplitted(int... exoVars) {

		List factors = new ArrayList();

		for(Conditional dom : this.getCFactorsSplittedDomains(exoVars)){
			int left = dom.getLeft();//(int) dom.get("left");
			int[] right = dom.getRight();//(int[]) dom.get("right");
			StructuralCausalModel infModel = new RemoveBarren().execute(this, ArraysUtil.append(right, left));
			VariableElimination inf = new FactorVariableElimination(infModel.getVariables());
			inf.setFactors(infModel.getFactors());
			BayesianFactor f = null;
			f = (BayesianFactor) inf.conditionalQuery(left, right);
			factors.add(f);
		}
		return factors;
	}


	public TIntObjectMap<BayesianFactor> getCFactorsSplittedMap() {

		TIntObjectMap factors = new FIntObjectHashMap();

		for(Conditional dom : this.getAllCFactorsSplittedDomains()){
			int left = dom.getLeft();//(int) dom.get("left");
			int[] right = dom.getRight();//(int[]) dom.get("right");
			StructuralCausalModel infModel = new RemoveBarren().execute(this, ArraysUtil.append(right, left));


			VariableElimination inf = new FactorVariableElimination(infModel.getVariables());
			inf.setFactors(infModel.getFactors());

			BayesianFactor f = null;
			if(!isMarkovianCC(left))
				f = (BayesianFactor) inf.conditionalQuery(left, right);
			else{
				int u = getExogenousParents(left)[0];
				f = BayesianFactor.combineAll(this.getFactors(left, u)).marginalize(u);
			}
			//System.out.println(f);
			factors.put(left,f);
		}
		return factors;
	}

	public TIntObjectMap<BayesianFactor> getCFactorsSplittedMap(int... exoVars) {

		TIntObjectMap factors = new FIntObjectHashMap();

		for(Conditional dom : this.getCFactorsSplittedDomains(exoVars)){
			int left = dom.getLeft();//(int) dom.get("left");
			int[] right = dom.getRight();//(int[]) dom.get("right");
			StructuralCausalModel infModel = new RemoveBarren().execute(this, ArraysUtil.append(right, left));
			VariableElimination inf = new FactorVariableElimination(infModel.getVariables());
			inf.setFactors(infModel.getFactors());

			BayesianFactor f = null;
			if(!isMarkovianCC(left))
				f = (BayesianFactor) inf.conditionalQuery(left, right);
			else{
				int u = getExogenousParents(left)[0];
				f = BayesianFactor.combineAll(this.getFactors(left, u)).marginalize(u);
			}
			factors.put(left,f);
		}
		return factors;
	}

	public BayesianFactor getCFactor(int...exoVars){
		return BayesianFactor.combineAll(this.getCFactorsSplitted(exoVars));
	}

	public List<Conditional> getAllCFactorsSplittedDomains() {
		return this.exoConnectComponents().stream().map(this::getCFactorsSplittedDomains).flatMap(List::stream).collect(Collectors.toList());
	}
	public BayesianFactor[] getQDecomposition() {
		return Arrays.stream(this.exoConnectComponents().stream().map(c -> this.getCFactor(c)).toArray()).toArray(BayesianFactor[]::new);
	}

	public TIntIntMap[] samples(int N, int... vars) {
		return IntStream.range(0, N).mapToObj(i -> sample(vars)).toArray(TIntIntMap[]::new);
	}
	public TIntIntMap[] samples(TIntIntMap[] observations, int... vars) {
		return Stream.of(observations).map(obs -> this.sample(obs, vars)).toArray(TIntIntMap[]::new);
	}




	public TIntIntMap[] samples(int N, TIntIntMap obs, int... vars) {
		return IntStream.range(0, N).mapToObj(i -> this.sample(obs, vars)).toArray(TIntIntMap[]::new);
	}

	public TIntIntMap sample(int... vars) {
		return sample(new FIntIntHashMap(), vars);
	}


	private ObservationBuilder sample(BayesianFactor bf) {
		double[] probs = bf.getData();
		List<Integer> states = IntStream.range(0, bf.getDomain().getSizeAt(0)).mapToObj(Integer::valueOf).collect(Collectors.toList());
		var ds = new DiscreteProbabilityCollectionSampler<Integer>(randomSource, states, probs);
		return bf.getDomain().observationOf(ds.sample());
	}
	
	public TIntIntMap sample(TIntIntMap obs, int... vars){
		int[] order = DAGUtil.getTopologicalOrder(this.getNetwork());
		for(int v : order){
			if(!obs.containsKey(v)) {
				BayesianFactor f = this.getFactor(v);

				// if(f != null)
				// 	f = f.copy();

				for (int pa : this.getParents(v)) {
					int spa = obs.get(pa);
					f = f.filter(pa, spa);
				}

				ObservationBuilder s = sample(f);
				obs.putAll(s);
			}
		}

		if(vars.length==0)
			vars = this.getVariables();

		for(int v:obs.keys())
			if(!ArraysUtil.contains(v, vars))
				obs.remove(v);

		return obs;
	}

	public Strides endogenousMarkovBlanket(int v){
		return this.getDomain(ArraysUtil.intersection(
				this.getNetwork().markovBlanket(v),
				this.getEndogenousVars()
		));
	}

	public String edgesToString(){
		Set<DefaultEdge> edgeSet = this.getNetwork().edgeSet();
		String edgesStr = edgeSet.toString();
		Map<String, String> replacements = Map.of(" ","", ":",",");

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			edgesStr = edgesStr.replace(entry.getKey(), entry.getValue());
		}
		return edgesStr;
	}


	public StructuralCausalModel reverseEdge(int from, int to) {

		int a = from;
		int b = to;

		//the edge should exist
		if (!ArraysUtil.contains(a, this.getParents(b)))
			throw new IllegalArgumentException("Wrong edge");

		if (new AllDirectedPaths(this.getNetwork()).getAllPaths(a, b, true, null).size() > 1)
			throw new IllegalArgumentException("There are other directed paths");

		StructuralCausalModel out = this.copy();

		// remove the current edge
		out.removeParent(b, a);

		// parents are now shared
		out.addParents(a, out.getParents(b));
		out.addParents(b, out.getParents(a));

		// add the reversed edge
		out.addParent(a, b);

		// define the new factors

		BayesianFactor fab = BayesianFactor.combineAll(this.getFactors(a, b));
		BayesianFactor fb = fab.marginalize(a);
		BayesianFactor fa_b = fab.divide(fb);

		out.setFactor(a, fa_b);
		out.setFactor(b, fb);
		return out;

	}

	public StructuralCausalModel reverseExoEdges(){
		StructuralCausalModel rmodel = this;
		for(int u : this.getExogenousVars()) {
			int chU[] = this.getEndogenousChildren(u);
			for (int x : DAGUtil.getTopologicalOrder(this.getNetwork(), chU)) {
				rmodel = rmodel.reverseEdge(u, x);
			}
		}
		return rmodel;
	}

	public HashMap<Integer, BayesianFactor> endogenousBlanketProb(){


		FactorVariableElimination inf = new FactorVariableElimination(new MinFillOrdering().apply(this));
		inf.setFactors(this.getFactors());

		HashMap probs = new HashMap();
		for(int u: this.getExogenousVars()){
			int[] blanket = this.endogenousMarkovBlanket(u).getVariables();
			probs.put(u, ((BayesianFactor)inf.conditionalQuery(blanket)));
		}
		return probs;

	}


	public StructuralCausalModel average(StructuralCausalModel model, int... vars) {

		if (vars.length == 0)
			vars = this.getVariables();

		if (!this.edgesToString().equals(model.edgesToString()))
			throw new IllegalArgumentException("Incompatible models");

		StructuralCausalModel out = model.copy();

		for (int v : vars) {
			BayesianFactor f1 = this.getFactor(v);
			BayesianFactor f2 = model.getFactor(v);
			out.setFactor(v, f1.addition(f2).scalarMultiply(0.5));
		}
		return out;
	}


	public StructuralCausalModel average(StructuralCausalModel model, double weightFirst, int... vars) {

		if (vars.length == 0)
			vars = this.getVariables();

		if (!this.edgesToString().equals(model.edgesToString()))
			throw new IllegalArgumentException("Incompatible models");

		StructuralCausalModel out = model.copy();

		for (int v : vars) {
			BayesianFactor f1 = this.getFactor(v).scalarMultiply(weightFirst);
			BayesianFactor f2 = model.getFactor(v).scalarMultiply(1-weightFirst);
			out.setFactor(v, f1.addition(f2));
		}
		return out;
	}


	public SparseDirectedAcyclicGraph getExogenousDAG(){
		SparseDirectedAcyclicGraph dag = this.getNetwork().copy();
		for(int x : this.getEndogenousVars()){
			for(int y: this.getEndogenousParents(x)){
				if(!isSpurious(y))
					dag.removeLink(y,x);
			}
		}
		return dag;
	}

	public SparseDirectedAcyclicGraph getEndogenousDAG(){
		return DAGUtil.getSubDAG(this.getNetwork(), this.getEndogenousVars());
	}


	public int getEndogenousTreewidth(){
		Graph<Integer, DefaultEdge> moral = DAGUtil.moral(this.getEndogenousDAG());
		return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() -1;

	}
	public int getExogenousTreewidth(){
		Graph<Integer, DefaultEdge> moral = DAGUtil.moral(this.getExogenousDAG());
		return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() - 1;

	}
	public int getTreewidth(){
		Graph<Integer, DefaultEdge> moral = DAGUtil.moral(this.getNetwork());
		return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() -1;

	}

	public StructuralCausalModel incrementVarIDs(int increment){
		StructuralCausalModel newModel = new StructuralCausalModel();

		for(int v: this.getVariables()){
			newModel.addVariable(v+increment, this.getSize(v), this.isExogenous(v));
		}

		for(int v: this.getVariables()){
			int vnew = v+increment;
			int[] parentsNew = IntStream.of(this.getParents(v)).map(i -> i+increment).toArray();
			int[] newDomain = IntStream.of(this.getFactor(v).getDomain().getVariables()).map(i -> i+increment).toArray();
			BayesianFactor newFactor = this.getFactor(v).copy().renameDomain(newDomain);

			newModel.addParents(vnew, parentsNew);
			newModel.setFactor(vnew, newFactor);
		}

		return newModel;
	}




	public boolean isCompatible(TIntIntMap[] data) {
		return this.isCompatible(data, FactorUtil.DEFAULT_DECIMALS);
	}

	public static boolean isCompatible(StructuralCausalModel model, TIntIntMap[] data){
		return model.isCompatible(data);
	}

	public boolean isCompatible(TIntIntMap[] data, int fixDecimals) {
		return this.getUncompatibleNodes(data, fixDecimals).isEmpty();
	}

	public boolean isCompatible(TIntIntMap[] data, int[] exoVars, int fixDecimals) {
		return this.getUncompatibleNodes(data, exoVars, fixDecimals).isEmpty();
	}
	public List<Integer> getUncompatibleNodes(TIntIntMap[] data, int fixDecimals){
		HashMap empMap = DataUtil.getEmpiricalMap(this, data);

		if(fixDecimals>0)
			empMap = FactorUtil.fixEmpiricalMap(empMap, fixDecimals);

		ExactCredalBuilder builder =
				ExactCredalBuilder.of(this)
						.setEmpirical(empMap.values())
						.setToVertex()
						.setRaiseNoFeasible(false)
						.build();

		return builder.getUnfeasibleNodes();
	}


	public List<Integer> getUncompatibleNodes(TIntIntMap[] data, int[] exoVars, int fixDecimals){
		HashMap empMap = DataUtil.getEmpiricalMap(this, data);

		if(fixDecimals>0)
			empMap = FactorUtil.fixEmpiricalMap(empMap, fixDecimals);

		ExactCredalBuilder builder =
				ExactCredalBuilder.of(this)
						.setEmpirical(empMap.values())
						.setToVertex()
						.setRaiseNoFeasible(false)
						.build(exoVars);

		return builder.getUnfeasibleNodes().stream().filter(u -> ArraysUtil.contains(u, exoVars)).collect(Collectors.toList());
	}

	public List<int[]> endoConnectComponents() {
		return DAGUtil.connectComponents(this.getExogenousDAG())
				.stream()
				.map(c-> IntStream.of(c).filter(i -> this.isEndogenous(i)).toArray())
				.collect(Collectors.toList());
	}

	public List<int[]> exoConnectComponents() {
		return DAGUtil.connectComponents(this.getExogenousDAG())
				.stream()
				.map(c-> IntStream.of(c).filter(i -> this.isExogenous(i)).toArray())
				.collect(Collectors.toList());
	}

	public int maxExoCC(){
		return this.exoConnectComponents().stream().mapToInt(c -> ((int[])c).length).max().getAsInt();
	}
	public int maxEndoCC(){
		return this.endoConnectComponents().stream().mapToInt(c -> ((int[])c).length).max().getAsInt();
	}

	public boolean isMarkovianCC(int endoVar){
		if(!this.isEndogenous(endoVar))
			throw new IllegalArgumentException("Wrong type of variables");

		if(this.getExogenousParents(endoVar).length>1)
			return false;
		int exoVar = this.getExogenousParents(endoVar)[0];

		if(this.getEndogenousChildren(exoVar).length>1)
			return false;


		return true;

	}

	public boolean isConservative(){
		for(int u: this.getExogenousVars()) {
			int[] X = this.getChildren(u);
			int[] Y = this.getEndogenousParents(X);
			BayesianFactor fjoint = BayesianFactor.combineAll(this.getFactors(X));
			if(!EquationOps.isConservative(fjoint, u, X))
				return false;
		}
		return true;
	}

	public boolean summaryConservative(){

		boolean conservative = true;
		for(int u: this.getExogenousVars()) {
			int[] X = this.getChildren(u);
			int[] Y = this.getEndogenousParents(X);
			BayesianFactor fjoint = BayesianFactor.combineAll(this.getFactors(X));
			System.out.print("f_"+Arrays.toString(X)+" ");
			List missingX = EquationOps.getMissingToConservative(fjoint, u, X);

			if(missingX.isEmpty())
				System.out.println("is conservative");
			else {
				System.out.println("is not conservative. Missing configurations: " + missingX.stream().map(x -> Arrays.toString((int[]) x)).collect(Collectors.joining(",")));
				conservative = false;
			}
		}
		return conservative;
	}


	public StructuralCausalModel dropExoState(int exoVar, int... toRemove){

		if(!this.isExogenous(exoVar))
			throw new IllegalArgumentException("Non exogenous variable");

		StructuralCausalModel newModel = this.copy();

		for (int s : (int[]) toRemove)
			newModel.removeState(exoVar, s);

		// Variables of affected factors
		int[] vars = ArraysUtil.append(this.getChildren(exoVar), exoVar);
		for(int v:vars){
			BayesianFactor f = this.getFactor(v);
			for (int s : (int[]) toRemove)
				f = FactorUtil.dropState(f, exoVar, s);
			newModel.setFactor(v, f);
		}

		return newModel;
	}

	public double ratioLogLikelihood(TIntIntMap[] data){
		return Probability.ratioLogLikelihood(this.getCFactorsSplittedMap(), DataUtil.getCFactorsSplittedMap(this, data),  1);
	}

	public double ratioLogLikelihood(TIntIntMap[] data, int... exoVars){
		return Probability.ratioLogLikelihood(this.getCFactorsSplittedMap(exoVars), DataUtil.getCFactorsSplittedMap(this, data, exoVars),  1);
	}

	public double logLikelihood(TIntIntMap[] data){
		return Probability.logLikelihood(this.getCFactorsSplittedMap(),
				DataUtil.getCFactorsSplittedMap(this, data), data.length);
	}

	public void removeDisconnected(){
		for(int v : this.getDisconnected())
			this.removeVariable(v);
	}

	public int[] intervenedVars(){
		return IntStream.of(this.getEndogenousVars()).filter(x -> this.getExogenousParents(x).length==0).toArray();
	}

	public void fixIntervened(){
		this.removeDisconnected();
		for(int v : intervenedVars()){
			int card = this.getDomain(v).getCombinations();
			int u = this.addVariable(card, true);
			BayesianFactor pu = this.getFactor(v).renameDomain(u);
			this.addParent(v,u);
			BayesianFactor fv = BayesianFactor.deterministic(this.getDomain(v), this.getDomain(u), IntStream.range(0,card).toArray());
			this.setFactor(v,fv);
			this.setFactor(u,pu);
		}
	}
	public StructuralCausalModel getWithFixedIntervened(){
		StructuralCausalModel out = this.copy();
		out.fixIntervened();
		return out;
	}

	public StructuralCausalModel subModel(int... variables){
		StructuralCausalModel subModel = this.copy();
		for(int v : subModel.getVariables())
			if(!ArraysUtil.contains(v,variables)) {
				subModel.removeVariable(v);
			}

		return subModel;
	}



	/**
	 * current implementation is never true ????
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StructuralCausalModel) {
			var other = (StructuralCausalModel)obj;
			if (!name.equals(other.name)) return false;

			//// vars tested by cardinalities as well
			// TIntSet thisVars = new FIntHashSet(getVariables());
			// TIntSet otherVars = new FIntHashSet(other.getVariables());
			// if (!thisVars.equals(otherVars)) return false;

			if (!other.cardinalities.equals(cardinalities)) return false;

			for (int i : getVariables()) {
				var thisFactor = factors.get(i);
				var otherFactor = other.factors.get(i);
				
				if (!Arrays.equals(thisFactor.getDomain().getVariables(), otherFactor.getDomain().getVariables())) return false;
				if (!Arrays.equals(thisFactor.getInteralData(), otherFactor.getInteralData())) return false;
			}
			return true;
		}

		return super.equals(obj);
	}
}