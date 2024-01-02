package ch.idsia.credici.collections;

import ch.idsia.credici.collections.FIntIntHashMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class FIntIntHashMap extends TIntIntHashMap {
    public FIntIntHashMap() {
        super();
        no_entry_key = -1;
        no_entry_value = -1;
    }

    public FIntIntHashMap(TIntIntMap map) {
        super(map);
        no_entry_key = -1;
        no_entry_value = -1;
    }

    public FIntIntHashMap(int[] keys, int[] values) {
        super(keys, values);
        no_entry_key = -1;
        no_entry_value = -1;
    }

    public static TIntIntMap of(int k, int i) {
        TIntIntMap ma = new FIntIntHashMap();
        ma.put(k, i);
        return ma;
    }

    public static TIntIntMap of(int[] k, int[] i) {
        return new FIntIntHashMap(k,i);
    }
}
