package ace;

import java.util.*;

class Variable {
	// original name from the BN.net file
	private String v_name;
	// assigned new id for the variable if needed
	private String v_id;
	// name of the parents in the order of BN file
	private List<Variable> v_parents;
	// values for the variable
	private List<String> v_values;
	// whether or not the variable is exogenous
	private boolean v_exogenous;
	// whether or not the CPT can be varied
	private boolean v_isVariable;

	// map each CPT index to a factor
	private Map<Integer, Factor> v_factors;
	// chunk size for each variable
	private List<Integer> chunk_size;

	public Variable() {
		v_parents = new ArrayList<Variable>();
		v_values = new ArrayList<String>();
		v_factors = new HashMap<Integer, Factor>();
		chunk_size = new ArrayList<Integer>();
		v_isVariable = false;
	}

	// setters
	public void set_name(String name) {
		v_name = name;
	}

	public void add_value(String value) {
		v_values.add(value);
	}

	public void add_parent(Variable parent) {
		v_parents.add(parent);
	}

	public void modify_CPT(Double CPT_entry, int index, boolean dummy) {
		// if chunk size is not initialized yet, we do so first
		if (chunk_size.size() == 0)
			init_chunk_size();
		// by default, if index = -1 then it means add a new one
		if (index == -1) {
			index = v_factors.size();
		}
		// index uniquely identify the factor
		// if the factor is already in the list, then we modify its value
		if (v_factors.containsKey(index))
			if (dummy)
				v_factors.get(index).set_dummy_value(CPT_entry);
			else
				v_factors.get(index).set_value(CPT_entry);
		else {
			Factor new_fac = make_factors(CPT_entry, index);
			if (dummy)
				new_fac.set_dummy_value(CPT_entry);
			else
				new_fac.set_value(CPT_entry);
			v_factors.put(index, new_fac);
		}
	}

	// create factors for the variable
	private Factor make_factors(Double CPT_entry, int index) {
		// we next traverse the parents and create strings and value
		int remain = index;
		Factor f = new Factor(this, v_parents, (!v_isVariable));
		for (int j = chunk_size.size() - 1; j >= 0; j--) {
			int chunk = chunk_size.get(j);
			int ind = remain / chunk;
			remain %= chunk;
			Variable par = v_parents.get(v_parents.size() - 1 - j);
			f.add_inst(par.get_values().get(ind));
		}
		f.add_inst(v_values.get(remain));
		return f;
	}

	// helper function for add CPT
	// initialize the chunk size if not done yet
	private void init_chunk_size() {
		// we first get the chunk size for each variable
		// total size initialized to be the size of child
		int tot_size = v_values.size();
		chunk_size.add(tot_size);
		// for each parent in reverse order, multiply by its size
		for (int i = v_parents.size() - 1; i >= 0; i--) {
			Variable var = v_parents.get(i);
			int card = var.get_values().size();
			tot_size *= card;
			chunk_size.add(tot_size);
		}
		// the last size is not needed - it's determined by others
		chunk_size.remove(chunk_size.size() - 1);
	}

	public void set_isVariable() {
		v_isVariable = true;
	}

	// getters
	public String get_name() {
		return v_name;
	}

	public List<String> get_values() {
		return v_values;
	}

	public List<Variable> get_parents() {
		return v_parents;
	}

	public List<Double> get_CPT() {
		List<Double> CPT = new ArrayList<Double>();
		for (int i = 0; i < v_factors.size(); i++) {
			Factor f = v_factors.get(i);
			CPT.add(f.get_value());
		}
		return CPT;
	}

	public boolean isVariable() {
		return v_isVariable;
	}

	public boolean isExogenous() {
		return (v_parents.size() == 0);
	}

	public List<Double> get_dummy_CPT() {
		List<Double> dummy_CPT = new ArrayList<Double>();
		for (int i = 0; i < v_factors.size(); i++) {
			Factor f = v_factors.get(i);
			dummy_CPT.add(f.get_dummy_value());
		}
		return dummy_CPT;
	}

	public Map<Integer, Factor> get_factors() {
		return v_factors;
	}
}
