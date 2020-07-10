package pgm20.examples;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.Party;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class Example9 {
    public static void main(String[] args) throws InterruptedException {


        // precise simple scm
        int x1=0, x2=1, x3=2, x4=3;
        int u1=4, u2=5, u3=6, u4=7;

        StructuralCausalModel m0 = Party.buildModel();

        TIntIntHashMap evidence = new TIntIntHashMap();
        evidence.put(x2, 1);
        int target = x4;


        m0.getEmpiricalProbs();

        CredalCausalVE inf0 = new CredalCausalVE(m0);
        inf0.query(target,evidence); //[0.887445652173913, 0.11255434782608696]  right

        CredalCausalVE inf1 = new CredalCausalVE(m0); //
        inf1.query(target,evidence); // K(vars[3]|[]) [0.887445652173913, 0.11255434782608696]

        /////////////////

        // credal model

        SparseModel m1 = inf1.getModel();

        /////////////////

        //single credal model from scratch

        SparseModel m2 = new SparseModel();

        // define the variables (endogenous and exogenous)
        x1 = m2.addVariable(2);
        x2 = m2.addVariable(2);
        x3 = m2.addVariable(2);
        x4 = m2.addVariable(2);

        u1 = m2.addVariable(2);
        u2 = m2.addVariable(4);
        u3 = m2.addVariable(4);
        u4 = m2.addVariable(3);


        m2.addParents(x1, u1);
        m2.addParents(x2, u2, x1);
        m2.addParents(x3, u3, x1);
        m2.addParents(x4, u4, x2, x3);


        // define the factors U
        BayesianToVertex conv = new BayesianToVertex();
        VertexFactor pu1 = conv.apply(new BayesianFactor(m2.getDomain(u1), new double[] { .4, .6 }), u1);
        VertexFactor pu4 = conv.apply(new BayesianFactor(m2.getDomain(u4), new double[] { .05, .9, .05 }),u4);

        VertexFactor pu2 = new VertexFactor(m2.getDomain(u2), Strides.empty());
        pu2.addVertex(new double[]{0.1, 0.87, 0.0, 0.03},0);
        pu2.addVertex(new double[]{0.07, 0.9, 0.03, 0.0},0);

        VertexFactor pu3 = new VertexFactor(m2.getDomain(u3), Strides.empty());
        pu3.addVertex(new double[]{0.05, 0.0, 0.85, 0.1},0);
        pu3.addVertex(new double[]{0.0, 0.05, 0.9, 0.05},0);


        m2.setFactor(u1,pu1);
        m2.setFactor(u2,pu2);
        m2.setFactor(u3,pu3);
        m2.setFactor(u4,pu4);

        // define factors X

        VertexFactor f1 = conv.apply(BayesianFactor.deterministic(m2.getDomain(x1), m2.getDomain(u1),0,1),x1);

        VertexFactor f2 = VertexFactor.deterministic(m2.getDomain(x2), m2.getDomain(m2.getParents(x2)),
                0,0, 0,1, 1,0, 1,1);
        //conv.apply(BayesianFactor.deterministic(m2.getDomain(x2), m2.getDomain(m2.getParents(x2)),
        //0,0, 0,1, 1,0, 1,1),x2);
        //0,0,1,1,  0,1,0,1),x2); //todo: bug in the conversion

        VertexFactor f3 = VertexFactor.deterministic(m2.getDomain(x3), m2.getDomain(m2.getParents(x3)),
                0,0, 0,1, 1,0, 1,1);

        VertexFactor f4 = VertexFactor.deterministic(m2.getDomain(x4), m2.getDomain(m2.getParents(x4)),
                0,0,0,0, 1,0,0,1, 1,0,0,1);
//    0,1,1,  0,0,0,  0,0,0, 0,1,1),x4);


        m2.setFactor(x1,f1);
        m2.setFactor(x2,f2);
        m2.setFactor(x3,f3);
        m2.setFactor(x4,f4);




        /////////////////

        //twin credal model from scratch

        SparseModel m3 = new SparseModel();

        // define the variables (endogenous and exogenous)
        x1 = m3.addVariable(2);
        x2 = m3.addVariable(2);
        x3 = m3.addVariable(2);
        x4 = m3.addVariable(2);

        int x1_ = m3.addVariable(2);
        int x2_ = m3.addVariable(2);
        int x3_ = m3.addVariable(2);
        int x4_ = m3.addVariable(2);

        u1 = m3.addVariable(2);
        u2 = m3.addVariable(4);
        u3 = m3.addVariable(4);
        u4 = m3.addVariable(3);


        m3.addParents(x1, u1);
        m3.addParents(x2, u2, x1);
        m3.addParents(x3, u3, x1);
        m3.addParents(x4, u4, x2, x3);

        m3.addParents(x1_, u1);
        m3.addParents(x2_, u2, x1_);
        m3.addParents(x3_, u3, x1_);
        m3.addParents(x4_, u4, x2_, x3_);


        // define the factors U
        conv = new BayesianToVertex();
        pu1 = conv.apply(new BayesianFactor(m3.getDomain(u1), new double[] { .4, .6 }), u1);
        pu4 = conv.apply(new BayesianFactor(m3.getDomain(u4), new double[] { .05, .9, .05 }),u4);

        pu2 = new VertexFactor(m3.getDomain(u2), Strides.empty());
        pu2.addVertex(new double[]{0.1, 0.87, 0.0, 0.03},0);
        pu2.addVertex(new double[]{0.07, 0.9, 0.03, 0.0},0);

        pu3 = new VertexFactor(m3.getDomain(u3), Strides.empty());
        pu3.addVertex(new double[]{0.05, 0.0, 0.85, 0.1},0);
        pu3.addVertex(new double[]{0.0, 0.05, 0.9, 0.05},0);


        m3.setFactor(u1,pu1);
        m3.setFactor(u2,pu2);
        m3.setFactor(u3,pu3);
        m3.setFactor(u4,pu4);

        // define factors X

        f1 = conv.apply(BayesianFactor.deterministic(m3.getDomain(x1), m3.getDomain(u1),0,1),x1);

        f2 = VertexFactor.deterministic(m3.getDomain(x2), m3.getDomain(m3.getParents(x2)),
                0,0, 0,1, 1,0, 1,1);


        f3 = VertexFactor.deterministic(m3.getDomain(x3), m3.getDomain(m3.getParents(x3)),
                0,0, 0,1, 1,0, 1,1);

        f4 = VertexFactor.deterministic(m3.getDomain(x4), m3.getDomain(m3.getParents(x4)),
                0,0,0,0, 1,0,0,1, 1,0,0,1);


        m3.setFactor(x1,f1);
        m3.setFactor(x2,f2);
        m3.setFactor(x3,f3);
        m3.setFactor(x4,f4);





        VertexFactor f1_ = conv.apply(BayesianFactor.deterministic(m3.getDomain(x1_), m3.getDomain(u1),0,1),x1_);

        VertexFactor f2_ = VertexFactor.deterministic(m3.getDomain(x2_), m3.getDomain(x1_,u2),
                0,0, 0,1, 1,0, 1,1);


        VertexFactor f3_ = VertexFactor.deterministic(m3.getDomain(x3_), m3.getDomain(x1_,u3),
                0,0, 0,1, 1,0, 1,1);

        VertexFactor f4_ = VertexFactor.deterministic(m3.getDomain(x4_), m3.getDomain(x2_,x3_,u4),
                0,0,0,0, 1,0,0,1, 1,0,0,1);


        m3.setFactor(x1_,f1_);
        m3.setFactor(x2_,f2_);
        m3.setFactor(x3_,f3_);
        m3.setFactor(x4_,f4_);

        m3.correctFactorDomains();
        m3.inspectFactorDomains();
        ///////////


        // P(X4' | X2'=1, X2=0)

        target = x4_;
        evidence = new TIntIntHashMap();
        evidence.put(x2,0);
        TIntIntHashMap intervention = new TIntIntHashMap();
        intervention.put(x2_,1);

        SparseModel do_csmodel = CausalOps.intervention(m3, intervention.keys()[0], intervention.values()[0]);

        // cut arcs coming from an observed node and remove barren w.r.t the target
        RemoveBarren removeBarren = new RemoveBarren();
        do_csmodel = removeBarren
                .execute(new CutObserved().execute(do_csmodel, evidence), target, evidence);

        System.out.println(Arrays.toString(removeBarren.getDeleted()));

        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: evidence.keys()){
            if(ArrayUtils.contains(do_csmodel.getVariables(), v)){
                filteredEvidence.put(v, evidence.get(v));
            }
        }

        // Get the new elimination order
        int[] newElimOrder = do_csmodel.getVariables();

        FactorVariableElimination ve = new FactorVariableElimination(newElimOrder);
        ve.setEvidence(filteredEvidence);
        ve.setNormalize(false);
        VertexFactor.CONVEX_HULL_MARG = true;
        ve.setFactors(do_csmodel.getFactors());
        VertexFactor result = ((VertexFactor)ve.run(target)).normalize();


        System.out.println("P(X4' | X2'=1, X2=0):");
        System.out.println(result);


    }
}
