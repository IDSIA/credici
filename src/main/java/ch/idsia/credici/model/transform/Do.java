package ch.idsia.credici.model.transform;

import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.change.DomainChange;
import gnu.trove.map.TIntIntMap;

/**
 * Network surgery for do operations.
 * 
 * class is Stateless.
 * 
 * @param <F> type of the factor
 * @param <M> type of the Graphical Model
 * 
 */
public class Do <F extends Factor<F>, M extends GraphicalModel<F>> {

	/**
	 * @stateless
	 * @param model
	 * @param dos
	 * @return
	 */
	public M execute(M model, TIntIntMap dos) {
		M copy = (M) model.copy();
		
		for (int key : dos.keys()) {
			
			// select the correct part of the factors by removing a child
			// via domain changer
			for (int child : copy.getChildren(key)) {
				copy.removeParent(child, key, new DomainChange<F>() {
				
					@Override
					public F remove(F f, int key) {
						return f.filter(key, dos.get(key));
					}
					@Override
					public F add(F factor, int variable) { return null; } // unused
				});
			}
			
			// completely remove the var now.
			copy.removeVariable(key);
		}
		
		return copy;
	}
	
	
	public M execute(M model, int variable, int state) {
		return execute(model, ObservationBuilder.observe(variable, state));
	}
}
