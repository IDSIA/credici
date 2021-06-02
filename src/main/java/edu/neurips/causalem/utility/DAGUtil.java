package edu.neurips.causalem.utility;

import edu.neurips.causalem.model.builder.CausalBuilder;
import edu.neurips.causalem.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;


public class DAGUtil {

    private static int[] nodesOperation(BinaryOperator<int[]> func, SparseDirectedAcyclicGraph... DAGs) {
        int[] out = DAGs[0].getVariables();
        for (int i = 1; i < DAGs.length; i++)
            out = func.apply(out, DAGs[i].getVariables());
        return out;

    }

    public static int[] nodesIntersection(SparseDirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::intersection, DAGs);
    }

    public static int[] nodesDifference(SparseDirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::difference, DAGs);
    }

    public static boolean isContained(SparseDirectedAcyclicGraph subDAG, SparseDirectedAcyclicGraph DAG) {
        for (int x : subDAG.getVariables()) {
            for (int y : subDAG.getChildren(x)) { // x -> y
                if (!DAG.containsEdge(x, y))
                    return false;
            }
        }
        return true;
    }


    public static SparseDirectedAcyclicGraph getSubDAG(SparseDirectedAcyclicGraph dag, int... nodes) {
        int[] toremove = ArraysUtil.difference(dag.getVariables(), nodes);
        SparseDirectedAcyclicGraph out = dag.copy();
        for (int v : toremove) {
            out.removeVariable(v);
        }
        return out;
    }


    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3, 0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x, y), new double[]{0.6, 0.5, 0.5, 0.5}));


        //int u1=2, u2=3;

        //EquationBuilder.fromVector(Strides.as(y,2))

        StructuralCausalModel model = CausalBuilder.of(bnet).setFillRandomExogenousFactors(3).build();

        CausalBuilder b = CausalBuilder.of(bnet).setFillRandomExogenousFactors(3);
        b.build();

        String str = Arrays.toString(
                nodesDifference(b.getCausalDAG(), b.getEmpiricalDAG())
        );

        System.out.println(str);

    }

    public static int[] getTopologicalOrder(DirectedAcyclicGraph dag, int... nodes) {

        if (nodes.length == 0)
            nodes = Arrays.asList(dag.vertexSet().toArray())
                    .stream().mapToInt(x -> ((Integer) x).intValue())
                    .toArray();

        int[] finalNodes = nodes;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new TopologicalOrderIterator<Integer, DefaultEdge>(dag), Spliterator.ORDERED
                ),
                false)
                .filter(x -> ArraysUtil.contains(x, finalNodes))
                .mapToInt(x -> ((Integer) x).intValue()).toArray();
    }


    public static SparseDirectedAcyclicGraph randomDag(int numNodes) {
        Random r = RandomUtil.getRandom();

        if (numNodes < 2)
            throw new IllegalArgumentException("The minumum number of nodes is 2.");

        SparseDirectedAcyclicGraph dag = new SparseDirectedAcyclicGraph();
        for (int i = 0; i < numNodes; i++) {
            dag.addVariable(i);
        }

        // number of sets in the top. order
        int numSets = r.nextInt(numNodes - 1) + 2;

        // gather all the nodes such as no empty sets

        List nodes =
                IntStream.range(0, numNodes)                      // <-- creates a stream of ints
                        .boxed()                                // <-- converts them to Integers
                        .collect(Collectors.toList());          // <-- collects the values to a list

        Collections.shuffle(nodes);

        int sets[][] = new int[numSets][];

        for (int s = 0; s < numSets; s++) {
            int setSize = 0;
            if (s == numSets - 1)
                setSize = nodes.size();
            else
                setSize = r.nextInt(nodes.size() - (numSets - s - 1)) + 1;
            sets[s] = new int[setSize];
            for (int i = 0; i < setSize; i++) {
                sets[s][i] = (int) nodes.remove(0);
            }
        }


        // set minimal outgoing arcs
        for (int s = 0; s < numSets - 1; s++) {
            for (int i : sets[s]) {
                // random node from the next set
                int j = sets[s + 1][r.nextInt(sets[s + 1].length)];
                dag.addLink(i, j);
            }
        }


        // set minimal ingoing arcs
        for(int s=1; s<numSets; s++) {
            for(int i : sets[s]){
                if(dag.getParents(i).length==0) {
                    // random node from the previous set
                    int j = sets[s-1][r.nextInt(sets[s-1].length)];
                    dag.addLink(j,i);
                }
            }
        }

        return dag;
    }


    public static Graph moral(SparseDirectedAcyclicGraph dag){

        Graph moral = new DefaultUndirectedGraph(DefaultEdge.class);
        for(int x : dag.getVariables())
            moral.addVertex(x);

        for(int x: dag.getVariables()){
            for(int y: dag.getParents(x)){
                moral.addEdge(y,x);
                for(int y2: dag.getParents(x)){
                    if(y!=y2 && !moral.containsEdge(y,y2))
                        moral.addEdge(y, y2);
                }
            }
        }
        return moral;
    }

    public static  List<int[]> connectComponents(Graph g){
        ConnectivityInspector connectInspect = new ConnectivityInspector(g);
        List connectedComponentList = connectInspect.connectedSets();
        return (List<int[]>) connectedComponentList.stream().map(s -> Ints.toArray((Set)s)).collect(Collectors.toList());
    }


}
