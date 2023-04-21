package ch.idsia.credici.learning.inference;

import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;

public class AceMethod implements ComponentInference {

    private AceInference ace = null;

    @Override
    public BayesianFactor posterior(int u) {
        return ace.posterior(u);
    }

    @Override
    public void set(StructuralCausalModel posteriorModel)    {
        var ace = getAce(posteriorModel);
        ace.update(posteriorModel);
    }

    @Override
    public void update(TIntIntMap observation) throws Exception {
        ace.dirty();
        ace.compute(observation);
    }

    private AceInference getAce(StructuralCausalModel posteriorModel)   {
        if (ace == null) {
            try {
                ace = new AceInference("src/resources/ace");
                ace.init(posteriorModel, true);
            } catch(Exception e) { 
                e.printStackTrace();
            }
        }
        return ace;
    }
}
