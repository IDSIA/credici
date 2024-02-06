package ch.idsia.credici.model.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.random.SobolSequenceGenerator;
import org.apache.commons.math3.util.FastMath;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.credici.utility.table.Table;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.CombinationsIterator;

/**
 * Convert a model into its Connected components submodels.
 * The connected compoenents will have in their model also all the endogenous parents of 
 * the endogenous variables. When querying P(U,e) one needs to remember that e includes 
 * this parents.  
 */
public class CComponents {
	private static final String CC_KEY = "CC-Key";
	
	AtomicInteger modelCounter; 
	
    public CComponents() { 
    	modelCounter = new AtomicInteger();
    }

    private StructuralCausalModel from; 
    
  
    public List<StructuralCausalModel> apply(StructuralCausalModel model) {
        var res = apply(model, null);
        return res.stream().map(Pair<StructuralCausalModel, DoubleTable>::getLeft).collect(Collectors.toList());
    }

    /** 
     * Transform the given {@link StructuralCausalModel} and Data {@link Table} into a list of 
     * CComponents with their part of the data.
     * 
     * The method also initializes a synchronized map for the results.
     * Which will be lists of Fully specified {@link StructuralCausalModel}
     * 
     * @param model the source model 
     * @param data the observation table
     * @return a list of model-table pairs.
     */
    public List<Pair<StructuralCausalModel, DoubleTable>> apply(StructuralCausalModel model, DoubleTable data) {
        results = Collections.synchronizedMap(new HashMap<>());

        from = model;
        Set<Integer> exo = IntStream.of(model.getExogenousVars()).boxed().collect(Collectors.toSet());

        Set<Integer> endo = IntStream.of(model.getVariables()).boxed().collect(Collectors.toSet());
        endo.removeAll(exo);

        Set<Integer> visited = new HashSet<>();

        List<Pair<StructuralCausalModel, DoubleTable>> models = new ArrayList<>();
        
        for (Integer exoVar : exo) {
            
            // already visited
            if (visited.contains(exoVar)) continue;

            var vars = collectVariables(exoVar, model, exo);
    
            // now create and copy
            StructuralCausalModel newmodel = createModel(vars, model, data);
            
            int[] exoVars = newmodel.getExogenousVars();
            visited.addAll(Arrays.stream(exoVars).boxed().collect(Collectors.toSet()));
            
            // filter the data table
            int[] endoVars = newmodel.getEndogenousVars(true);
            
            if (data != null) {
                models.add(Pair.of(newmodel, data.subtable(endoVars)));
            } else {
                models.add(Pair.of(newmodel, null));
            }
        }

        return models;
    }


    private Map<Integer, List<StructuralCausalModel>> results;


    
    public synchronized void addResult(StructuralCausalModel model) {
    	Integer k = (Integer) model.getData(CC_KEY);
    	if (k == null) { throw new IllegalStateException(); }
        results.compute(k, (key, value) -> {
        	if (value == null) {
        		value = new ArrayList<StructuralCausalModel>();
        	}
        	value.add(model);
        	return value;
        });
    }


    /**
     * Restore the original model from a collection of CComponents.
     * This will copy all CComponent endogenous and exogenous factors into the target model.
     * The structure of the target model will be the original one.
     * 
     * @param models
     * @return
     */
    public StructuralCausalModel revert(Collection<StructuralCausalModel> models) {
        StructuralCausalModel target = from.copy();
        for (StructuralCausalModel scm : models) {
            for (int exo : scm.getExogenousVars()) {
                target.setFactor(exo, scm.getFactor(exo));
            }
            for (int endo : scm.getEndogenousVars()) {
            	target.setFactor(endo, scm.getFactor(endo));
            }
        }
        return target;
    }

    /**
     * Create the expanded CComponent model.
     * 
     * @param newExo the list of exogenous variables in the component
     * @param newEndo the list of endogenous variables in the component
     * @param spurious the additional x variable in the ccomponent
     * @param model the original model where we take equations, priors and var info.
     * 
     * @return a new SCM with for the component
     */
    private StructuralCausalModel createModel(Triple<Set<Integer>, Set<Integer>, Set<Integer>> vars,  StructuralCausalModel model, DoubleTable dataset) {
    	
        var newExo = vars.getLeft(); 
        var newEndo = vars.getMiddle();
        var spurious = vars.getRight();

        Integer name = modelCounter.incrementAndGet();
        results.put(name, new ArrayList<>());

        StructuralCausalModel newmodel = new StructuralCausalModel("" + name);
        newmodel.setData(CC_KEY, name);
        
        // same random source
        // newmodel.setRandomSource(model.getRandomSource());

        for (int newExoVar : newExo) {
            newmodel.addVariable(newExoVar, model.getSize(newExoVar), true);
            
            var factor = model.getFactor(newExoVar);
            if (factor != null) newmodel.setFactor(newExoVar, factor.copy());
        }

        // visit all children, but put in open only their exogenous parents
        Map<Integer, Set<Integer>> parenting = new HashMap<>();
        for (int newEndoVar : newEndo) {
            // non exogenous nor endogenous parents of the newEndoVar
            Set<Integer> parents = toSet(model.getParents(newEndoVar));
            parents.removeAll(newExo);
            parents.removeAll(newEndo);
            parenting.put(newEndoVar, parents);

            // in the inference model we need also parents of endogenous nodes
            newmodel.addVariable(newEndoVar, model.getSize(newEndoVar), false);
            spurious.addAll(parents);
            for (int p:parents) {
                newmodel.addVariable(p, model.getSize(p), StructuralCausalModel.VarType.DEPENDENCY);
            }
        }

        // add parenting and factors
        for (int newEndoVar : newEndo) {
            newmodel.addParents(newEndoVar, model.getParents(newEndoVar).clone());
            var factor = model.getFactor(newEndoVar);
            if (factor != null) newmodel.setFactor(newEndoVar, factor.copy());
        }

        // additional x
        for (int x : spurious) {
            
        	Set<Integer> domain = Arrays.stream(model.getParents(x)).boxed().collect(Collectors.toSet());
        	domain.retainAll(spurious);
        	domain.add(x);
        	
            int[] dom_vars = domain.stream().mapToInt(i->i).sorted().toArray();
            newmodel.addParents(x, ArraysUtil.removeAllFromSortedArray(dom_vars, x));
            
            if (dataset != null) {
	            // interesting parents 
	            Strides s = model.getDomain(dom_vars);
	            double[] marginal = dataset.getWeights(s.getVariables(), s.getSizes());
	            
	            var factor = model.getFactor(x);
	            boolean log = factor != null ? factor.isLog() : false;
	            if (log) {
	            	marginal = Arrays.stream(marginal).map(FastMath::log).toArray();
	            }
	            
	            BayesianFactor f = new BayesianFactor(s, marginal, log);
	            f = f.normalize();
	            newmodel.setFactor(x, f);
            }
        }
        return newmodel;
    }

