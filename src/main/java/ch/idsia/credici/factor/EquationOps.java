package ch.idsia.credici.factor;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.hash.TIntIntHashMap;

public class EquationOps {
	public static void setValue(BayesianFactor f, TIntIntHashMap exoPaValues, TIntIntHashMap endoPaValues, int var, int value){
		//System.out.println("exoPaValues: "+exoPaValues);
		//System.out.println("endoPaValues: "+endoPaValues);


		TIntIntHashMap conf = new TIntIntHashMap();

		for(int y : endoPaValues.keys())
			conf.put(y, endoPaValues.get(y));

		for(int u : exoPaValues.keys())
			conf.put(u, exoPaValues.get(u));

		for(int v=0; v<f.getDomain().getCardinality(var); v++){
			conf.put(var,v);
			int offset = FactorUtil.getOffset(f, conf);
			if(v!=value) {
				f.setValueAt(0, offset);
				//System.out.println("0 -> "+offset);
			}
			else {
				f.setValueAt(1, offset);
				//System.out.println("1 -> "+offset);

			}
		}

	}

	public static int maxExoCardinality(Strides domEndoCh, Strides domEndoPa) {
		int cardEndoCh = domEndoCh.getCombinations();
		int cardEndoPa = domEndoPa.remove(domEndoCh).getCombinations();
		return (int)Math.pow(cardEndoCh, cardEndoPa);
	}

	public static int maxExoCardinality(int exoVar, StructuralCausalModel model) {
		if(!model.isExogenous(exoVar))
			throw new IllegalArgumentException("Variable "+exoVar+" is not exogenous");
		Strides domEndoCh = model.getDomain(model.getEndogenousChildren(exoVar));
		Strides domEndoPa = model.getDomain(model.getEndegenousParents(model.getEndogenousChildren(exoVar)));
		return maxExoCardinality(domEndoCh, domEndoPa);
	}
}
