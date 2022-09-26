package ch.idsia.credici.learning.ve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ch.idsia.crema.factor.GenericFactor;
import gnu.trove.map.hash.TIntObjectHashMap;

public class FactorQueue<F extends GenericFactor> implements Iterator<ArrayList<F>> {
	
	private TIntObjectHashMap<ArrayList<F>> data;
	private int[] sequence;
	private ArrayList<F> results;

	public FactorQueue(int[] sequence) {
		this.data = new TIntObjectHashMap<ArrayList<F>>();
		this.sequence = sequence;
        this.results = new ArrayList<>();

		for (int var : sequence) {
			this.data.put(var, new ArrayList<F>());
		}
	}
	
    /** 
     * Add a factor to the next variable in the queue. 
     * Factors not associated to any further variable are added to the
     * results list. 
     */
	public void add(F factor) {
		for (int variable : sequence) {
			if (factor.getDomain().contains(variable)) {
				data.get(variable).add(factor);
				return;
			}
		}
		
		results.add(factor);
	}
	
	public void init(List<F> factors) {
		LinkedList<F> items = new LinkedList<F>(factors);
		for (int variable : sequence) {
			ListIterator<F> iterator = items.listIterator();
			while(iterator.hasNext()) {
				F f = iterator.next();
				if (f.getDomain().contains(variable)) {
					data.get(variable).add(f);
					iterator.remove();
				}
			}
		}
	}
	
	public int getVariable() {
		return sequence[0];
	}
	@Override
	public boolean hasNext() {
		return sequence.length > 0;
	}
	
	@Override
	public ArrayList<F> next() {
		int next = sequence[0];
		ArrayList<F> factors = data.get(next);
		int[] new_sequence = new int[sequence.length - 1];
		System.arraycopy(sequence, 1, new_sequence, 0, new_sequence.length);
		sequence = new_sequence;
		data.remove(next);
		return factors;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

    public Collection<F> getResults() {
        return results;
    }
}
