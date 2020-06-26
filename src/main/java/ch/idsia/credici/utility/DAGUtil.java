package ch.idsia.credici.utility;

import ch.idsia.credici.model.SCMBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;

import java.util.Arrays;
import java.util.function.BinaryOperator;


public class DAGUtil {

    private static int[] nodesOperation(BinaryOperator<int[]> func, SparseDirectedAcyclicGraph... DAGs){
        int[] out = DAGs[0].getVariables();
        for(int i=1; i<DAGs.length; i++)
            out = func.apply(out, DAGs[i].getVariables());
        return out;

    }
    public static int[] nodesIntersection(SparseDirectedAcyclicGraph... DAGs){
        return nodesOperation(ArraysUtil::intersection, DAGs);
    }
    public static int[] nodesDifference(SparseDirectedAcyclicGraph... DAGs){
        return nodesOperation(ArraysUtil::difference, DAGs);
    }

    public static boolean isContained(SparseDirectedAcyclicGraph subDAG, SparseDirectedAcyclicGraph DAG){
        for(int x : subDAG.getVariables()){
            for(int y : subDAG.getChildren(x)){ // x -> y
                if(!DAG.containsEdge(x,y))
                    return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[]{0.6,0.5, 0.5,0.5}));


        //int u1=2, u2=3;

        //EquationBuilder.fromVector(Strides.as(y,2))

        StructuralCausalModel model =  SCMBuilder.of(bnet).setFillRandomExogenousFactors(3).build();

        SCMBuilder b = SCMBuilder.of(bnet).setFillRandomExogenousFactors(3);
        b.build();

        String str = Arrays.toString(
        nodesDifference(b.getCausalDAG(), b.getEmpiricalDAG())
        );

        System.out.println(str);



    }


}
