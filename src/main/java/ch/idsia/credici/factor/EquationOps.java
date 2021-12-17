package ch.idsia.credici.factor;

import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import gnu.trove.map.hash.TIntIntHashMap;

public class EquationOps {
	public static void setValue(BayesianFactor f, TIntIntHashMap exoPaValues, TIntIntHashMap endoPaValues, int var, int value){


		TIntIntHashMap conf = new TIntIntHashMap();

		for(int y : endoPaValues.keys())
			conf.put(y, endoPaValues.get(y));

		for(int u : exoPaValues.keys())
			conf.put(u, exoPaValues.get(u));

		for(int v=0; v<f.getDomain().getCardinality(var); v++){
			conf.put(var,v);
			int offset = FactorUtil.getOffset(f, conf);
			if(v!=value)
				f.setValueAt(0, offset);
			else
				f.setValueAt(1, offset);
		}

	}
}
