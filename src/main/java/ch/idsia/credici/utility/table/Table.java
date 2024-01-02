package ch.idsia.credici.utility.table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.impl.hash.TIntIntHash;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ch.idsia.credici.collections.FIntIntHashMap;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

/**
 * A data table counting duplicates.
 */
public class Table implements Iterable<Map.Entry<int[], Double>> {

	private int[] columns;

	/**
	 * Using a {@link TreeMap}. This will use a comparator that we can provide.
	 * HashMap uses the object's hash that won't work correctly for arrays of int.
	 */
	private TreeMap<int[], Double> dataTable;



	public Table(int[] columns, int[][] data) {

		dataTable = new TreeMap<>(Arrays::compare);

		this.columns = columns;
		if (data != null) {
			for (int[] inst : data) {
				add(inst, 1);
			}
		}
	}

	/**
	 * Create a Table from an array of {@link TIntIntMap}s. 
	 * @param data
	 * @param unit
	 * @param add
	 */
	public Table(TIntIntMap[] data) {

		dataTable = new TreeMap<>(Arrays::compare);

		if (data == null || data.length == 0)
			throw new IllegalArgumentException("Need data");

		this.columns = TIntMapConverter.cols(data);
		
		for (TIntIntMap inst : data) {
			add(inst, 1);
		}
	}

	
	
	public Table(int[] columns) {
		this(columns, null);
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
	 * @param index
	 * @return
	 */
	public double getWeight(TIntIntMap index) {
		return dataTable.get(getIndex(index));
	}

	/**
	 * Get the list of weight for all possible combinations of values of the specified columns 
	 * assuming each column has the indicated number of possible states.
	 * 
	 * @param vars
	 * @param sizes
	 * @return
	 */
	public double[] getWeights(int[] vars, int[] sizes) {
		int[] state = new int[vars.length];

		int cumsize = 1;

		TIntIntMap strides = new TIntIntHashMap();
		for (int i = 0; i < vars.length; ++i) {
			cumsize = cumsize * sizes[i];
			strides.put(vars[i], cumsize);
		}
		
		int[] col_strides = new int[columns.length];

		for (int i = 0; i < columns.length; ++i) {
			if (strides.containsKey(columns[i])) {
				col_strides[i] = strides.get(i);
			}
		}
		
		double[] results = new double[cumsize];
		
		for (var item : dataTable.entrySet()) {
			int[] states = item.getKey();
			int offset = 0;
			for (int i = 0; i < columns.length; ++i) {
				offset += col_strides[i] * states[i];
			}
			results[offset] += item.getValue();
		}
		
		return results;
	}
	
	/** 
	 * Get the weight of a row
	 * 
	 * @param index
	 * @return
	 */
	public double getWeight(int[] index) {
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
	protected void add(int[] row, double count) {
		dataTable.compute(row, (k, v) -> (v == null) ? count : v + count);
	}

	/**
	 * Add a new row using a different column order.
	 * 
	 * @param cols  int[] the new columns order
	 * @param inst  int[] the row to be added in cols order
	 * @param count the number of rows being added.
	 */
	public void add(int[] cols, int[] inst, double count) {
		int[] row = Arrays.stream(columns).map(col -> ArrayUtils.indexOf(cols, col)).map(i -> inst[i]).toArray();

		dataTable.compute(row, (k, v) -> (v == null) ? count : v + count);
	}

	/**
	 * Add a TIntIntMap with the specified count. The map must contain all the keys
	 * specified in the columns
	 * 
	 * @param inst  {@link TIntIntMap} - the row to be added
	 * @param count <T> the number of rows being added.
	 */
	public void add(TIntIntMap inst, double count) {
		int[] row = Arrays.stream(columns).map(inst::get).toArray();
		dataTable.compute(row, (k, v) -> (v == null) ? count : v + count);
	}

	/**
	 * Add a TIntIntMap with unit count. The map must contain all the keys specified
	 * in the columns
	 * 
	 * @param inst  {@link TIntIntMap} - the row to be added
	 * @param count <T> the number of rows being added.
	 */
	public void add(TIntIntMap inst) {
		add(inst, 1);
	}

	/**
	 * Create a sub-table for the specified columns. Columns not present in this
	 * table will be ignore and not be part of the resulting table.
	 * 
	 * @param cols the subset of columns
	 * @return a new Table
	 */
	public Table subtable(int[] cols) {
		int[] idx = Arrays.stream(cols).map(col -> ArrayUtils.indexOf(columns, col)).filter(a -> a >= 0).toArray();
		int[] matching = IntStream.of(idx).map(id -> columns[id]).toArray();

		Table res = new Table(matching);
		for (Map.Entry<int[], Double> entry : dataTable.entrySet()) {
			int[] values = entry.getKey();
			double count = entry.getValue();

			int[] newkey = Arrays.stream(idx).map(i -> values[i]).toArray();
			res.add(newkey, count);
		}
		return res;
	}

	/**
	 * Covert weights of a Table
	 * 
	 * @param op  the conversion operation
	 * @return the new Table
	 */
	public Table mapWeights(Function<Double, Double> op) {
		// new table shares the same columns by default
		Table table = new Table(columns);
		for (Map.Entry<int[], Double> entry : dataTable.entrySet()) {
			table.dataTable.put(entry.getKey(), op.apply(entry.getValue()));
		}
		return table;
	}

	/**
	 * Read a table from a whitespace separated file. The whitespace can be any
	 * regex \s character.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Table readTable(String filename) throws IOException {
		return readTable(filename, "\\s");
	}

	/**
	 * Read table from sep separated lists of values. Lists are separated by
	 * newlines and first row is the header. Header row must be integers.
	 * 
	 * @param filename
	 * @param sep
	 * @return
	 * @throws IOException
	 */
	public static Table readTable(String filename, String sep) throws IOException {
		try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
			String[] cols = input.readLine().split(sep);
			int[] columns = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
			Table ret = new Table(columns);

			String line;
			while ((line = input.readLine()) != null) {
				cols = line.split(sep);

				int[] row = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
				ret.add(row, 1);
			}
			return ret;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String out = String.join("\t",
				Arrays.stream(columns).<String>mapToObj(Integer::toString).toArray(a -> new String[a]));
		sb.append("count\t").append(out).append('\n');

		for (Map.Entry<int[], Double> entry : dataTable.entrySet()) {
			out = String.join("\t",
					Arrays.stream(entry.getKey()).<String>mapToObj(Integer::toString).toArray(a -> new String[a]));
			sb.append(entry.getValue()).append('\t').append(out).append('\n');
		}
		return sb.toString();
	}

	@Override
	public Iterator<Map.Entry<int[], Double>> iterator() {
		return dataTable.entrySet().iterator();
	}

	public Iterable<Pair<TIntIntMap, Double>> mapIterable() {
		return new Iterable<Pair<TIntIntMap, Double>>() {

			@Override
			public Iterator<Pair<TIntIntMap, Double>> iterator() {

				var iter = dataTable.entrySet().iterator();
				return new Iterator<Pair<TIntIntMap, Double>>() {

					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public Pair<TIntIntMap, Double> next() {
						var nextVal = iter.next();
						TIntIntMap ret = new FIntIntHashMap(columns, nextVal.getKey());
						return Pair.of(ret, nextVal.getValue());
					}
				};
			}
		};
	}

	public static void main(String[] args) {
		TIntIntMap p = new TIntIntHashMap();
		p.put(1, 2);
		System.out.println(p.get(123));
	}

	public int[] getColumns() {
		return this.columns;
	}
}
