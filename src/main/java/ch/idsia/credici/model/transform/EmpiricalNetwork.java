package ch.idsia.credici.model.transform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import ch.idsia.credici.Table;
import ch.idsia.credici.collections.FIntHashSet;
import ch.idsia.credici.collections.FIntObjectHashMap;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.TopologicalOrder;
import ch.idsia.crema.inference.ve.order.TopologicalOrder2;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.impl.hash.TIntHash;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class EmpiricalNetwork {

    private double ll; 

    public BayesianNetwork apply2(StructuralCausalModel model, Table data) {
        var bn = new BayesianNetwork();
        TIntObjectMap<TIntSet> varcc = new FIntObjectHashMap<>();
        TIntObjectMap<TIntSet> Y = new FIntObjectHashMap<>();

        for (int variable : model.getEndogenousVars()) {    
            bn.addVariable(variable, model.getSize(variable));
            Y.put(variable, new FIntHashSet(model.getEndogenousParents(variable)));

            int[] parents = model.getExogenousParents(variable);
            int[] endoch = model.getEndogenousChildren(parents);

            TIntSet component = new FIntHashSet();
            if (varcc.containsKey(variable)) 
                component.addAll(varcc.get(variable));
                varcc.put(variable, component);
            component.addAll(endoch);
            for (var p : endoch) {
                if (varcc.containsKey(p)) {
                    component.addAll(varcc.get(p));
                }
                varcc.put(p, component);
            }
        }

        Set<TIntSet> cc = new HashSet<>(varcc.valueCollection());
        for (TIntSet component : cc) {
            //
        }
        return bn;
    }


    public BayesianNetwork apply(StructuralCausalModel model, Table data) {
        

        var bn = new BayesianNetwork();
        
        TIntSet exo = new FIntHashSet(model.getExogenousVars());
        TIntList open = new TIntArrayList();

        TIntObjectMap<TIntSet> source_parents = new FIntObjectHashMap<>();
        TIntObjectMap<TIntSet> target_parents = new FIntObjectHashMap<>();


        for (int variable : model.getEndogenousVars()) {    
            bn.addVariable(variable, model.getSize(variable));
            
            var par = new FIntHashSet(model.getParents(variable));
            par.removeAll(exo);
            
            // no endogenous parents (all exogenous parents)
            if (par.isEmpty()) open.add(variable);
            else {
                source_parents.put(variable, par);
            }
            target_parents.put(variable, new FIntHashSet(par));
        }


        // parent of a node are the children 
        // of the exogenous parents 
        // of the children 
        while(!open.isEmpty()) {
            int current = open.iterator().next();
            open.remove(current);

            int[] curr_children = model.getChildren(current);
            for (int child : curr_children) {
                var parents = source_parents.get(child);
                parents.remove(current);

                if (parents.isEmpty()) 
                    open.add(child);
            }

            int[] exoparent = model.getExogenousParents(curr_children);
            int[] children = model.getEndogenousChildren(exoparent);
            for (int child : children) {
                target_parents.get(child).add(current);
            }
        }

        TIntIntMap[] imap = data.convert();
        
        target_parents.forEachEntry((int k, TIntSet v) -> {
            int[] p = v.toArray();
            Arrays.sort(p);
            bn.addParents(k, p);

            BayesianFactor bf = DataUtil.getCondProb(imap, bn.getDomain(k), bn.getDomain(p));
            bn.setFactor(k, bf);
            return true;
        });

        // log likelihood 
        ll = 0;
        for (TIntIntMap ev : imap) {
            for (int variable : bn.getVariables()) {
                var bf = bn.getFactor(variable);
                int[] states = Arrays.stream(bf.getDomain().getVariables()).map(v->ev.get(v)).toArray();
                double p = FastMath.log(bf.getValue(states));
                ll+=p;
            }
        }
        return bn;
    }

    public double getLl() {
        return ll;
    }
}
