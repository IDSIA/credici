package ch.idsia.credici.collections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class FIntObjectHashMap<T> extends TIntObjectHashMap<T> {
    public FIntObjectHashMap() {
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, -1);
        this.no_entry_value = -1;
    }

    public FIntObjectHashMap( TIntObjectMap<? extends T> map ) {
        super(map.size(), 0.5f, -1);
        putAll( map );
    }

}
