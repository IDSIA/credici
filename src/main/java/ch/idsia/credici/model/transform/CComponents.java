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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.random.SobolSequenceGenerator;

import ch.idsia.credici.Table;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.CombinationsIterator;

/**
 * Convert a model into its Connected components submodels.
 * The connected compoenents will have in their model also all the endogenous parents of 
 * the endogenous variables. When querying P(U,e) one needs to remember that e includes 
 * this parents.  
 */
public class CComponents implements Iterable<StructuralCausalModel> {

    public CComponents() { 
        initRandom();
    }

    public void initRandom() { 
    }
    
    private StructuralCausalModel from; 

    public List<StructuralCausalModel> apply(StructuralCausalModel model) {
        var res = apply(model, null);
        return res.stream().map(Pair<StructuralCausalModel, Table>::getLeft).collect(Collectors.toList());
    }

    public List<Pair<StructuralCausalModel, Table>> apply(StructuralCausalModel model, Table data) {
        results = Collections.synchronizedMap(new HashMap<>());


        from = model;
        Set<Integer> exo = IntStream.of(model.getExogenousVars()).boxed().collect(Collectors.toSet());

        Set<Integer> endo = IntStream.of(model.getVariables()).boxed().collect(Collectors.toSet());
        endo.removeAll(exo);

        Set<Integer> visited = new HashSet<>();

        List<Pair<StructuralCausalModel, Table>> models = new ArrayList<>();
        
        for (Integer exoVar : exo) {
            
            // already visited
            if (visited.contains(exoVar)) continue;

            var vars = collectVariables(exoVar, model, exo);
    
            // now create and copy
            StructuralCausalModel newmodel = createModel(vars, model);
            
            int[] exoVars = newmodel.getExogenousVars();
            visited.addAll(Arrays.stream(exoVars).boxed().collect(Collectors.toSet()));
            
            // filter the data table
            int[] endoVars = newmodel.getEndogenousVars();
            
            if (data != null) {
                models.add(Pair.of(newmodel, data.subtable(endoVars)));
            } else {
                models.add(Pair.of(newmodel, null));
            }
        }

        return models;
    }


    Map<String, List<StructuralCausalModel>> results;

    public void addResults(String name, List<StructuralCausalModel> models) {
        if (!results.get(name).isEmpty()) { 
            System.out.println("WOW");
        }
        results.put(name, models);
    }

    public void addResult(StructuralCausalModel model) {
        var list = results.get(model.getName());
        list.add(model);
        results.put(model.getName(), list);
    }


    public StructuralCausalModel revert(Collection<StructuralCausalModel> models) {
        StructuralCausalModel target = from.copy();
        for (StructuralCausalModel scm : models) {
            for (int exo : scm.getExogenousVars()) {
                target.setFactor(exo, scm.getFactor(exo));
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
    private StructuralCausalModel createModel(Triple<Set<Integer>, Set<Integer>, Set<Integer>> vars,  StructuralCausalModel model) {

        var newExo = vars.getLeft(); 
        var newEndo = vars.getMiddle();
        var spurious = vars.getRight();

        String name = newExo.toString();
        results.put(name, new ArrayList<>());

        StructuralCausalModel newmodel = new StructuralCausalModel("" + name);
        // same random source
        newmodel.setRandomSource(model.getRandomSource());

        for (int newExoVar : newExo) {
            newmodel.addVariable(newExoVar, model.getSize(newExoVar), true);
            newmodel.setFactor(newExoVar, model.getFactor(newExoVar).copy());
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
                newmodel.addVariable(p, model.getSize(p), false, true);
            }
        }

        // add parenting and factors
        for (int newEndoVar : newEndo) {
            newmodel.addParents(newEndoVar, model.getParents(newEndoVar).clone());
            newmodel.setFactor(newEndoVar, model.getFactor(newEndoVar).copy());
        }

        // additional x
        for (int x : spurious) {
            int size = model.getSize(x);
            double[] dta = new double[size];
            BayesianFactor f = new BayesianFactor(model.getDomain(x), new double[size], true);
            newmodel.setFactor(x, f);
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


    public Iterator<StructuralCausalModel> exaustiveIterator() {
        final Collection<Collection<StructuralCausalModel>> data = results.values().stream().map(a -> {return (Collection<StructuralCausalModel>) a; }).collect(Collectors.toList());
        return new Iterator<StructuralCausalModel>() {

            CombinationsIterator<StructuralCausalModel> iter = new CombinationsIterator<>(data);
        
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


    @Override
    public Iterator<StructuralCausalModel> iterator() {
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
