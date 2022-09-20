package ch.idsia.credici.model.tools;

import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import org.apache.commons.lang3.NotImplementedException;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.ChordalGraphMaxCliqueFinder;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Arrays;

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
        //todo implement
        return dag;
    }

    public static SparseDirectedAcyclicGraph isValidCausalDAG(SparseDirectedAcyclicGraph dag) {
        throw new NotImplementedException("Not implemented");
    }

    public int[] getExogenous(){
        throw new NotImplementedException("Not implemented");
    }

    public int[] getEndogenous(){
        throw new NotImplementedException("Not implemented");
    }

   public boolean isExogenous(){
       throw new NotImplementedException("Not implemented");
   }
   public boolean isEndogenous(){
       throw new NotImplementedException("Not implemented");
   }

   public boolean isMarkovian(SparseDirectedAcyclicGraph dag){
       throw new NotImplementedException("Not implemented");
   }

   public boolean isQuasiMarkovian(SparseDirectedAcyclicGraph dag){
       throw new NotImplementedException("Not implemented");
   }

   public boolean isNonQuasiMarkovian(SparseDirectedAcyclicGraph dag){
       throw new NotImplementedException("Not implemented");
   }



}
