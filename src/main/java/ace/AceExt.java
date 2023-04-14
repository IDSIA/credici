package ace;

import java.util.*;

import org.apache.commons.io.IOUtils;

import ch.idsia.crema.utility.ArraysUtil;

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

public class AceExt {
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
	// inst file
	private String m_inst_file;
	// Ace compile file location
	private String m_compile_file;
	// Ace evaluate file location
	private String m_evaluate_file;
	// marginal file
	private String m_marginal_file;

	// conversion between variable and name
	private Map<String, Variable> n2v;
	private Map<Variable, String> v2n;

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
	private String[] m_variables;

	private boolean table = false;
	/*
	 * net_file: a string representing the location of the .NET file
	 * variables: variables whose CPTs may be modified during training
	 */
	public AceExt(String net_file,  String[] variables, String exepath, boolean table) {
		// for constructing the network
		m_net_file = net_file;
		this.table = table;
		// create a dummy net file
		m_dummy_net_file = m_net_file.substring(0, m_net_file.length() - 4) + "_dummy.net";
		m_dummy_lmap_file = m_dummy_net_file + ".lmap";
		m_lmap_file = m_net_file + ".lmap";
		m_inst_file = m_net_file + ".inst";
		m_marginal_file = m_net_file + ".marginals";
		m_compile_file = exepath + "/compile";
		m_evaluate_file = exepath + "/evaluate";

		m_variables = variables;
		m_vars = new ArrayList<Variable>();
		n2v = new HashMap<String, Variable>();
		v2n = new HashMap<Variable, String>();
		m_num_params = 0;
		m_order_potentials = new ArrayList<String>();

		// for parsing and updating lmap file
		dum2fac = new HashMap<Double, Factor>();
		lmap_fix_lines = new HashMap<Integer, String>();
		lmap_var_lines = new HashMap<Integer, Factor>();
		lmap_num_lines = 0;

		/* Operations on NET file */
		// parse the .net file
		if(debug) System.out.println("=== ACE Extension for Training v1.0 ===");
		long startTime = 0;

		if(debug) startTime = System.currentTimeMillis();
		if(debug) System.out.println("  ** parse net file **");
		parse_net(variables);

		if(debug) System.out.println("  ** create a dummy net file **");
		generate_dummy_net(m_dummy_net_file);
		
		/* Operations on lmap file */
		// invoke the process that creates .lmap file from dummy net file
		if(debug) System.out.println("  ** compile the dummy net file **");
		Ice_compile(m_dummy_net_file, m_compile_file);

		// parse the lmap file which updates lmap_fix_lines and lmap_var_lines
		if(debug) System.out.println("  ** parse the dummy lmap file **");
		parse_lmap(m_dummy_lmap_file);

		// generate an lmap file for the original .net file
		if(debug) System.out.println("  ** generate the lmap file **");
		generate_lmap(m_lmap_file);
		
		if(debug) {
			long endTime = System.currentTimeMillis();
			System.out.println(" = Compilation took " + (endTime - startTime) + "ms");
		}
	}

	// update CPTs for variables
	// Mapping of variable and 1D array for each variable
	// also update the lmap file
	public void update_CPTs(Map<String, List<Double>> params) {
		long startTime = 0;
		if (debug) startTime = System.currentTimeMillis();

		// update CPTs
		for (Map.Entry<String, List<Double>> entry : params.entrySet()) {
			String var_name = entry.getKey();
			List<Double> cpt = entry.getValue();
			Variable var = n2v.get(var_name);
			for (int i = 0; i < cpt.size(); i++) {
				var.modify_CPT(cpt.get(i), i, false);
			}
		}
		// update lmap
		generate_lmap(m_lmap_file);
		if(debug) {
			long endTime = System.currentTimeMillis();
			System.out.println("== Updating parameters took " + (endTime - startTime) + "ms");
		}
	}

	// compute query
	// return marginals for each queried variable
	// input query_nodes: names of the nodes that will be queried
	// evidence: map variable to a value

