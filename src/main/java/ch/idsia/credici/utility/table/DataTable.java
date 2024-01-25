package ch.idsia.credici.utility.table;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.collections.FIntIntHashMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class DataTable<T, O> implements Iterable<Map.Entry<int[], T>> {
	protected final int[] columns;
	protected O metadata;

	protected T unit;
	protected T zero;
	protected BiFunction<T, T, T> add;
	protected Function<Integer, T[]> createArray;

	/**
	 * Using a {@link TreeMap}. This will use a comparator that we can provide.
	 * HashMap uses the object's hash that won't work correctly for arrays of int.
	 */
	protected TreeMap<int[], T> dataTable;

	public void setMetadata(O data) {
		this.metadata = data;
	}

	public O getMetadata() {
		return metadata;
	}



	protected DataTable(int[] columns, T unit, T zero, BiFunction<T, T, T> add, Function<Integer, T[]> array) {
		this.columns = columns;
		this.unit = unit;
		this.zero = zero;
		this.add = add;
		this.createArray = array;
		this.dataTable = new TreeMap<>(Arrays::compare);

	}

	


	/**
	 * Sort and Expand or limit the map to the columns of the table.
	 * 
	 * @param map
	 * @return int[] of the values for the table columns
	 */
	private int[] getIndex(TIntIntMap map) {
		return Arrays.stream(columns).map(map::get).toArray();
	}

	/**
	 * Get the weight of a row
	 * 
	 * @param index
	 * @return
	 */
	public T getWeight(TIntIntMap index) {
		return dataTable.get(getIndex(index));
	}

	/**
	 * Get the list of weight for all possible combinations of values of the
	 * specified columns assuming each column has the indicated number of possible
	 * states.
	 * 
	 * @param vars
	 * @param sizes
	 * @return
	 */
	public T[] getWeightsFor(int[] vars, int[] sizes) {

		// cumulative size
		int cumsize = 1;

		TIntIntMap strides = new TIntIntHashMap();
		for (int i = 0; i < vars.length; ++i) {
			strides.put(vars[i], cumsize);
			cumsize = cumsize * sizes[i];
		}

		int[] col_strides = new int[columns.length];

		for (int i = 0; i < columns.length; ++i) {
			if (strides.containsKey(columns[i])) {
				col_strides[i] = strides.get(columns[i]);
			}
		}

		T[] results = createArray.apply(cumsize);
		for (int i = 0; i< results.length; ++i) {
			results[i] = zero;
		}
		
		for (var item : dataTable.entrySet()) {
			int[] states = item.getKey();
			int offset = 0;
			for (int i = 0; i < columns.length; ++i) {
				offset += col_strides[i] * states[i];
			}
			results[offset] = add.apply(results[offset], item.getValue());
		}

		return results;
	}

	
	/**
	 * Get the weight of a row
	 * 
	 * @param index
	 * @return
	 */
	public T getWeight(int[] index) {
		if (index.length != columns.length)
			throw new IllegalArgumentException("Wrong index size. Must match the columns");

		return dataTable.get(index);
	}

	/**
	 * Add to dataTable assuming correctly ordered row items
	 * 
	 * @param row   the item to be added
	 * @param count the number of rows to be added
	 */
	public void add(int[] row, T count) {
		dataTable.compute(row, (k, v) -> (v == null) ? count : add.apply(v, count));
	}

	/**
	 * Add a new row using a different column order.
	 * 
	 * @param cols  int[] the new columns order
	 * @param inst  int[] the row to be added in cols order
	 * @param count the "number" of rows being added.
	 */
	public void add(int[] cols, int[] inst, T count) {
		int[] row = Arrays.stream(columns).map(col -> ArrayUtils.indexOf(cols, col)).map(i -> inst[i]).toArray();

		dataTable.compute(row, (k, v) -> (v == null) ? count : add.apply(v, count));
	}

	/**
	 * Add a TIntIntMap with the specified count. The map must contain all the keys
	 * specified in the columns
	 * 
	 * @param inst  {@link TIntIntMap} - the row to be added
	 * @param count <T> the number of rows being added.
	 */
	public void add(TIntIntMap inst, T count) {
		int[] row = Arrays.stream(columns).map(inst::get).toArray();
		dataTable.compute(row, (k, v) -> (v == null) ? count : add.apply(v, count));
	}

	/**
	 * Add a TIntIntMap with unit count. The map must contain all the keys specified
	 * in the columns
	 * 
	 * @param inst  {@link TIntIntMap} - the row to be added
	 * @param count <T> the number of rows being added.
	 */
	public void add(TIntIntMap inst) {
		add(inst, unit);
	}

	/**
	 * Fills the provided sub-table with aggregated data from this table. 
	 * Columns missing in this table are set to zero.
	 * 
	 * @param cols the subset of columns
	 * @return a new Table
	 */
	protected <SUB extends DataTable<T,O>> SUB subtable(SUB tofill) {
		int[] cols = tofill.columns;
		
		// indices of the desired columns
		int[] idx = Arrays.stream(cols).map(col -> ArrayUtils.indexOf(columns, col)).toArray();
		//int[] matching = IntStream.of(idx).map(id -> columns[id]).toArray();

		for (Map.Entry<int[], T> entry : dataTable.entrySet()) {
			int[] values = entry.getKey();
			T count = entry.getValue();

			int[] newkey = Arrays.stream(idx).map(i -> i < 0 ? 0 : values[i]).toArray();
			tofill.add(newkey, count);
		}
		
		return tofill;
	}

	/**
	 * Covert weights of a Table
	 * 
	 * @param op the conversion operation
	 * @return the new Table
	 */
	public DataTable<T,O> mapWeights(Function<T, T> op) {
		// new table shares the same columns by default
		DataTable<T,O> table = new DataTable<T,O>(columns, unit, zero, add, createArray);
		for (Map.Entry<int[], T> entry : dataTable.entrySet()) {
			table.dataTable.put(entry.getKey(), op.apply(entry.getValue()));
		}
		return table;
	}

	

	@Override
	public Iterator<Map.Entry<int[], T>> iterator() {
		return dataTable.entrySet().iterator();
	}

	
	public Iterable<Pair<TIntIntMap, T>> mapIterable() {
		return new Iterable<Pair<TIntIntMap, T>>() {

			@Override
			public Iterator<Pair<TIntIntMap, T>> iterator() {

				var iter = dataTable.entrySet().iterator();
				return new Iterator<Pair<TIntIntMap, T>>() {

					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public Pair<TIntIntMap, T> next() {
						var nextVal = iter.next();
						TIntIntMap ret = new FIntIntHashMap(columns, nextVal.getKey());
						return Pair.of(ret, nextVal.getValue());
					}
				};
			}
		};
	}


	public int[] getColumns() {
		return this.columns;
	}
}
