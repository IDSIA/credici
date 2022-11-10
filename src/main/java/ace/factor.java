package ace;

import java.util.*;

class Factor {
    // variable for the CPT
    private Variable f_var;
    // parents for the variable
    private List<Variable> f_parents;
    // value combination for parents and child
    private List<String> f_inst;
    // numerical value for the inst
    private Double f_value;
    // dummy numerical value for the inst
    private Double f_dummy_value;
    // associated words in the lmap - this is just a template
    // need to replace with the required value
    // specific prob value was replaced with "@"
    private String f_lmap;
    // whether or not the entry is functional
    private boolean is_fixed;

    // constructor
    public Factor(Variable var, List<Variable> parents, boolean fixed){
	f_var = var;
	f_parents = parents;
	f_inst = new ArrayList<String>();
	is_fixed = fixed;
    }

    // modify the lmap string to adjust for the new f_value
    // if dummy = true, then modify the f_dummy_lmap instead
    private void modify_lmap(boolean dummy){
	
    }

    // display the factor
    public void display(){
	System.out.println(f_inst + ": " + f_value + ' ' + f_dummy_value);
    }
    // setters
    public void set_value(Double val){
	f_value = val;
	modify_lmap(false);
    }
    public void set_dummy_value(Double val){
	f_dummy_value = val;
	modify_lmap(true);
    }
    public void set_lmap(String s){
	f_lmap = s;
    }
    public void add_inst(String s){
	f_inst.add(s);
    }
    // getters
    public Double get_value(){
	return f_value;
    }
    public Double get_dummy_value(){
	return f_dummy_value;
    }
    public String get_lmap(){
	String lmap = f_lmap.replace("@",String.valueOf(f_value));
	return lmap;
    }
    public String get_dummy_lmap(){
	String dummy_lmap = f_lmap.replace("@",String.valueOf(f_dummy_value));
	return dummy_lmap;
    }
    public List<String> get_inst(){
	return f_inst;
    }
    public boolean isfixed(){
	return is_fixed;
    }
}
