package examples;

import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.RandomUtil;

import java.util.Arrays;

public class quasimarkovian {
    public static void main(String[] args) throws InterruptedException {

    for(int i=0; i<1; i++) {
        RandomUtil.setRandomSeed(i);


        int x1, x2, u1, u2, u;

        StructuralCausalModel m = new StructuralCausalModel();
        x1 = m.addVariable(2, false);
        x2 = m.addVariable(2, false);
        u1 = m.addVariable(5, true);
        m.addParents(x2, u1, x1);
        m.addParents(x1, u1);


        m.setFactor(x1,
                BayesianFactor.deterministic(
                        m.getDomain(x1),
                        m.getDomain(u1),
                        0, 1, 1, 0, 0)
        );

        m.setFactor(x2,
                BayesianFactor.deterministic(m.getDomain(x2), m.getDomain(m.getParents(x2)), 1, 1, 1, 1, 0, 0, 0, 1, 0, 0)
        );



       m.setFactor(u1,
                new BayesianFactor(m.getDomain(u1), new double[]{0.2, 0.2, 0.2, 0.2, 0.2})
        );

        m.fillExogenousWithRandomFactors(2);

        System.out.println(m.getFactor(u1));
        m.printSummary();

        System.out.println("\nempirical join:");
        BayesianFactor emp_join = m.getProb(x1).combine(m.getProb(x2)).fixPrecission(9, x1, x2);
        System.out.println("P(X1=0,X2) = " + emp_join.filter(x1, 0));
        System.out.println("P(X1=1,X2) = " + emp_join.filter(x1, 1));


        System.out.println(Arrays.toString(emp_join.getDomain().getVariables()));
        System.out.println(Arrays.toString(emp_join.getData()));


        SparseModel csmodel_v = m.toCredalNetwork(true, m.getEmpiricalProbs());
        SparseModel csmodel_c = m.toCredalNetwork(false, m.getEmpiricalProbs());


        System.out.println("\nCredal set for U1");

        System.out.println(csmodel_v.getFactor(u1));
        ((SeparateHalfspaceFactor) csmodel_c.getFactor(u1)).printLinearProblem();

        CredalCausalVE inf = new CredalCausalVE(m, m.getEmpiricalProbs());
        VertexFactor res1 = (VertexFactor) inf.causalQuery()
                .setIntervention(x1, 1)
                .setTarget(x2)
                .run();

        //System.out.println(res1);

        VertexFactor res2 = (VertexFactor) inf.counterfactualQuery()
                .setIntervention(x1, 1)
                .setEvidence(x1, 0)
                .setTarget(x2)
                .run();

        System.out.println("seed = "+i);
        System.out.println(res1.filter(x2, 0));

        System.out.println(res2.filter(4, 0 ));
        if(res1.getData()[0].length==1 && res2.getData()[0].length>1)
            break;


    }

    }
}
