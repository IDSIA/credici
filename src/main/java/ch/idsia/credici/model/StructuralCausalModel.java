package ch.idsia.credici.model;

import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToHalfSpace;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.convert.HalfspaceToVertex;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.Strides;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.ChordalGraphMaxCliqueFinder;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Author:  Rafael Cabañas
 * Date:    04.02.2020
 * <p>
 * A Structural Causal Model is a special type of {@link GenericSparseModel}, composed with {@link BayesianFactor} and
 * constructed on a {@link SparseDirectedAcyclicGraph}. Differs from Bayesian networks on having 2 different
 * kind of variables: exogenous and endogenous
 */
public class StructuralCausalModel extends GenericSparseModel<BayesianFactor, SparseDirectedAcyclicGraph> {


	private String name="";

	/** set of variables that are exogenous. The rest are considered to be endogenous */
	private Set<Integer> exogenousVars = new HashSet<Integer>();



	/**
	 * Create the directed model using the specified network implementation.
	 */
	public StructuralCausalModel() {
		super(new SparseDirectedAcyclicGraph());
	}

	public StructuralCausalModel(String name) {
		super(new SparseDirectedAcyclicGraph());
		this.name=name;
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

		if(endoVarSizes.length != empiricalDAG.getVariables().length)
			throw new IllegalArgumentException("endoVarSizes vector should as long as the number of vertices in the dag");

		Strides dagDomain = new Strides(empiricalDAG.getVariables(), endoVarSizes);

		if(exoVarSizes.length==0){
			exoVarSizes =  IntStream.of(empiricalDAG.getVariables())
					.map(v -> dagDomain.intersection(ArrayUtils.add(empiricalDAG.getParents(v), v)).getCombinations()+1)
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
		for (int v: this.getVariables()){
			int vid = copy.addVariable(v, this.getSize(v), this.isExogenous(v));
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
		if(vid>max) max = vid;
		max++;
		this.cardinalities.put(vid, size);
		network.addVariable(vid, size);
		if(exogenous)
			this.exogenousVars.add(vid);
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
		Set<Integer> endogenousVars = new HashSet<Integer>();

		for(int v : this.getVariables())
			if(!this.exogenousVars.contains(v))
				endogenousVars.add(v)	;
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

	public int[] getEndegenousParents(int... vars){
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
	 * @param v
	 * @return
	 */
	public int[] getEndogenousChildren(int v){
		return ArraysUtil.intersection(
				this.getEndogenousVars(),
				this.getChildren(v)
		);
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
			randomizeExoFactor(u, prob_decimals);
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
	public void fillExogenousWithRandomFactors(int prob_decimals){

		for(int u : this.getExogenousVars()){
				randomizeExoFactor(u, prob_decimals);
		}
	}


	public void randomizeExoFactor(int u, int prob_decimals){
		this.setFactor(u,
				BayesianFactor.random(this.getDomain(u),
						this.getDomain(this.getParents(u)),
						prob_decimals, false)
		);

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

		 */
		public void fillWithRandomEquations(){

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
	public TIntObjectMap[] getRandomFactors(int prob_decimals){

		StructuralCausalModel model = this.copy();

		model.fillWithRandomFactors(prob_decimals);


		TIntObjectMap<BayesianFactor> equations  = new TIntObjectHashMap<>();
		TIntObjectMap<BayesianFactor> empirical  = new TIntObjectHashMap<>();

		for(int v : model.getEndogenousVars()){
			equations.put(v, model.getFactor(v));
			empirical.put(v, model.getProb(v).fixPrecission(5,v));
		}

		return new TIntObjectMap[] {empirical, equations};

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


		if(CausalInfo.of(this).isMarkovian() || (vars.length==1 && this.getEndegenousParents(vars).length==0)) {
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
			pvar = (BayesianFactor) inf.conditionalQuery(vars, this.getEndegenousParents(vars));

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
		return CausalOps.intervention(this, var, state, true);
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
									Ints.asList(Ints.concat(new int[]{x}, this.getEndegenousParents(x))))))
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
									Ints.asList(Ints.concat(ch_u, this.getEndegenousParents(ch_u))))
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


	public BayesianFactor[] getEmpiricalProbs(){

		BayesianFactor[] empirical = new BayesianFactor[getExogenousVars().length];
		int i = 0;
		for(int u : getExogenousVars()){
			int[] ch_u = getEndogenousChildren(u);
			empirical[i] = getProb(ch_u).fixPrecission(5,ch_u);
			i++;
		}
		return empirical;
	}

	public HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap(boolean fix){

		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();
		int i = 0;
		for(int[] x : this.endoConnectComponents()) {
			BayesianFactor p = getProb(x);
			if(fix) p = p.fixPrecission(5, x);
			empirical.put(Arrays.stream(x).boxed().collect(Collectors.toSet()), p);
			i++;
		}
		return empirical;
	}

	public HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap() {
		return this.getEmpiricalMap(true);
	}
	/*
	public HashMap<Set<Integer>, BayesianFactor> getTianFactorisation(){

		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();
		for(int[] c : this.endoConnectComponents()) {

			int[] pa = IntStream.of(this.getEndegenousParents(c)).filter(v -> !ArrayUtils.contains(c, v)).toArray();

			for(int x : c){


				BayesianFactor f = merge().conditionalProb(new int[]{x}, pa);


			}

			//empirical.put(Arrays.stream(x).boxed().collect(Collectors.toSet()), p);
		}
		return empirical;
	}

*/
	public BayesianNetwork getEmpiricalNet(){

		if(this.getExogenousTreewidth()>1)
			throw new IllegalArgumentException("Non quasi markovian model");

		BayesianNetwork bnet = new BayesianNetwork();

		// Copy the endogenous variables
		IntStream.of(getEndogenousVars()).forEach( v -> bnet.addVariable(v, this.getSize(v)));

		// Set the factors
		for(int v: getEndogenousVars()){
			bnet.addParents(v, getEndegenousParents(v));
			bnet.setFactor(v, getProb(v).fixPrecission(5, v));
		}

		return bnet;
	}



	public StructuralCausalModel findModelWithEmpirical(int prob_decimals, BayesianFactor[] empirical, int[] keepFactors, long maxIterations){


		if(this.getExogenousTreewidth()>1)
			throw new IllegalArgumentException("Non quasi markovian model");

		StructuralCausalModel smodel = this.copy();
		SparseModel cmodel = null;
		for (int i = 0; i < maxIterations; i++) {

			try {
				smodel.fillWithRandomFactors(prob_decimals);
				for(int v:keepFactors)
					smodel.setFactor(v,this.getFactor(v));
				cmodel = smodel.toCredalNetwork(true, empirical);
				break;

			} catch (Exception e) { }
		}


		if(cmodel != null){
			for(int u : smodel.getExogenousVars()){
				smodel.setFactor(u, ((VertexFactor)cmodel.getFactor(u)).sampleVertex());
			}

		}else{
			smodel = null;
		}

		return smodel;
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
		return sample(new TIntIntHashMap(), vars);
	}
	public TIntIntMap sample(TIntIntMap obs, int... vars){



		Iterator it = this.getNetwork().iterator();
		while(it.hasNext()){
			int v = (int) it.next();
			if(!obs.containsKey(v)) {
				BayesianFactor f = this.getFactor(v);
				if(f != null)
					f = f.copy();

				for (int pa : this.getParents(v)) {
					f = f.filter(pa, obs.get(pa));
				}
				obs.putAll(f.sample());
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
		Set edgeSet = this.getNetwork().edgeSet();
		String edgesStr = edgeSet.toString();
		Map<String, String> replacements = Map.of(" ","", ":",",");

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			edgesStr.replace(entry.getKey(), entry.getValue());
		}
		return edgesStr;
	}


	public StructuralCausalModel reverseEdge(int from, int to) {

		int a = from, b = to;

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
			for(int y: this.getEndegenousParents(x)){
				dag.removeLink(y,x);
			}
		}
		return dag;
	}

	public int getExogenousTreewidth(){
		Graph moral = DAGUtil.moral(this.getExogenousDAG());
		return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() - 1;

	}
	public int getTreewidth(){
		Graph moral = DAGUtil.moral(this.getNetwork());
		return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() - 1;

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

	public boolean isCompatible(TIntIntMap[] data, int fixDecimals){
		boolean compatible = false;
		try {
			HashMap empMap = DataUtil.getEmpiricalMap(this, data);

			//System.out.println(empMap);

			if(fixDecimals>0)
				empMap = FactorUtil.fixEmpiricalMap(empMap, fixDecimals);


			ExactCredalBuilder builder =
					ExactCredalBuilder.of(this)
							.setEmpirical(empMap.values())
							.setToVertex()
							.setRaiseNoFeasible(false)
							.build();


			if (builder.getUnfeasibleNodes().size() == 0)
				compatible = true;
			//else
			//	System.out.println(builder.getUnfeasibleNodes());
		}catch(Exception e){
			//if(data[0].keys().length>5)
			//	System.out.println(e);
/*
			HashMap empMap = DataUtil.getEmpiricalMap(this, data);

			if(fixDecimals>0)
				empMap = FactorUtil.fixEmpiricalMap(empMap, fixDecimals);

			System.out.println(empMap);

			ExactCredalBuilder builder =
					ExactCredalBuilder.of(this)
							.setEmpirical(empMap.values())
							.setToVertex()
							.setRaiseNoFeasible(false)
							.build();

*/


		}

		return compatible;
	}

	public List<int[]> endoConnectComponents(){
		return DAGUtil.connectComponents(this.getExogenousDAG())
				.stream()
				.map(c-> IntStream.of(c).filter(i -> this.isEndogenous(i)).toArray())
				.collect(Collectors.toList());
	}

	public List<int[]> exoConnectComponents(){
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



}