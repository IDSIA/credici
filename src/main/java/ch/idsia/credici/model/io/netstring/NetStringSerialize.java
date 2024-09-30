package ch.idsia.credici.model.io.netstring;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jgrapht.graph.DefaultEdge;

import com.google.common.base.Function;


import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;

public class NetStringSerialize {

	private IntFunction<String> naming = Integer::toString;
	
	public void setNaming(IntFunction<String> naming) {
		this.naming = naming;
	}

	
	public record Style(Function<String, String> variable, Function<Integer, String> size, String separator,
			BiFunction<String, String, String> arc_r, BiFunction<String, String, String> arc_l) {
		
		public static Style SCM = new Style((i) -> i, (i) -> "", "", (a, b) -> "(" + a + "->" + b + ")",
				(a, b) -> "(" + a + "<-" + b + ")");

		public static Style AGRUM = new Style((i) -> i, (i) -> "[" + i + "]", ";", (a, b) -> a + "->" + b,
				(a, b) -> a + "<-" + b);
	};

	private Style style = Style.SCM;

	public NetStringSerialize() {
	}

	public NetStringSerialize(Style style) {
		this.style = style;
	}

	private Function<Integer, String> variableAdder(GenericSparseModel<BayesianFactor, SparseDirectedAcyclicGraph> model) {
		return (variable) -> {
			StringBuilder sb = new StringBuilder();
			sb.append(style.variable.apply(naming.apply(variable)))
			.append(style.size().apply(model.getSize(variable)));
			return sb.toString();
		};
	}

	private Function<DefaultEdge, String> edgeAdder(GenericSparseModel<BayesianFactor, SparseDirectedAcyclicGraph> model) {
		return (edge) -> {
			int from = model.getNetwork().getEdgeSource(edge);
			int target = model.getNetwork().getEdgeTarget(edge);
			
			return style.arc_r().apply(
					variableAdder(model).apply(from), 
					variableAdder(model).apply(target)
			);
		};
	}

	

	public String apply(GenericSparseModel<BayesianFactor, SparseDirectedAcyclicGraph> model) {
		return model.getNetwork().edgeSet().stream().map(edgeAdder(model)).collect(Collectors.joining(style.separator));
	}		
}
