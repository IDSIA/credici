import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.RandomUtilities;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.DAGModel;

public class SimpleEquationless {
    public static void main(String[] args) {

        StructuralCausalModel m = new StructuralCausalModel();

        int u = m.addVariable(3, true);
        int x = m.addVariable(3);

        m.addParent(x,u);

        BayesianFactor fx = EquationBuilder.of(m).withAllAssignments(x);
        m.setFactor(x, fx);

        BayesianFactor probX = RandomUtilities.BayesianFactorRandom(m.getDomain(x), Strides.empty(),  2, false);

        System.out.println(probX);

        DAGModel<VertexFactor> credal = m.toVCredal(probX);

        for(int v: credal.getVariables())
            System.out.println(credal.getFactor(v));


    }
}
