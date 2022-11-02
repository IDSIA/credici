package ch.idsia.credici.model.tools;

import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.ChordalGraphMaxCliqueFinder;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CausalGraphTools {
    public static SparseDirectedAcyclicGraph getExogenousDag(SparseDirectedAcyclicGraph dag){
        //getExogenousDAG
        int[] endoVars = Arrays.stream(dag.getVariables()).filter(v -> dag.getParents(v).length!=0).toArray();

        SparseDirectedAcyclicGraph subDag = dag.copy();
        for(int x : endoVars){
            for(int y: dag.getParents(x)){
                if(dag.getParents(y).length>0) {
                    subDag.removeLink(y, x);
                }
            }
        }
        return subDag;
    }

    public static int getExogenousTreewidth(DirectedAcyclicGraph dag){
        Graph moral = DAGUtil.moral(getExogenousDag((SparseDirectedAcyclicGraph) dag));
        return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() - 1;

    }

    public static SparseDirectedAcyclicGraph makeValidCausalDAG(SparseDirectedAcyclicGraph dag) {
        SparseDirectedAcyclicGraph newDag = dag.copy();

        int[] internal = DAGUtil.getNonRootNodes(dag);
        int[] roots = DAGUtil.getRootNodes(dag);

        // Get the problematic nodes
        int[] toFixNodes = IntStream.of(internal)
                .filter(x -> ArraysUtil.intersection(dag.getParents(x), roots).length == 0)
                .toArray();

        int nextID = Arrays.stream(dag.getVariables()).max().getAsInt() + 1;
        for(int x : toFixNodes) {
            newDag.addVariable(nextID);
            newDag.addLink(nextID, x);
            nextID++;
        }

        return newDag;
    }

    public static boolean isValidCausalDAG(SparseDirectedAcyclicGraph dag) {
        //All non-root nodes have at least a parent that is root
        int[] internal = DAGUtil.getNonRootNodes(dag);
        int[] roots = DAGUtil.getRootNodes(dag);
        return IntStream.of(internal)
                .allMatch(x -> ArraysUtil.intersection(dag.getParents(x), roots).length > 0);

    }

    public static int[] getExogenous(SparseDirectedAcyclicGraph dag){
        return DAGUtil.getRootNodes(dag);
    }

    public static int[] getEndogenous(SparseDirectedAcyclicGraph dag){
        return DAGUtil.getNonRootNodes(dag);
    }


   public static boolean isExogenous(SparseDirectedAcyclicGraph dag, int v){
       return dag.getParents(v).length==0;
   }
   // We assume that the DAG is valid for SCMs
   public static boolean isEndogenous(SparseDirectedAcyclicGraph dag, int v){
       return !isExogenous(dag, v);
   }

    public static int[] getEndogenousParents(SparseDirectedAcyclicGraph dag, int v){
        return IntStream.of(dag.getParents(v)).filter( x -> isEndogenous(dag, x)).toArray();
    }
    public static int[] getEndogenousChildren(SparseDirectedAcyclicGraph dag, int v){
        return IntStream.of(dag.getChildren(v)).filter( x -> isEndogenous(dag, x)).toArray();
    }
    public static int[] getExogenousParents(SparseDirectedAcyclicGraph dag, int v){
        return IntStream.of(dag.getParents(v)).filter( x -> isExogenous(dag, x)).toArray();
    }
    public static int[] getExogenousChildren(SparseDirectedAcyclicGraph dag, int v){
        return IntStream.of(dag.getChildren(v)).filter( x -> isExogenous(dag, x)).toArray();
    }


   public static boolean isMarkovian(SparseDirectedAcyclicGraph dag){
        return IntStream.of(getExogenous(dag)).allMatch(u -> getEndogenousChildren(dag, u).length==1);
   }

   public static boolean isQuasiMarkovian(SparseDirectedAcyclicGraph dag){
       return IntStream.of(getEndogenous(dag)).allMatch(x -> getExogenousParents(dag, x).length==1);
   }

   public static boolean isNonQuasiMarkovian(SparseDirectedAcyclicGraph dag){
       return IntStream.of(getEndogenous(dag)).anyMatch(x -> getExogenousParents(dag, x).length>1);
   }

   public static int getMarkovianity(SparseDirectedAcyclicGraph dag){
        if (isMarkovian(dag)) return 0;
        if (isQuasiMarkovian(dag)) return 1;
        return 2;
   }

   public static List<int[]> exoConnectComponents(SparseDirectedAcyclicGraph dag){
        return DAGUtil.connectComponents(getExogenousDag(dag))
                .stream()
                .map(c -> IntStream.of(c).filter(v -> isExogenous(dag, v)).toArray())
                .collect(Collectors.toList());
   }

    public static List<int[]> endoConnectComponents(SparseDirectedAcyclicGraph dag){
        return DAGUtil.connectComponents(getExogenousDag(dag))
                .stream()
                .map(c -> IntStream.of(c).filter(v -> isEndogenous(dag, v)).toArray())
                .collect(Collectors.toList());
    }

    public static boolean isCofounded(SparseDirectedAcyclicGraph dag, int x){
        for(int u : getExogenousParents(dag, x)){
            if(getEndogenousChildren(dag, u).length>1)
                return true;
        }
        return false;
    }
    public static boolean isNotCofounded(SparseDirectedAcyclicGraph dag, int x) {
        return !isCofounded(dag, x);
    }

    public static int[] getCofoundedVars(SparseDirectedAcyclicGraph dag){
       return IntStream.of(getEndogenous(dag)).filter( x -> isCofounded(dag, x)).toArray();
    }

    public static int[] getNonCofoundedVars(SparseDirectedAcyclicGraph dag){
        return ArraysUtil.difference(getEndogenous(dag), getCofoundedVars(dag));
    }

    public static void main(String[] args) {
        SparseDirectedAcyclicGraph dag = DAGUtil.build("(0,1),(1,2)");
        System.out.println(dag);
        System.out.println(isValidCausalDAG(dag));
        System.out.println(makeValidCausalDAG(dag));

    }



}
