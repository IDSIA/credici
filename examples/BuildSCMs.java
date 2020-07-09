import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class BuildSCMs {

    public static void main(String[] args) {

        // Bayesian network
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);


        /** Simple API **/

        // Markovian equationless from BN
        StructuralCausalModel m1 = StructuralCausalModel.of(bnet);
        // Markovian equationless from DAG and sizes
        StructuralCausalModel m2 = StructuralCausalModel.of(bnet.getNetwork(), bnet.getSizes(bnet.getVariables()));

        /** Parametrizable API **/

        // Markovian equationless from BN
        StructuralCausalModel m3 = CausalBuilder.of(bnet).build();
        // Markovian equationless from DAG and sizes
        StructuralCausalModel m4 = CausalBuilder.of(bnet.getNetwork(), bnet.getSizes(bnet.getVariables())).build();

        /// Aditional cases:

        // Markovian equationless with random P(U)
        StructuralCausalModel m5 =
                CausalBuilder.of(bnet)
                .setFillRandomExogenousFactors(2)
                .build();

        // Markovian with random P(U) and equations
        StructuralCausalModel m6 =
                CausalBuilder.of(bnet)
                        .setFillRandomExogenousFactors(2)
                        .setFillRandomEquations(true)
                        .build();

        // Markovian equationaless specifying causal DAG
        SparseDirectedAcyclicGraph dag = bnet.getNetwork().copy();
        int u1 = dag.addVariable();
        int u2 = dag.addVariable();
        dag.addLink(u1, y);
        dag.addLink(u2, x);

        StructuralCausalModel m7 =
                CausalBuilder.of(bnet)
                        .setCausalDAG(dag)
                        .build();


        // Quasi Markovian specifying causal DAG with random factors
        SparseDirectedAcyclicGraph dag2 = bnet.getNetwork().copy();
        int u = dag2.addVariable();
        dag2.addLink(u, y);
        dag2.addLink(u,x);

        StructuralCausalModel m8 =
                CausalBuilder.of(bnet)
                        .setCausalDAG(dag2)
                        .setExoVarSizes(new int[]{4})
                        .setFillRandomEquations(true)
                        .build();



        // Markovian case specifying equations and with random exogenous factors

        BayesianFactor eqy = EquationBuilder.fromVector(
                Strides.as(y,2), Strides.as(u1,2),
                0,1
        );

        BayesianFactor eqx = EquationBuilder.fromVector(
                Strides.as(x,2), Strides.as(u2,4),
                0,1,1,0
        );

        BayesianFactor[] eqs = {eqy, eqx};

        StructuralCausalModel m9 = CausalBuilder.of(bnet)
                .setEquations(eqs)
                .setFillRandomExogenousFactors(3)
                .build();


        // Quasi markovian equationless case (specifying DAG and exo sizes)
        // todo: not implemented yet



    }

}
