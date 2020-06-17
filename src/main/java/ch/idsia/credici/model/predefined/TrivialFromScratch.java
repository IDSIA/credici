package ch.idsia.credici.model.predefined;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;


public class TrivialFromScratch {
    public static StructuralCausalModel buildModel(){

        // Create an empty model
        StructuralCausalModel model = new StructuralCausalModel();

        // define the variables (endogenous and exogenous)
        int x = model.addVariable(2);                   // endogenous by default
        int ux = model.addVariable(3, true);

        // define the factors
        BayesianFactor fx = BayesianFactor.deterministic(model.getDomain(x), model.getDomain(ux), 1,1,0);
        BayesianFactor pux = new BayesianFactor(model.getDomain(ux), new double[] { 0.6, 0.2, 0.2 });

        model.setFactor(x,fx);
        model.setFactor(ux, pux);

        return model;

    }
}
