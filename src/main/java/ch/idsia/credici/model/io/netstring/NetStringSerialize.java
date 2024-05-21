package ch.idsia.credici.model.io.netstring;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jgrapht.graph.DefaultEdge;

import ch.idsia.credici.model.StructuralCausalModel;

public class NetStringSerialize {

	public String apply(StructuralCausalModel model) {
		var network = model.getNetwork();
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		
		for (DefaultEdge edge :  network.edgeSet()) {
			int from = network.getEdgeSource(edge);
			int target = network.getEdgeTarget(edge);
			if (model.isExogenous(from)) { 
				sb1.append("[").append(from);
				sb1.append("->").append(target).append(']');
			} else {
				sb2.append("(").append(from);
				sb2.append("->").append(target).append(')');
			}
		}
		
		sb2.append(sb1);
		return sb2.toString();
	}
}
