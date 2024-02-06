package ch.idsia.credici.utility.table;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class DoubleTable extends DataTable<Double, Double> {

	public DoubleTable(int[] columns) {
		super(columns, 1d, 0d, (a, b) -> a + b, (i) -> new Double[i]);
	}

	public DoubleTable(int[] columns, int[][] data) {
		this(columns);

		if (data != null) {
			for (int[] inst : data) {
				add(inst, unit);
			}
		}
	}

	/**
	 * Create a Table from an array of {@link TIntIntMap}s.
	 * 
	 * @param data
	 * @param unit
	 * @param add
	 */
	public DoubleTable(TIntIntMap[] data) {
		this(TIntMapConverter.cols(data));

		if (data.length == 0)
			throw new IllegalArgumentException("Need data");

		for (TIntIntMap inst : data) {
			add(inst);
		}
	}

	public DoubleTable subtable(int[] cols) {
		DoubleTable tofill = new DoubleTable(cols);
		return super.subtable(tofill);
	}

	
	/**
	 * Scale weights between 0 and 1
	 * @return
	 */
	public DoubleTable scale() {
		double small = 0;
		
		for (var entry : dataTable.entrySet()) {
			small = Math.max(small, entry.getValue());
		}
		
		//System.out.println("AMX " + small);
		if (small == 0) return this;
		
		DoubleTable res = new DoubleTable(columns);
		for (var entry : dataTable.entrySet()) {
			res.add(entry.getKey(), entry.getValue() / small);
		}
		
		return res;
	}
	
	
	
	
	public double[] getWeights(int[] vars, int[] sizes) {
		Double[] dta = super.getWeightsFor(vars, sizes);
		return Arrays.stream(dta).mapToDouble(i -> i).toArray();
	}

	public double[] getWeights2(int[] vars, int[] sizes) {

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

	TIntIntMap getKeyMap(int[] index) {
		TIntIntMap map = new TIntIntHashMap();
		for (int i = 0; i < index.length; ++i) {
			map.put(columns[i], index[i]);
		}
		return map;
	}

	public TIntIntMap[] toMap(boolean roundup) {
		return dataTable.entrySet().stream().flatMap(row -> IntStream
				.range(0, (int) (row.getValue() + (roundup ? 0.5 : 0))).mapToObj(i -> this.getKeyMap(row.getKey())))
				.toArray(TIntIntHashMap[]::new);

	}

	/**
	 * Read a table from a whitespace separated file. The whitespace can be any
	 * regex \s character.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static DoubleTable readTable(String filename) throws IOException {
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
	public static DoubleTable readTable(String filename, String sep) throws IOException {
		try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
			String[] cols = input.readLine().split(sep);
			int[] columns = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
			DoubleTable ret = new DoubleTable(columns);

			String line;
			while ((line = input.readLine()) != null) {
				cols = line.split(sep);

				int[] row = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
				ret.add(row, 1d);
			}
			return ret;
		}
	}

	static int parseInt(String value) {
		return (int) Double.parseDouble(value);
	}
	
	public static DoubleTable readTable(String filename, int skip, String sep, Map<String, Integer> columns_out)
			throws IOException {
		try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
			for (int i = 0; i < skip; ++i)
				input.readLine().split(sep);

			String[] values = input.readLine().split(sep);
			int[] columns = IntStream.range(0, values.length).toArray();
			if (columns_out != null) {
				for (int i = 0; i < values.length; ++i) {
					columns_out.put(values[i], columns[i]);
				}
			}

			DoubleTable ret = new DoubleTable(columns);

			String line;
			while ((line = input.readLine()) != null) {
				values = line.split(sep);

				int[] row = Arrays.stream(values).mapToInt(DoubleTable::parseInt).toArray();
				ret.add(row, 1d);
			}
			return ret;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String head = Arrays.stream(columns).mapToObj(Integer::toString).collect(Collectors.joining(" | "));
		sb.append(head).append(" | weight\n");

		for (Map.Entry<int[], Double> row : dataTable.entrySet()) {
			String key = Arrays.stream(row.getKey()).mapToObj(Integer::toString).collect(Collectors.joining(" | "));
			sb.append(key).append(" | ").append(row.getValue()).append("\n");
		}
		return sb.toString();

	}

}
