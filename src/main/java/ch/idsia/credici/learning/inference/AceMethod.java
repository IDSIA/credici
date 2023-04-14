package ch.idsia.credici.learning.inference;

import java.io.IOException;
import java.util.HashMap;

import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;

public class AceMethod implements EMInference, TotalTiming {

    private double setupTime = 0;
	private double queryTime = 0;
	

    private AceInference ace = null;

    @Override
    public double getQueryTime() {
        return queryTime;
    }

    @Override
    public double getSetupTime() {
        return setupTime;
    }

    @Override
    public void reset() {
        setupTime = 0;
        queryTime = 0;
    }

    @Override
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U,TIntIntMap obs, TIntIntMap filteredobs, String hash) throws InterruptedException, IOException {
        BayesianFactor bf = posteriorModel.getFactor(U);
        if (ace == null) {
            ace = new AceInference("src/resources/ace");
            ace.setNetwork(posteriorModel);
            ace.setUseTable(true);
            ace.compile();
        }
        else {
            ace.update(posteriorModel.getFactor(U), U);
        }

        double[] data =  ace.query(U, obs);

        queryTime += ace.getQueryTime();
        setupTime += ace.getSetupTime();

        return new BayesianFactor(bf.getDomain(), data);
    }
}
