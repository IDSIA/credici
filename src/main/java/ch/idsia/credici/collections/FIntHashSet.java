package ch.idsia.credici.collections;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class FIntHashSet extends TIntHashSet {
    public FIntHashSet() { 
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, -1);
    }

    public FIntHashSet(int[] v) { 
        super(Math.max( v.length, DEFAULT_CAPACITY ), DEFAULT_LOAD_FACTOR, -1);
        addAll(v);
    }

    public FIntHashSet(TIntSet set) {
        super(Math.max(set.size(), DEFAULT_CAPACITY ), DEFAULT_LOAD_FACTOR, -1);
        addAll(set);
    }
}
