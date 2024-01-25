package ch.idsia.credici.inference.multi;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.model.GraphicalModel;

public interface VariableMapping<T extends GraphicalModel<?>> {

	/**
	 * Get the global id of a variable from the specified model.
	 * 
	 * @param source
	 * @param variable_id
	 * @return
	 */
	public int modelToWorld(T model, int variable_id);
	
	/**
	 * Convert a world variable id to the model's one
	 *  
	 * @param model
	 * @param variable_id
	 * @return
	 */
	public int worldToModel(T model, int variable_id);
	
	/**
	 * Add a model to the mapping
	 * 
	 * @param model
	 */
	public void addModel(T model);
}