    /**
     * Collect all endogenous and exogenous variables from the model starting from u
     * @param model
     * @return
     */
    private Triple<Set<Integer>, Set<Integer>, Set<Integer>> collectVariables(int u, StructuralCausalModel model, Set<Integer> exo) {
        
        Deque<Integer> open = new LinkedList<>();
        open.add(u);
        
        Set<Integer> newExo = new HashSet<>();
        Set<Integer> newEndo = new HashSet<>();

        // the set of variables that are not part of the ccomponent but
        // are needed to be able to observe them 
        Set<Integer> spurious = new HashSet<>();
        
        while(!open.isEmpty()){
            int currentExo = open.pop();
            newExo.add(currentExo);

            Set<Integer> children = toSet(model.getChildren(currentExo));
            newEndo.addAll(children);

            for (Integer endoVar : children) {
                Set<Integer> p = toSet(model.getParents(endoVar));
                // only exogenous
                p.retainAll(exo);

                // already visited?
                p.removeAll(newExo);

                open.addAll(p);
            }
        }

        return Triple.of(newExo, newEndo, spurious);
    }

    private static Set<Integer> toSet(int[] data) {
        return IntStream.of(data).boxed().collect(Collectors.toSet());
    }


    /** 
     * Re-compose the results into complete models connecting the CComponents again. 
     * FSCM of the different CCompoents can be reunited at will and do not have
     * to be paired. This strategy will iterate over combinations using 
     * a sobol sampling.
     * @return an interator over full and Fully specified {@link StructuralCausalModel}s.
     */
    public Iterator<StructuralCausalModel> sobolIterator() {
        
        return new Iterator<StructuralCausalModel>() {
            SobolSequenceGenerator generator = new SobolSequenceGenerator(results.size());    

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public StructuralCausalModel next() {
                double[] sobol = generator.nextVector();
                ArrayList<StructuralCausalModel> sample = new ArrayList<>();
                int offset = 0;
                for (var item : results.entrySet()) {
                    List<StructuralCausalModel> nets = item.getValue();
                    int value = (int)(nets.size() * sobol[offset++]);
                    sample.add(nets.get(value));
                }
                return revert(sample);
            }
        };
    }

    public long combinations() {
    	long comb = 1;
    	for (var entry : results.entrySet()) {
    		comb *= entry.getValue().size();
    	}
    	return comb;
    }
    
    public void simplify() {
    	for (var entry : results.entrySet()) {
        	Set<StructuralCausalModel> s = new HashSet<>(entry.getValue());
        	entry.getValue().clear();
        	entry.getValue().addAll(s);
    	}
    }

    /** 
     * Re-compose the results into complete models connecting the CComponents again. 
     * FSCM of the different CCompoents can be reunited at will and do not have
     * to be paired. This strategy will iterate over all possible combinations of 
     * the different components' FSCM.
     * @return an interator over full and Fully specified {@link StructuralCausalModel}s.
     */
    public Iterator<StructuralCausalModel> exaustiveIterator() {
        final Collection<Collection<StructuralCausalModel>> items = results.entrySet().stream()
        .map(a -> {
        	return (Collection<StructuralCausalModel>) a.getValue(); 
        }).collect(Collectors.toList());
        
        final CombinationsIterator<StructuralCausalModel> iter = new CombinationsIterator<>(items);

        
        return new Iterator<StructuralCausalModel>() {

            
        
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public StructuralCausalModel next() {
                var current = iter.next();
                return revert(current);
            }
        };
    }

    /** 
     * Re-compose the results into complete models connecting the CComponents again. 
     * FSCM of the different CCompoents can be reunited at will and do not have
     * to be paired. This strategy will iterate over combinations using 
     * the original strategy of visiting components paired. 
     * 
     * @return an interator over full and Fully specified {@link StructuralCausalModel}s.
     */
    public Iterator<StructuralCausalModel> alignedIterator() {
        int max = results.values().stream().mapToInt(List::size).max().getAsInt();
        return new Iterator<StructuralCausalModel>() {
            int index = 0;
            @Override
            public boolean hasNext() {
                return index < max;
            }
            @Override
            public StructuralCausalModel next() {
                List<StructuralCausalModel> models = new ArrayList<>();
                for (var list : results.values()) {
                    models.add(list.get(index % max));
                }
                ++index;
                return revert(models);
            }
        };
    }
}
