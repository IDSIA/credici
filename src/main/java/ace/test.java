package ace;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Math;

class Test {
    // this function will evoke a python script to create a rbn .net file
    private static void test_rbn(){

	double eps = 1e-8;
	
	// we first run the rbn python script to create a rbn net files
	String command1 = "rm -rf ../examples/rbn_exp";
	String command2 = "python3 ../examples/rbn/rbn.py";
	String command3 = "cp -r ../examples/rbn ../examples/rbn_exp";
	try {
	    Process p = Runtime.getRuntime().exec(new String[]{"bash","-c",command1+" && " +command2+" && "+command3});
	    try{
		p.waitFor();
	    } catch (Exception e){
		e.printStackTrace();
	    }
	}catch (IOException e){
	    e.printStackTrace();
	}

	String dire = "../examples/rbn_exp";
	String dire_stats = dire + "/stats.txt";
	String[] vars = {};
	// extract the variables
	FileInputStream stream = null;
	try {
	    stream = new FileInputStream(dire_stats);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	String strLine;
	try {
	    while ((strLine = reader.readLine()) != null){
		vars = strLine.trim().split("\\s+");		
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}

	// create an ace_ext object
	String dire_net = dire + "/rbn.net";
	AceExt myObj = new AceExt(dire_net, vars, "src/resources/ace");

	// prepare for the evidence and query nodes
	List<String>query_nodes = Arrays.asList(vars);
	Map<String,String>evidence = new HashMap<String,String>();
	String dire_inst = dire + "/inst.txt";
	stream = null;
	try {
	    stream = new FileInputStream(dire_inst);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		evidence.put(words[0],words[1]);
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}


	// run marginal query and compare with the answer
	Map<String,List<Double>> ans;       
	ans = myObj.evaluate(query_nodes,evidence);
	// compare here
	String dire_rbn_ans = dire + "/rbn.ans";
	Map<String,List<Double>> label = new HashMap<String,List<Double>>();
	stream = null;
	try {
	    stream = new FileInputStream(dire_rbn_ans);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		List<Double>probs = new ArrayList<Double>();
		for (int i = 1; i < words.length; i ++){
		    probs.add(Double.valueOf(words[i]));
		}		
		label.put(words[0],probs);		
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}
	// check if they match	
	if (ans.size() != label.size()){
	    System.out.println("  !! rbn marginal length differs !!");
	    System.exit(1);
	}
	for (Map.Entry<String,List<Double>> entry : ans.entrySet()) {
	    String var_name = entry.getKey();
	    List<Double> marginal = entry.getValue();
	    List<Double> lab = label.get(var_name);
	    if (marginal.size() != lab.size()){
		System.out.println("  !! rbn probability length differs !!");
		System.exit(1);
	    }
	    for (int i = 0; i < marginal.size(); i ++){
		if (Math.abs(marginal.get(i)-lab.get(i)) > eps){
		    System.out.println("  !! rbn marginal doesn't match !!");
		    System.out.println(marginal);
		    System.out.println(lab);
		    System.exit(1);
		}
	    }	    
	}

	// update the parameters according to the file for rbn1
	Map<String,List<Double>> params = new HashMap<String,List<Double>>();
	String dire_rbn1 = dire + "/rbn1.txt";
	stream = null;
	try {
	    stream = new FileInputStream(dire_rbn1);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		String name = words[0];		
		List<Double> cpt = new ArrayList<Double> ();
		for (int i = 1; i < words.length; i ++){
		    cpt.add(Double.valueOf(words[i]));
		}
		params.put(name,cpt);
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}
	myObj.update_CPTs(params);
	// evaluate under the new parameters
	ans = myObj.evaluate(query_nodes,evidence);
	// compare here
	dire_rbn_ans = dire + "/rbn1.ans";
	label = new HashMap<String,List<Double>>();
	stream = null;
	try {
	    stream = new FileInputStream(dire_rbn_ans);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		List<Double>probs = new ArrayList<Double>();
		for (int i = 1; i < words.length; i ++){
		    probs.add(Double.valueOf(words[i]));
		}		
		label.put(words[0],probs);		
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}
	// check if they match	
	if (ans.size() != label.size()){
	    System.out.println("  !! rbn1 marginal length differs !!");
	    System.exit(1);
	}
	for (Map.Entry<String,List<Double>> entry : ans.entrySet()) {
	    String var_name = entry.getKey();
	    List<Double> marginal = entry.getValue();
	    List<Double> lab = label.get(var_name);
	    if (marginal.size() != lab.size()){
		System.out.println("  !! rbn1 probability length differs !!");
		System.exit(1);
	    }
	    for (int i = 0; i < marginal.size(); i ++){
		if (Math.abs(marginal.get(i)-lab.get(i)) > eps){
		    System.out.println("  !! rbn1 marginal doesn't match !!");
		    System.exit(1);
		}
	    }	    
	}

	
	// update the parameters according to the file for rbn2
	params = new HashMap<String,List<Double>>();
	String dire_rbn2 = dire + "/rbn2.txt";
	stream = null;
	try {
	    stream = new FileInputStream(dire_rbn2);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		String name = words[0];		
		List<Double> cpt = new ArrayList<Double> ();
		for (int i = 1; i < words.length; i ++){
		    cpt.add(Double.valueOf(words[i]));
		}
		params.put(name,cpt);
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}
	myObj.update_CPTs(params);
	// evaluate under the new parameters
	ans = myObj.evaluate(query_nodes,evidence);
	// compare here
	dire_rbn_ans = dire + "/rbn2.ans";
	label = new HashMap<String,List<Double>>();
	stream = null;
	try {
	    stream = new FileInputStream(dire_rbn_ans);
	}catch (FileNotFoundException e) {
            e.printStackTrace();
        }	
	reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    while ((strLine = reader.readLine()) != null){
		String[] words = strLine.trim().split("\\s+");
		List<Double>probs = new ArrayList<Double>();
		for (int i = 1; i < words.length; i ++){
		    probs.add(Double.valueOf(words[i]));
		}		
		label.put(words[0],probs);		
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}
	// check if they match	
	if (ans.size() != label.size()){
	    System.out.println("  !! rbn2 marginal length differs !!");
	    System.exit(1);
	}
	for (Map.Entry<String,List<Double>> entry : ans.entrySet()) {
	    String var_name = entry.getKey();
	    List<Double> marginal = entry.getValue();
	    List<Double> lab = label.get(var_name);
	    if (marginal.size() != lab.size()){
		System.out.println("  !! rbn2 probability length differs !!");
		System.exit(1);
	    }
	    for (int i = 0; i < marginal.size(); i ++){
		if (Math.abs(marginal.get(i)-lab.get(i)) > eps){
		    System.out.println("  !! rbn2 marginal doesn't match !!");
		    System.exit(1);
		}
	    }	    
	}	
    }
    public static void main( String [] args ){
	System.out.println("Testing the ACE Extension");

	int times = 100;
	for (int i = 0;	 i < times; i ++){
	    System.out.println("== Case " + i + ": ");
	    test_rbn();
	}
	
	/*
	// test constructor
	String[] vars = {"asia", "smoke", "lung", "bronc"};
	Ace_Ext myObj = new Ace_Ext("../examples/asia/asia_det.net", vars);
	// test evaluate
	Map<String,List<Double>> ans;
	List<String>query_nodes = new ArrayList<String> (Arrays.asList("lung","bronc"));
	Map<String,String>evidence = new HashMap<String,String>();
	evidence.put("asia","yes");
	evidence.put("xray","yes");
	ans = myObj.evaluate(query_nodes,evidence);
	List<Double> marginal1 = ans.get("lung");
	assert (marginal1.get(0) == 0.055);
	assert (marginal1.get(1) == 0.945);
	List<Double> marginal2 = ans.get("bronc");
	assert (marginal1.get(0) == 0.45000000000000007);
	assert (marginal1.get(1) == 0.5499999999999999);
	*/
	/*
	for (Map.Entry<String,List<Double>> entry : ans.entrySet()) {
	    String var_name = entry.getKey();
	    List<Double> marginal = entry.getValue();
	    assert 
	}
	*/

	/*
	// test update
	Map<String,List<Double>> params = new HashMap<String,List<Double>>();
	String name = "lung";
	List<Double> cpt = new ArrayList<Double> (Arrays.asList(0.9,0.1,0.8,0.2));
	params.put(name,cpt);
	myObj.update_CPTs(params);
	// test evaluate
	query_nodes = new ArrayList<String> (Arrays.asList("lung","bronc"));
	evidence = new HashMap<String,String>();
	evidence.put("asia","yes");
	evidence.put("xray","yes");
	ans = myObj.evaluate(query_nodes,evidence);
	marginal1 = ans.get("lung");
	assert (marginal1.get(0) == 0.8500000000000001);
	assert (marginal1.get(1) == 0.15000000000000002);
	marginal2 = ans.get("bronc");
	assert (marginal1.get(0) == 0.45000000000000007);
	assert (marginal1.get(1) == 0.5499999999999999);
	*/
    }    
}