	public Map<String, List<Double>> evaluate(List<String> query_nodes,
			Map<String, String> evidence) {
		long startTime = 0;
		if(debug) startTime = System.currentTimeMillis();
		// we first create an .inst file
		write_inst(evidence);
		// execute the marginal
		// create a process which execute the command
		String command = m_evaluate_file + " " + m_net_file + " " + m_inst_file;
		String io = ""; 
		try {
			//Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
	
			Process p = Runtime.getRuntime().exec(command);
			try {
				int exit = p.waitFor();
				if (exit != 0) { 
					throw new RuntimeException(m_evaluate_file + " " + m_net_file + " " + m_inst_file);
				}
				io = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// extract query information from the marginal file
		Map<String, List<Double>> query_ans = new HashMap<String, List<Double>>();
		HashSet<String> query_vars = new HashSet<String>(query_nodes);
		match_queries(query_ans, query_vars);
		save_timings(io);
		if(debug) {
			long endTime = System.currentTimeMillis();
			System.out.println("== Evaluating " + query_nodes.size() + " query nodes with " + evidence.size()
					+ " evidences took " + (endTime - startTime) + "ms " + command);
		}
		return query_ans;
	}

	private double lastSetupTime;
	private double lastQueryTime;
	private double lastReadTime;

	private void save_timings(String io) {
		this.lastSetupTime = -1;
		this.lastQueryTime = -1;
		this.lastReadTime = -1;

		io.lines().forEach(line -> {
			if (line.startsWith("  Total Initialization Time (ms) : ")) 
				this.lastSetupTime = Double.valueOf(line.split(":")[1].trim());
			else if (line.startsWith("Network Read Time (ms) : ")) 
				this.lastReadTime = Double.valueOf(line.split(":")[1].trim());
			else if (line.startsWith("  Total Inference Time (ms) : "))
				this.lastQueryTime = Double.valueOf(line.split(":")[1].trim());
		});
	}


	public double getLastQueryTime() {
		return lastQueryTime;
	}

	public double getLastReadTime() {
		return lastReadTime;
	}
	public double getLastSetupTime() {
		return lastSetupTime;
	}
	
	private void match_queries(Map<String, List<Double>> query_ans, HashSet<String> query_vars) {
		/*
		 * The file stream code was referenced from
		 * https://stackoverflow.com/questions/45826412/how-to-parse-a-simple-text-file-
		 * in-java
		 */
		String strLine;
		try (BufferedReader reader = new BufferedReader(new FileReader(m_marginal_file))) {
			while ((strLine = reader.readLine()) != null) {
				if (strLine.startsWith("Pr(e)")) {
					String[] e = strLine.split("=");
					double pe = Double.valueOf(e[1].trim());
					query_ans.put("e", Arrays.asList(pe));
				} else if (strLine.contains(",e)")) {
					String[] words = strLine.split("\\,");
					String var_name = words[0].substring(3, words[0].length());
					// if the variable is one of the queried var
					//if (query_vars.contains(var_name)) {
						List<Double> vals = new ArrayList<Double>();
						int recording = 0;
						String newstr = "";
						for (int i = 1; i < strLine.length(); i++) {
							if (strLine.charAt(i - 1) == '[')
								recording = 1;
							if (strLine.charAt(i) == ']')
								recording = 0;
							if (recording == 1 && (strLine.charAt(i) != ' '))
								newstr += strLine.charAt(i);
						}
						words = newstr.split("\\,");
						for (int i = 0; i < words.length; i++) {
							Double dec_num = Double.parseDouble(words[i]);
							vals.add(dec_num);
						}
						query_ans.put(var_name, vals);
					//}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void write_inst(Map<String, String> evidence) {
		String buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<instantiation date=\"Jun 4, 2005 7:07:21 AM\">\n";
		for (Map.Entry<String, String> entry : evidence.entrySet()) {
			String var_name = entry.getKey();
			String val = entry.getValue();
			buffer += "<inst id=\"";
			buffer += var_name;
			buffer += "\" value=\"";
			buffer += val;
			buffer += "\"/>\n";
		}
		buffer += "</instantiation>\n";
		// write to the file
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(m_inst_file))){
			writer.write(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// parse the .net network file
	private void parse_net(String[] variables) {
		
		String strLine;
		try (BufferedReader reader = new BufferedReader(new FileReader(m_net_file))) {
			while ((strLine = reader.readLine()) != null) {
				String[] words = strLine.trim().split("\\s+");
				// create a new node
				if (words[0].equals("node")) {
					String name = words[1];
					Variable var = new Variable();
					var.set_name(name);
					n2v.put(name, var);
					v2n.put(var, name);
					m_vars.add(var);
					// we set it to be a variable if found it in m_variables
					for (int i = 0; i < m_variables.length; i++) {
						if (m_variables[i].equals(name)) {
							var.set_isVariable();
							break;
						}
					}

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
						var = n2v.get(child);
					}
					// if | exists in the string
					else {
						child = words[pos_line - 1];
						var = n2v.get(child);
						// add parents for the child
						for (int i = pos_line + 1; i < words.length; i++) {
							if (words[i].charAt(0) != ')') {
								var.add_parent(n2v.get(words[i]));
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
		if(debug) {
			System.out.println("      = NET INFO = ");
			System.out.println("          total " + m_vars.size() + " nodes: " + m_variables.length +
					" variables, " + (m_vars.size() - m_variables.length) + " constants");
			System.out.println("          total " + m_num_params + " parameters ");
		}
	}

	// create a dummy net file
	// all CPTs are filled with dummy values
	private void generate_dummy_net(String dummy_file) {
		// each increment step
		Double dummy_step = 1.0 / 2 / m_num_params;
		// initial dummy value
		Double dummy_val = 0.0;

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
			Variable var = n2v.get(name);
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
		//String command1 = compile_file + " " + method + " -cd06 " + net_file;
		//String command2 = "mv " + net_file + ".ac" + " " + m_net_file + ".ac";
		//System.out.println(command1);
		//System.out.println(command2);
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
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(lmap_file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String strLine;
		try {
			while ((strLine = reader.readLine()) != null) {
				String[] words = strLine.split("\\$");
				boolean fixed = true;
				for (int i = 0; i < words.length; i++) {
					String word = words[i];
					// if the word is a numerical value and is one of the keys
					// we add the line to be a var line, also update the factor
					if (Utils.isNumeric(word) && dum2fac.containsKey(Double.parseDouble(word))) {
						Factor f = dum2fac.get(Double.parseDouble(word));
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
}
