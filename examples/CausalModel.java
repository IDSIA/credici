import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.credici.model.StructuralCausalModel;

public class CausalModel {
    public static void main(String[] args) {

        /* This code implements the Party example */

        StructuralCausalModel model = new StructuralCausalModel();

        // define the variables (endogenous and exogenous)
        int x1 = model.addVariable(2);
        int x2 = model.addVariable(2);
        int x3 = model.addVariable(2);
        int x4 = model.addVariable(2);

        int u1 = model.addVariable(2, true);
        int u2 = model.addVariable(4, true);
        int u3 = model.addVariable(4, true);
        int u4 = model.addVariable(3, true);

        // Define the structure
        model.addParents(x1, u1);
        model.addParents(x2, u2, x1);
        model.addParents(x3, u3, x1);
        model.addParents(x4, u4, x2, x3);




        // define the CPTs of the exogenous variables
        BayesianFactor pu1 = BayesianFactorBuilder.as(model.getDomain(u1), new double[] { .4, .6 });
        BayesianFactor pu2 = BayesianFactorBuilder.as(model.getDomain(u2), new double[] { .07, .9, .03, .0 });
        BayesianFactor pu3 = BayesianFactorBuilder.as(model.getDomain(u3), new double[] { .05, .0, .85, .10 });
        BayesianFactor pu4 = BayesianFactorBuilder.as(model.getDomain(u4), new double[] { .05, .9, .05 });

        model.setFactor(u1,pu1);
        model.setFactor(u2,pu2);
        model.setFactor(u3,pu3);
        model.setFactor(u4,pu4);

        // Define the CPTs of endogenous variables as deterministic functions
        BayesianFactor f1 = BayesianFactorBuilder.deterministic(model.getDomain(x1), model.getDomain(u1),0,1);

        BayesianFactor f2 = BayesianFactorBuilder.deterministic(model.getDomain(x2), model.getDomain(u2,x1),
                0,0,1,1,  0,1,0,1);

        BayesianFactor f3 = BayesianFactorBuilder.deterministic(model.getDomain(x3), model.getDomain(u3,x1),
                0,0,1,1,  0,1,0,1);



        BayesianFactor f4 = BayesianFactorBuilder.deterministic(model.getDomain(x4), model.getDomain(u4,x3,x2),
                0,1,1,  0,0,0,  0,0,0, 0,1,1);


        model.setFactor(x1,f1);
        model.setFactor(x2,f2);
        model.setFactor(x3,f3);
        model.setFactor(x4,f4);

        model.printSummary();


        BayesianFactor f4_ = EquationBuilder.of(model).withAllAssignments(x4);
        System.out.println(f4_);

    }
}
