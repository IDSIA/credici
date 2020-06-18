package pgm20.examples;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.credici.model.StructuralCausalModel;

public class GenerateModels {
    public static void main(String[] args) {


        int x1, x2, u1, u2, u;

        StructuralCausalModel m = new StructuralCausalModel();
        x1 = m.addVariable(2, false);
        x2 = m.addVariable( 2, false);
        u1 = m.addVariable( 3, true);
        u2 = m.addVariable( 5, true);

        m.addParents(x2, u2,x1);
        m.addParents(x1, u1);


        m.setFactor( x1,
                BayesianFactor.deterministic(m.getDomain(x1), m.getDomain(u1), 0, 1, 1)
        );

        m.setFactor( x2,
                //BayesianFactor.deterministic(m.getDomain(x2), m.getDomain(m.getParents(x2)), 1,1, 1,0, 0,0, 0,1, 0,0) // original
                BayesianFactor.deterministic(m.getDomain(x2), m.getDomain(m.getParents(x2)), 1,1, 1,1, 0,0, 0,1, 0,0)   // without CI
        );

        m.setFactor(u1,
                new BayesianFactor(m.getDomain(u1), new double[]{0.33333333, 0.33333333, 0.33333334})
                //new BayesianFactor(m.getDomain(u1), new double[]{0.3, 0.3, 0.4})

        );


        m.setFactor(u2,
                new BayesianFactor(m.getDomain(u2), new double[]{0.2, 0.2, 0.2, 0.2, 0.2})
        );

        m.printSummary();

        System.out.println("\nempirical join:");
        BayesianFactor emp_join = m.getProb(x1).combine(m.getProb(x2)).fixPrecission(9, x1,x2);
        System.out.println("P(X1=0,X2) = "+emp_join.filter(x1,0));
        System.out.println("P(X1=1,X2) = "+emp_join.filter(x1,1));


        SparseModel csmodel_v = m.toCredalNetwork(true, m.getEmpiricalProbs());
        SparseModel csmodel_c = m.toCredalNetwork(false, m.getEmpiricalProbs());


        System.out.println("\nCredal set for U1");

        System.out.println(csmodel_v.getFactor(u1));
        ((SeparateHalfspaceFactor) csmodel_c.getFactor(u1)).printLinearProblem();

        System.out.println("\nCredal set for U2");
        System.out.println(csmodel_v.getFactor(u2));
        ((SeparateHalfspaceFactor) csmodel_c.getFactor(u2)).printLinearProblem();

        System.out.println("\nFactorisation: ");

        System.out.println("P(X1) = "+m.getEmpiricalProbs()[0]);

        System.out.println("P(X2|X1=0) = "+ m.getEmpiricalProbs()[1].filter(0, 0));
        System.out.println("P(X2|X1=1) = "+m.getEmpiricalProbs()[1].filter(0,1));

        BayesianFactor[] emp = new BayesianFactor[2];
        int i = 0;
        for(int x: m.getEndogenousVars()){
            emp[i] = m.getProb(x).fixPrecission(5, x);

            i++;

        }

        //m.findModelWithEmpirical(4,emp, 100).printSummary();

        /// Quasi markovian
        System.out.println("\n\nQuasi-markovian\n");
        StructuralCausalModel m2 = new StructuralCausalModel();
        x1 = m2.addVariable(2, false);
        x2 = m2.addVariable( 2, false);
        u = m2.addVariable( 5, true);

        m2.addParents(x2, u,x1);
        m2.addParents(x1, u);

        m2 = m2.findModelWithEmpirical(3,new BayesianFactor[]{emp_join}, new int[]{}, 10000);


        m2.printSummary();

        System.out.println("\nempirical join 2:");
        BayesianFactor emp_join2 = m2.getEmpiricalProbs()[0].fixPrecission(9, x1,x2);
        System.out.println("P(X1=0,X2) = "+emp_join2.filter(x1,0));
        System.out.println("P(X1=1,X2) = "+emp_join2.filter(x1,1));


        SparseModel csmodel2_v = m2.toCredalNetwork(true, emp_join2);
        SparseModel csmodel2_c = m2.toCredalNetwork(false, emp_join2);


        System.out.println("\nCredal set for U");

        System.out.println(csmodel2_v.getFactor(u));
        ((SeparateHalfspaceFactor) csmodel2_c.getFactor(u)).printLinearProblem();


    }
}
