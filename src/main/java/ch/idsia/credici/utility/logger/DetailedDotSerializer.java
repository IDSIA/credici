package ch.idsia.credici.utility.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.IndexIterator;

public class DetailedDotSerializer {
	
	
	
	
	protected static Function<Integer, String> nodeName(GraphicalModel<BayesianFactor> model) {
		if (model instanceof BayesianNetwork) {
			return (node) -> {
				return "X<sub>" + node + "</sub>";
			};
		} else {
			final StructuralCausalModel sm = (StructuralCausalModel) model;
			return (node) -> {
				String label;
				if (sm.isEndogenous(node)) {
					label = "X";
				} else if (sm.isExogenous(node)) {
					label = "U";
				} else {
					label = "W";
				}
	
				return label + "<sub>" + node + "</sub>";
			};
		}
	}
		

	protected String apply(DoubleTable table, Function<Integer, String> nodeName) {
		NumberFormat formatter = new DecimalFormat("0.###");

		StringBuilder builder = new StringBuilder();
		builder.append("<TABLE cellspacing='0' border='1'>").append("<TR>");

		for (int col : table.getColumns()) {
			builder.append("<TD bgcolor='#333'><FONT color='white'>").append(nodeName.apply(col))
					.append("</FONT></TD>");
		}
		builder.append("<TD><FONT color='white'>W</FONT></TD></TR>");

		// contents
		var iter = table.iterator();
		while (iter.hasNext()) {
			var row = iter.next();
			builder.append("<TR>");

			int[] states = row.getKey();
			Arrays.stream(states).forEach(s -> builder.append("<TD bgcolor='silver'>").append(s).append("</TD>"));

			builder.append("<TD>").append(formatter.format(row.getValue())).append("</TD>");

			builder.append("</TR>");

		}
		builder.append("</TABLE>");
		return builder.toString();
	}

	public String apply(Info record) {
		String name = record.getModelName();
		GraphicalModel<BayesianFactor> model = record.getModel();
		final Function<Integer, String> nodeName = record.getNodeName() == null ? DetailedDotSerializer.nodeName(model) : record.getNodeName();
		
		var highlight = record.getHighlight();
		
		
		StringBuilder builder = new StringBuilder();
		if (name == null)
			name = "model";
		
		builder.append("digraph \"").append(name).append("\" {\n node [shape=none];\n").append("\n");

		if (record.getData() != null)
			builder.append("TABLE [label=<").append(apply(record.getData(), nodeName)).append(">]\n");

		StringBuilder arcs = new StringBuilder();

		NumberFormat formatter = new DecimalFormat("0.###");

		for (int i : model.getVariables()) {
			
			int[] parents = model.getParents(i);
			
			BayesianFactor factor = model.getFactor(i);
			if (factor != null) {
				Strides domain = factor.getDomain();
				Strides conditioning = domain.remove(i);

				int csize = conditioning.getCombinations();
				builder.append("N").append(i).append("[label=<<TABLE cellspacing='0' border='1'>");
				builder.append("<TR><TD bgcolor='#333' colspan='").append(csize + 2).append("'><FONT color='white'>");
				builder.append("P(").append(nodeName.apply(i));
				if (conditioning.getSize() > 0) {
					builder.append("|");
					String c = IntStream.of(conditioning.getVariables()).<String>mapToObj(a -> nodeName.apply(a))
							.collect(Collectors.joining(","));
					builder.append(c);
				}
				builder.append(")</FONT></TD></TR>");

				for (int p : conditioning.getVariables()) {
					builder.append("<TR>");
					builder.append("<TD bgcolor='silver' colspan='2'>").append(nodeName.apply(p)).append("</TD>");

					int stride = conditioning.getStride(p);
					int size = conditioning.getCardinality(p);
					for (int s = 0; s < csize; s += stride) {
						int state = (s / stride) % size;
						builder.append("<TD bgcolor='silver' colspan='").append(stride).append("'>");
						builder.append(state).append("</TD>");
					}
					builder.append("</TR>");
				}

				int stride = domain.getStride(i);
				int states = domain.getCardinality(i);
				for (int state = 0; state < states; ++state) {
					builder.append("<TR>");
					if (state == 0) {
						builder.append("<TD bgcolor='silver' rowspan='").append(states).append("'>")
								.append(nodeName.apply(i)).append("</TD>");
					}

					builder.append("<TD bgcolor='silver'>").append(state).append("</TD>");

					IndexIterator iter = factor.getDomain().getIterator(conditioning);
					while (iter.hasNext()) {
						int index = iter.next();
						boolean h = highlight != null && highlight.containsKey(i) && highlight.get(i).contains(index);
						double value = factor.getValueAt(index + state * stride);
						if (h)
							builder.append("<TD cellpadding='2' bgcolor='#611'><FONT color='#fcc'>");
						else
							builder.append("<TD cellpadding='2'>");
						builder.append(formatter.format(value));
						if (h)
							builder.append("</FONT>");
						builder.append("</TD>");
					}
					builder.append("</TR>");
				}

				builder.append("</TABLE>>];\n");

			} else {
				builder.append("N").append(i).append("[shape=\"circle\" label=<").append(nodeName.apply(i)).append(">];\n");
			}

			for (int parent : parents) {
				arcs.append('N').append(parent).append(" -> N").append(i).append(";\n");
			}
		}

		builder.append(arcs);
		
		if (record.getTitle() != null) builder.append("labelloc=\"t\"\nlabel=\"").append(record.getTitle()).append("\"\n");
		
		builder.append("}");
		return builder.toString();

	}


	
	
	public static void saveModel(String filename, Info r) {
		try {
			DetailedDotSerializer serializer = new DetailedDotSerializer();

			File f = File.createTempFile(filename, ".dot");

			String file = serializer.apply(r);
			Files.writeString(Path.of(f.getAbsolutePath()), file);
			ProcessBuilder b = new ProcessBuilder("/opt/homebrew/bin/dot", "-Tpng", "-o", filename,
					f.getAbsolutePath());
			Process p = b.start();
			p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
