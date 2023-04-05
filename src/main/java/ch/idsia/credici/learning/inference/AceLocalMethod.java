package ch.idsia.credici.learning.inference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;

/**
 * custom ace for each P(U)
 */
public class AceLocalMethod implements EMInference {

    private double setupTime = 0;
	private double queryTime = 0;
	private double readTime = 0;

    private Map<Integer, AceInference> aces = new HashMap<>();



    @Override
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs, String hash) throws InterruptedException, IOException {
        BayesianFactor bf = posteriorModel.getFactor(U);
        AceInference ace = aces.get(U);

        if (ace == null) {
            StructuralCausalModel infModel = new RemoveBarren().execute(posteriorModel, new int[] {U}, obs);
            ace = new AceInference("src/resources/ace");
            ace.setNetwork(infModel);
            ace.compile();
            aces.put(U, ace);
        }
        else {
            ace.update(posteriorModel.getFactor(U), U);
        }

        double[] data =  ace.query(U, obs);

        queryTime += ace.getQueryTime();

        return new BayesianFactor(bf.getDomain(), data);
    }

    public double getQueryTime() {
        return queryTime;
    }
}
