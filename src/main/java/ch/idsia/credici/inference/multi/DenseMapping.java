package ch.idsia.credici.inference.multi;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.model.GraphicalModel;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class DenseMapping<T extends GraphicalModel<?>> implements VariableMapping<T> {
	TIntArrayList mapping; 
	Map<T, TIntIntMap> reverse;
	
	public DenseMapping() {
		mapping = new TIntArrayList();
		reverse = new HashMap<T, TIntIntMap>();
	}
	
	@Override
	public void addModel(T model) {
		int[] variables = model.getVariables();
		mapping.ensureCapacity(mapping.size() + variables.length);
		TIntIntMap remapping = new TIntIntHashMap();
		reverse.put(model, remapping);
		
		int vid = mapping.size(); // next id
		for (int variable : variables) {
			remapping.put(variable, vid++);
			mapping.add(variable);
		}
	}

	@Override
	public int worldToModel(T model, int variable_id) {
		// we have a direct mapping between world ids and model ids
		return mapping.get(variable_id);
	}
	
	@Override
	public int modelToWorld(T model, int variable_id) {
		return reverse.get(model).get(variable_id);
	}
}
