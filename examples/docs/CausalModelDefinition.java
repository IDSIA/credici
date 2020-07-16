package docs;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;



/** NOTE: DO NOT CHANGE TABS HERE */

// 15

public class CausalModelDefinition {
public static void main(String[] args) {

// Explicit definition

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
BayesianFactor pu1 = new BayesianFactor(model.getDomain(u1), new double[] { .4, .6 });
BayesianFactor pu2 = new BayesianFactor(model.getDomain(u2), new double[] { .07, .9, .03, .0 });
BayesianFactor pu3 = new BayesianFactor(model.getDomain(u3), new double[] { .05, .0, .85, .10 });
BayesianFactor pu4 = new BayesianFactor(model.getDomain(u4), new double[] { .05, .9, .05 });

model.setFactor(u1,pu1);
model.setFactor(u2,pu2);
model.setFactor(u3,pu3);
model.setFactor(u4,pu4);

// Define the CPTs of endogenous variables as deterministic functions
BayesianFactor f1 = EquationBuilder.of(model).fromVector(x1, 0,1);
BayesianFactor f2 = EquationBuilder.of(model).fromVector(x2,0,0,1,1,  0,1,0,1);
BayesianFactor f3 = EquationBuilder.of(model).fromVector(x3,0,0,1,1,  0,1,0,1);
BayesianFactor f4 = EquationBuilder.of(model).fromVector(x4,0,1,1,  0,0,0,  0,0,0, 0,1,1);

model.setFactor(x1,f1);
model.setFactor(x2,f2);
model.setFactor(x3,f3);
model.setFactor(x4,f4);

model.printSummary();


////////// Causal Builder

// Bayesian network
BayesianNetwork bnet = new BayesianNetwork();
int y = bnet.addVariable(2);
int x = bnet.addVariable(2);


// Markovian equationless from BN
StructuralCausalModel m1 = StructuralCausalModel.of(bnet);

// Markovian equationless from DAG and sizes
StructuralCausalModel m2 = StructuralCausalModel.of(bnet.getNetwork(), bnet.getSizes(bnet.getVariables()));

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
u1 = dag.addVariable();
u2 = dag.addVariable();
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

//128

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


//150

}
}
