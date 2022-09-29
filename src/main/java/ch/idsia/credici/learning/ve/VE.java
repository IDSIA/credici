package ch.idsia.credici.learning.ve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.model.math.FactorOperation;
import ch.idsia.crema.model.math.Operation;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class VE<F extends Factor<F>> implements JoinInference<F, F> {

    private int[] sequence;
    private TIntIntMap order;

    private List<F> factors;

    /** network could contain disconnected components lets collect them all*/
    private F output;

    private TIntIntMap instantiation;

    private Operation<F> operator;

    private boolean normalize = true;

    /**
     * Constructs a variable elimination specifying the algebra.
     * Factors, evidence and elimnation sequence must be specified with setters.
     *
     * @param ops
     */
    public VE() {
        this.operator = new FactorOperation<>();
    }

    /**
     * Constructs a variable elimination specifying the algebra to be used for the
     * factors and the elimination order
     *
     * @param ops
     * @param sequence
     */
    public VE(int[] sequence) {
        setSequence(sequence);
        this.operator = new FactorOperation<>();
    }

    /**
     * Set the elimination sequence to be used. Variables will be eliminated in this order.
     * The sequence may include the query!
     * <p>Elimination sequencies can be generated with an {@link OrderingStrategy}.
     * </p>
     *
     * @param sequence
     */
    public void setSequence(int[] sequence) {
        order = new TIntIntHashMap();
        for (int i = 0; i < sequence.length; ++i) {
            order.put(sequence[i], i);
        }

        this.sequence = sequence;
    }

    /**
     * Populate the problem with the factors to be considered.
     * Collection version.
     *
     * @param factors
     */
    public void setFactors(Collection<? extends F> factors) {
        this.factors = new ArrayList<>(factors);
    }

    /**
     * Populate the problem with the factors to be considered.
     * Array version.
     *
     * @param factors
     */
    public void setFactors(F[] factors) {
        this.factors = Arrays.asList(factors);
    }

    /**
     * Fix some query states. The provided argument is a map of variable - state
     * associations.
     *
     * @param instantiation
     */
    public void setInstantiation(TIntIntMap instantiation) {
        this.instantiation = instantiation;
    }


    /**
     * Specify if the resulting value should be normalized.
     * Will result in asking K(Q|e) vs K(Qe)
     *
     * @param norm
     */
    public void setNormalize(boolean norm) {
        normalize = norm;
    }

    /**
     * Execute the variable elimination asking for the marginal or posterior of the specified
     * variables. If multiple variables are specified the joint over the query is computed.
     * <p>
     * <p>
     * The elimination sequence is to be specified via {@link VariableElimination#setSequence(int[])}.
     *
     * @param query
     * @return
     */
    public F run(int... query) {
        // variables should be sorted
        query = ArraysUtil.sort(query);

        FactorQueue<F> queue = new FactorQueue<>(sequence);
        queue.init(factors);
        boolean normalize = false;

        while (queue.hasNext()) {
            int variable = queue.getVariable();
            System.out.println("Var " + variable);
            Collection<F> var_factors = queue.next();

            if (!var_factors.isEmpty()) {
                for (F f : var_factors) {
                   System.out.println(f);
                }
                F last = FactorUtil.combine(operator, var_factors);
                System.out.println("combined: " + last);
                if (instantiation != null && instantiation.containsKey(variable)) {
                    int state = instantiation.get(variable);
                    last = operator.filter(last, variable, state);
                    System.out.println("Filtered " + last);
                }
                if (Arrays.binarySearch(query, variable) >= 0) {
                    // query var // nothing to do
                    System.out.println("Var is target");
                } else {
                    last = operator.marginalize(last, variable);
                    System.out.println("Marginalized " + last);
                }
                queue.add(last);
            }
        }

        Collection<F> res = queue.getResults();
        F last = FactorUtil.combine(operator,res);
        
        if (this.normalize) {
            last = FactorUtil.normalize(operator, last);
        }

        
        return last;
    }


    private int[] union(int[] first, int[]... others) {
        TIntSet set = new TIntHashSet(first);
        for (int[] other : others) {
            set.addAll(other);
        }
        int[] data = set.toArray();
        Arrays.sort(data);
        return data;
    }


    
    @Override
    public F apply(GraphicalModel<F> model, int[] query, TIntIntMap assignement) throws InterruptedException {
        setInstantiation(assignement);
        setFactors(model.getFactors());
        query = union(query, assignement.keys());
        return run(query);
    }

 
    public static void main(String[] args) {
        BayesianNetwork bn = new BayesianNetwork();
        int A = bn.addVariable(2);
        int B = bn.addVariable(2);
        int C = bn.addVariable(2);

        int e1 = bn.addVariable(2);
        int e2 = bn.addVariable(2);

        bn.addParents(e1, A, B);
        bn.addParent(e2, C);

        BayesianFactor bf = new BayesianFactor(bn.getDomain(A,B,e1));
        bf.setValue(0.1, 0,0,0);
        bf.setValue(0.9, 0,0,1);
        bf.setValue(0.2, 0,1,0);
        bf.setValue(0.8, 0,1,1);
        bf.setValue(0.3, 1,0,0);
        bf.setValue(0.7, 1,0,1);
        bf.setValue(0.4, 1,1,0);
        bf.setValue(0.6, 1,1,1);
        bn.setFactor(e1, bf);

        bf = new BayesianFactor(bn.getDomain(A));
        bf.setData(new double[]{0.6,0.4});
        bn.setFactor(A, bf);
        
        bf = new BayesianFactor(bn.getDomain(B));
        bf.setData(new double[]{0.3,0.7});
        bn.setFactor(B, bf);

        bf = new BayesianFactor(bn.getDomain(C,e2));
        bf.setValue(0.25, 0,0);
        bf.setValue(0.75, 0,1);
        bf.setValue(0.75, 1,0);
        bf.setValue(0.25, 1,1);
        bn.setFactor(e2, bf);

        bf = new BayesianFactor(bn.getDomain(C));
        bf.setData(new double[]{0.5,0.5});
        bn.setFactor(C, bf);

        TIntIntHashMap ev = new TIntIntHashMap();
        ev.put(e1, 0);
        ev.put(e2, 0);


        MinFillOrdering mfo = new MinFillOrdering();
		int[] order = mfo.apply(bn);
        
        VE<BayesianFactor> ve = new VE<>(order);
        ve.setNormalize(false);
        ve.setFactors(bn.getFactors());
        try {
            BayesianFactor f = ve.apply(bn, new int[]{C,e1}, ev);
            System.out.print(f);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }


    public void setEvidence(TIntIntMap obs) {
        throw new NotImplementedException("");
    }
}
