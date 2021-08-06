package ch.idsia.credici.utility;

import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
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

import org.jgrapht.graph.DirectedAcyclicGraph;



public class DAGUtil {

    private static int[] nodesOperation(BinaryOperator<int[]> func, DirectedAcyclicGraph... DAGs) {
        int[] out = getVariables(DAGs[0]);
        for (int i = 1; i < DAGs.length; i++)
            out = func.apply(out, getVariables(DAGs[i]));
        return out;

    }

    public static int[] nodesIntersection(DirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::intersection, DAGs);
    }

    public static int[] nodesDifference(DirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::difference, DAGs);
    }

    public static boolean isContained(DirectedAcyclicGraph subDAG, DirectedAcyclicGraph DAG) {
        for (int x : getVariables(subDAG)) {
            for (int y : getChildren(subDAG, x)) { // x -> y
                if (!DAG.containsEdge(x, y))
                    return false;
            }
        }
        return true;
    }


    public static DirectedAcyclicGraph getSubDAG(DirectedAcyclicGraph dag, int... nodes) {
        int[] toremove = ArraysUtil.difference(getVariables(dag), nodes);
        DirectedAcyclicGraph out = (DirectedAcyclicGraph) dag.clone();
        for (int v : toremove) {
            out.removeVertex(v);
        }
        return out;
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


    public static DirectedAcyclicGraph randomDag(int numNodes) {
        Random r = RandomUtil.getRandom();

        if (numNodes < 2)
            throw new IllegalArgumentException("The minumum number of nodes is 2.");

        DirectedAcyclicGraph dag = new DirectedAcyclicGraph(DefaultEdge.class);
        for (int i = 0; i < numNodes; i++) {
            dag.addVertex(i);
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
                dag.addEdge(i, j);
            }
        }


        // set minimal ingoing arcs
        for(int s=1; s<numSets; s++) {
            for(int i : sets[s]){
                if(getParents(dag,i).length==0) {
                    // random node from the previous set
                    int j = sets[s-1][r.nextInt(sets[s-1].length)];
                    dag.addEdge(j,i);
                }
            }
        }

        return dag;
    }


    public static Graph moral(DirectedAcyclicGraph dag){

        Graph moral = new DefaultUndirectedGraph(DefaultEdge.class);
        for(int x : getVariables(dag))
            moral.addVertex(x);

        for(int x: getVariables(dag)){
            for(int y: getParents(dag, x)){
                moral.addEdge(y,x);
                for(int y2: getParents(dag, x)){
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

    public static int[] getVariables(DirectedAcyclicGraph dag){
        return dag.vertexSet().stream().mapToInt(i->(int)i).toArray();
    }

    public static int[] getChildren(DirectedAcyclicGraph dag, int x){
        return dag.getDescendants(x).stream().mapToInt(i->(int)i).toArray();
    }

    public static int[] getParents(DirectedAcyclicGraph dag, int x){
        return dag.getAncestors(x).stream().mapToInt(i->(int)i).toArray();
    }
}
