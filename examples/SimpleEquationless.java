import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;

public class SimpleEquationless {
    public static void main(String[] args) {

        StructuralCausalModel m = new StructuralCausalModel();

        int u = m.addVariable(3, true);
        int x = m.addVariable(3);

        m.addParent(x,u);

        BayesianFactor fx = EquationBuilder.of(m).withAllAssignments(x);
        m.setFactor(x, fx);

        BayesianFactor probX = BayesianFactor.random(m.getDomain(x), Strides.empty(),  2, false);

        System.out.println(probX);

        SparseModel credal = m.toCredalNetwork(true, probX);

        for(int v: credal.getVariables())
            System.out.println(credal.getFactor(v));


    }
}
