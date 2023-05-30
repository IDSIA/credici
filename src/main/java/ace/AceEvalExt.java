package ace;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.IOUtils;

import ch.idsia.crema.utility.ArraysUtil;
import edu.ucla.belief.ace.Evidence;
import edu.ucla.belief.ace.OnlineEngineSop;
import gnu.trove.map.TIntIntMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AceEvalExt {

	private CrediciEngine engine;

	private final static boolean debug = false;

	// all varaibles from the BN
	private List<Variable> m_vars;
	// net file
	private String m_net_file;
	// lmap file
	private String m_lmap_file;
	// dummy net file
	private String m_dummy_net_file;
	// dummy lmap file
	private String m_dummy_lmap_file;
	// Ace compile file location
	private String m_compile_file;
	

	// conversion between variable and name
	private Map<Integer, Variable> n2v;

	// utils
	private Utils utils;

	// number of total parameters in CPTs
	private int m_num_params;
	// order of potentials for the variables
	private List<String> m_order_potentials;

	// for lmap parser
	// map a dummy CPT value to a factor
	private Map<Double, Factor> dum2fac;
	// lmap lines - most of which are invariant
	private Map<Integer, String> lmap_fix_lines;
	// lmap lines that should be replaced by the factor's i_lmap
	private Map<Integer, Factor> lmap_var_lines;
	// lmap number of lines
	private int lmap_num_lines;

	// variables whose CPTs can be modified
	// we should randomize those CPTs when compile the Ace
	private Set<Integer> m_variables;

	private boolean table = false;

	/**
	 * Create from a compiled engine like the one provided
	 * @param engine
	 * @throws Exception
	 */
	public AceEvalExt(AceEvalExt copy) throws Exception {
		this.outInVarMapping = new HashMap<>(copy.outInVarMapping);
		this.inOutVarMapping = new HashMap<>(copy.inOutVarMapping);

		String ac = copy.engine.getAcfilename();
		m_lmap_file = copy.engine.getLmfilename();
		this.engine = new CrediciEngine(m_lmap_file, ac, true);
	}

	/**
	 * @param net_file string representing the location of the .NET file
	 * @param variables int[] variables whose CPTs may be modified during training
	 * @param exepath the location of the ace executables
	 * @param table boolean indicating whether to use the java implemntation (true) or the native c2d compile (false)
	 */
	public AceEvalExt(String net_file,  int[] variables, String exepath, boolean table) throws Exception {

		// for constructing the network
		m_net_file = net_file;
		this.table = table;
		// create a dummy net file
		m_dummy_net_file = m_net_file.substring(0, m_net_file.length() - 4) + "_dummy.net";
		m_dummy_lmap_file = m_dummy_net_file + ".lmap";
		m_lmap_file = m_net_file + ".lmap";
		m_compile_file = exepath + "/compile";
		
		m_variables = IntStream.of(variables).boxed().collect(Collectors.toCollection(HashSet::new));

		m_vars = new ArrayList<>();
		n2v = new HashMap<>();
		m_num_params = 0;
		m_order_potentials = new ArrayList<>();

		// for parsing and updating lmap file
		dum2fac = new HashMap<>();
		lmap_fix_lines = new HashMap<>();
		lmap_var_lines = new HashMap<>();
		lmap_num_lines = 0;

		/* Operations on NET file */
		// parse the .net file

		parse_net();

		generate_dummy_net(m_dummy_net_file);
		
		/* Operations on lmap file */
		// invoke the process that creates .lmap file from dummy net file
		Ice_compile(m_dummy_net_file, m_compile_file);

		// parse the lmap file which updates lmap_fix_lines and lmap_var_lines
		parse_lmap(m_dummy_lmap_file);
	

		// generate an lmap file for the original .net file
		if(debug) System.out.println("  ** generate the lmap file **");
		generate_lmap(m_lmap_file);
		
		// init ace engine
		engine = new CrediciEngine(m_lmap_file, m_net_file + ".ac", true);	
		storeNameMap();
	}
	
	Map<Integer, Integer> outInVarMapping;
	Map<Integer, Integer> inOutVarMapping;
	private void storeNameMap() {
		this.outInVarMapping = new HashMap<>();
		this.inOutVarMapping = new HashMap<>();

		for (Map.Entry<Integer, Variable> entry : n2v.entrySet()) {
			int v = engine.varForName("n"+entry.getKey());
			outInVarMapping.put(entry.getKey(), v);
			inOutVarMapping.put(v, entry.getKey());
		}
	}
	// update CPTs for variables
	// Mapping of variable and 1D array for each variable
	// also update the lmap file
	public void update_CPTs(Integer u, double[] params) {
		Variable var = n2v.get(u);
		for (int i = 0; i < params.length; i++) {
			int idx = var.getAbsoluteIndex(i);
			//var.get_factors().get(i).set_value(params[i]);
			engine.updateFactor(idx, params[i]);
		}
	}

	public void commitLmap() throws Exception {
		generate_lmap(m_lmap_file);
		engine = new CrediciEngine(m_lmap_file, m_net_file + ".ac", table);
	}

	// compute query
	// return marginals for each queried variable
	// input query_nodes: names of the nodes that will be queried
	// evidence: map variable to a value

	public Map<Integer, double[]> evaluate(int[] query, TIntIntMap evidence) throws Exception {
			Evidence e = new Evidence(engine);

		evidence.forEachEntry((key,val)->{
			int a = outInVarMapping.get(key);
			e.varCommit(a, val);
			return true;
		});
		engine.evaluate(e);

		Map<Integer, double[]> res = new HashMap<>();
		res.put(-1, new double[] {engine.evaluationResults()});

		engine.differentiate();

		for (int v : query) {
			int veng = outInVarMapping.get(v);
			double[] d = engine.varMarginals(veng); // P(v,e)
			res.put(v, d);
		}
		return res;
	}



	// parse the .net network file
	private void parse_net() {
		
		String strLine;
		try (BufferedReader reader = new BufferedReader(new FileReader(m_net_file))) {
			while ((strLine = reader.readLine()) != null) {
				String[] words = strLine.trim().split("\\s+");
				// create a new node
				if (words[0].equals("node")) {
					String name = words[1];
					
					int vid = Integer.parseInt(name.substring(1));
					Variable var = new Variable();
					var.set_name(name);
					
					n2v.put(vid, var);
					
					m_vars.add(var);

					// we set it to be a variable if found it in m_variables
					if (m_variables.contains(vid))
							var.set_isVariable();						
	
	
					// read another line that contains {
					while ((strLine = reader.readLine()) != null) {
						words = strLine.trim().split("\\s+");
						if (words[0].equals("states")) {
							break;
						}
					}
					for (int i = 0; i < words.length; i++) {
						String word = words[i];
						if (word.length() <= 2) {
							continue;
						}
						if (word.charAt(0) == '\"') {
							var.add_value(word.substring(1, word.length() - 1));
						} else if (word.charAt(1) == '\"') {
							var.add_value(word.substring(2, word.length() - 1));
						}
					}
				}
				// create potential for a node
				else if (words[0].equals("potential")) {
					int pos_line = 0;
					for (pos_line = 0; pos_line < words.length; pos_line++) {
						if (words[pos_line].equals("|"))
							break;
					}
					String child;
					Variable var;
					
					// if no | in the string
					if (pos_line == words.length) {
						child = words[pos_line - 2];
						if (child.equals(")")) {
							child = words[pos_line - 3];	
						}
						int cid = varId(child);
						var = n2v.get(cid);
					}
					// if | exists in the string
					else {
						child = words[pos_line - 1];
						int cid = varId(child);
						var = n2v.get(cid);
						// add parents for the child
						for (int i = pos_line + 1; i < words.length; i++) {
							if (words[i].charAt(0) != ')') {
								int pid = varId(words[i]);
								var.add_parent(n2v.get(pid));
							} else
								break;
						}
					}
					// we next extract the CPTs
					while ((strLine = reader.readLine()) != null) {
						if (strLine.equals("}")) {
							break;
						} else {
							strLine = strLine.replace("(", " ");
							strLine = strLine.replace(")", " ");
							words = strLine.trim().split("\\s+");
							for (int i = 0; i < words.length; i++) {
								String word = words[i];
								if (utils.isNumeric(word)) {
									var.modify_CPT(Double.parseDouble(word), -1, false);
									m_num_params++;
								}
							}
						}
					}
					m_order_potentials.add(child);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Summarize graph info
	}

	// create a dummy net file
	// all CPTs are filled with dummy values
	private void generate_dummy_net(String dummy_file) {
		// each increment step
		double dummy_step = 1.0 / 2 / m_num_params;
		// initial dummy value
		double dummy_val = 0.0;

		String buffer = "net\n{\n}\n";
		// add nodes
		for (int i = 0; i < m_vars.size(); i++) {
			Variable var = m_vars.get(i);
			buffer += "node ";
			buffer += var.get_name();
			buffer += "\n{\nstates = ( ";
			List<String> values = var.get_values();
			for (int j = 0; j < values.size(); j++) {
				String value = values.get(j);
				buffer += "\"";
				buffer += value;
				buffer += "\" ";
			}
			buffer += ");\n}\n";
		}

		// add potentials
		for (int i = 0; i < m_order_potentials.size(); i++) {
			String name = m_order_potentials.get(i);
			int vid = varId(name);
			Variable var = n2v.get(vid);
			buffer += "potential ( ";
			buffer += var.get_name();
			List<Variable> parents = var.get_parents();
			if (parents.size() == 0) {
				buffer += " )\n";
			} else {
				buffer += " | ";
				for (int j = 0; j < parents.size(); j++) {
					buffer += parents.get(j).get_name();
					buffer += " ";
				}
				buffer += ")\n";
			}
			buffer += "{\n";
			buffer += "data = ";
			Map<Integer, Factor> factors = var.get_factors();
			for (int j = 0; j < factors.size(); j++) {
				Factor f = factors.get(j);
				// if the entry is deterministic, we use the original CPT value
				if (f.isfixed()) {
					var.modify_CPT(f.get_value(), j, true);
				} else {
					dummy_val += dummy_step;
					var.modify_CPT(dummy_val, j, true);
					dum2fac.put(f.get_dummy_value(), f);
				}
			}
			List<Double> dummy_CPT = var.get_dummy_CPT();
			List<Integer> parents_card = new ArrayList<Integer>();
			for (int j = 0; j < parents.size(); j++) {
				Variable par_var = parents.get(j);
				parents_card.add(par_var.get_values().size());
			}
			buffer += utils.make_CPT(dummy_CPT, parents_card);
			buffer += ";\n}\n";
		}

		// write to the file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(dummy_file))) {
			writer.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void Ice_compile(String net_file, String compile_file) {
		// create a process which execute the command
		String method = this.table ? "-forceTabular" : "-forceC2d";
		try {
				Process p = new ProcessBuilder().
					command(compile_file, method, "-cd06", net_file).
					redirectError(new File("err.err")).
					start();

				int exit = p.waitFor();
				if (exit != 0) {
					throw new RuntimeException(compile_file +  " "+method +" -cd06 "+ net_file);
				}
				
				p = new ProcessBuilder().
					command("mv", net_file + ".ac", m_net_file + ".ac").
					redirectError(new File("err2.err")).
					start();

				exit = p.waitFor();
				if (exit != 0) {
					throw new RuntimeException(compile_file +  " "+ method +" -cd06 "+ net_file);
				}
		} catch (InterruptedException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parse_lmap(String lmap_file) {
		String strLine;
		try (BufferedReader reader = new BufferedReader(new FileReader(lmap_file))) {
			while ((strLine = reader.readLine()) != null) {
				String[] words = strLine.split("\\$");
				boolean fixed = true;
				for (int i = 0; i < words.length; i++) {
					String word = words[i];
					// if the word is a numerical value and is one of the keys
					// we add the line to be a var line, also update the factor
					if (Utils.isNumeric(word) && dum2fac.containsKey(Double.parseDouble(word))) {
						Factor f = dum2fac.get(Double.parseDouble(word));
						f.setIndex(Integer.parseInt(words[2]));
						String rpl_str = strLine.replace(word, "@");
						f.set_lmap(rpl_str);

						lmap_var_lines.put(lmap_num_lines, f);
						fixed = false;
						break;
					}
				}
				if (fixed) {
					lmap_fix_lines.put(lmap_num_lines, strLine);
				}
				lmap_num_lines++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void generate_lmap(String lmap_file) {
		String buffer = "";
		for (int i = 0; i < lmap_num_lines; i++) {
			// if line i is a variable line
			if (lmap_var_lines.containsKey(i)) {
				Factor f = lmap_var_lines.get(i);
				String s = f.get_lmap();
				buffer += s;
				buffer += "\n";
			}
			// otherwise, we directly print the line
			else {
				buffer += lmap_fix_lines.get(i);
				buffer += "\n";
			}
		}
		// write to the file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(lmap_file))) {
			writer.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int varId(String name) {
		return Integer.parseInt(name.substring(1));
	}
}
