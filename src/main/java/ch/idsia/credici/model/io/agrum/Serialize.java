package ch.idsia.credici.model.io.agrum;

import java.io.PrintWriter;
import java.util.function.IntFunction;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.netstring.NetStringSerialize;
import ch.idsia.credici.model.io.netstring.NetStringSerialize.Style;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class Serialize {
	
	private IntFunction<String> naming = Integer::toString;
	
	public Serialize() {
		
	}
	
	public Serialize(IntFunction<String> naming) {
		this.naming = naming;
	}
	
	public void serialize(StructuralCausalModel model, PrintWriter out) {
		
		NetStringSerialize ns = new NetStringSerialize(Style.AGRUM);
		ns.setNaming(naming);
		out.println("bn = gum.fastBN(\"" + ns.apply(model) + "\")");
		out.println();
		
		for (int variable : model.getVariables()) {
			var f = model.getFactor(variable);
			serialize(f, variable, out);
		}
	}
	
	public void serialize(BayesianNetwork model, PrintWriter out) {
		
		NetStringSerialize ns = new NetStringSerialize(Style.AGRUM);
		out.println("bn = gum.fastBN(\"" + ns.apply(model) + "\")");
		out.println();
		
		for (int variable : model.getVariables()) {
			var f = model.getFactor(variable);
			serialize(f, variable, out);
		}
	}


	void serialize(BayesianFactor f, int variable, PrintWriter out) {
		Strides domain = f.getDomain();
		Strides conditioning = domain.remove(variable);
		int stride = domain.getStride(variable);
		int size = domain.getCardinality(variable);
		var iter = domain.getIterator(conditioning);
		if (conditioning.getSize() == 0) {
			out.print("bn.cpt(\"" + naming.apply(variable) + "\")[:]=[");
			for (int state = 0; state < size; ++state) {
				out.print(f.getValueAt(state) + ",");
			}
			out.println("]");
		} else {
			while (iter.hasNext()) {
				int[] pos = iter.getPositions().clone();
				int offset = iter.next();
				out.print("bn.cpt(\"" + naming.apply(variable)  + "\")[{");

				int[] vars = conditioning.getVariables();

				for (int i = 0; i < pos.length; ++i) {
					out.print("\"" + naming.apply(vars[i]) + "\":" + pos[i] + ",");
				}

				out.print("}]=[");
				for (int state = 0; state < size; ++state) {
					out.print(f.getValueAt(offset + state * stride) + ",");
				}
				out.println("]");
			}
		}
		out.println();
	}
}
