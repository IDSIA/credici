package ch.idsia.credici.model.transform;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.change.DomainChange;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Network surgery for do operations.
 * 
 * class is Stateless.
 * 
 * @param <F> type of the factor
 * @param <M> type of the Graphical Model
 * 
 */
public class Do <M extends GenericSparseModel<BayesianFactor, ? extends SparseDirectedAcyclicGraph>> {

	private TIntIntMap newEvidence;
	
	private boolean removingVariables = true;
	
	public Do() {
		// TODO Auto-generated constructor stub
	}
	
	public Do(boolean removingVariables) {
		this.removingVariables = removingVariables;
	}
	
	
	public void setRemoveVariable(boolean removeVariable) {
		this.removingVariables = removeVariable;
	}
	
	public boolean isRemovingVariables() {
		return removingVariables;
	}
	
	
	public TIntIntMap getNewEvidence() {
		return newEvidence;
	}
	
	/**
	 *
	 * @param model
	 * @param dos
	 * @return
	 */
	public M execute(M model, TIntIntMap dos) {
		M copy = (M) model.copy();

		// no new evidence
		newEvidence = new TIntIntHashMap();
		
		for (int key : dos.keys()) {
			
			// select the correct part of the factors by removing a child
			// via domain changer
			if (removingVariables) {
				// merge evidence of the do into the CPT
				for (int child : copy.getChildren(key)) {
					copy.removeParent(child, key, new DomainChange<BayesianFactor>() {
					
						@Override
						public BayesianFactor remove(BayesianFactor f, int key) {
							return f.filter(key, dos.get(key));
						}
						@Override
						public BayesianFactor add(BayesianFactor factor, int variable) { return null; } // unused
					});
				}
				
				// completely remove the var now.
				copy.removeVariable(key);
				
			} else {
				for (int parent : copy.getParents(key)) {
					copy.removeParent(key, parent);
				}
				
				double[] cpt = new double[copy.getSize(key)];
				cpt[dos.get(key)] = 1;
				copy.setFactor(key, new BayesianFactor(copy.getDomain(key), cpt, false));
				
				newEvidence.putAll(dos);
			}
		}
		
		return copy;
	}
	
	
	public M execute(M model, int variable, int state) {
		return execute(model, ObservationBuilder.observe(variable, state));
	}
}
