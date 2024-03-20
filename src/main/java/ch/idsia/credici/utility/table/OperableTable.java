package ch.idsia.credici.utility.table;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class Aggregate<T> {
	private final T value;
	private final BiFunction<T, T, T> add;

	public Aggregate(T value, BiFunction<T, T, T> add) {
		this.value = value;
		this.add = add;
	}

	public Aggregate<T> add(T value) {
		T newvalue = this.add.apply(this.value, value);
		return new Aggregate<>(newvalue, this.add);
	}

	public Aggregate<T> add(Aggregate<T> item) {
		T newvalue = this.add.apply(this.value, item.value);
		return new Aggregate<>(newvalue, this.add);
	}
}



public class OperableTable extends DataTable<Number[], Double> {

	public static interface Column<T extends Number> {
		public T add(T first, T second);
		public T zero();
		
		public static <T extends Number> Column<T> get(Class<T> clazz) {
//			switch(clazz) {
//			case Double.class:
//				
//			}
			return null;
		}
	}
	
	public class DoubleColumn implements Column<Double> {
		@Override
		public Double add(Double first, Double second) {
			return first + second;
		}
		@Override
		public Double zero() {
			return 0.0;
		}
	}
	public class IntColumn implements Column<Double> {
		@Override
		public Double add(Double first, Double second) {
			return first + second;
		}
		@Override
		public Double zero() {
			return 0.0;
		}
	}
	private Column<Number>[] columnTypes;
	
	private Number[] init() {
		return Arrays.stream(columnTypes).map(Column::zero).toArray(Number[]::new);
	}
	
	public OperableTable(int[] columns, Class<? extends Number>... types) {
		super(columns, null, null, null, null );
		columnTypes = Arrays.stream(types).map((c) -> Column.get(c)).toArray(Column[]::new);
		this.zero = init();
	}

	public void add(int[] key, Number... values) {
		//this.add(key, new Bound(values));
	}

	public void addAll(Map<int[], Number[]> data) {
		for (var entry : data.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}

//	public void toCSV(String filename, Function<Integer, String> naming, String... extra) {
//		try (FileWriter writer = new FileWriter(filename)) {
//			for (int col : columns) {
//				writer.append(naming.apply(col)).append(',');
//			}
//
//			String rest = Arrays.stream(extra)
//					.flatMap((name) -> Arrays.stream(new String[] { name + "_min", name + "_max" }))
//					.collect(Collectors.joining(","));
//			writer.append(rest);
//			writer.append("\n");
//
//			for (var entry : dataTable.entrySet()) {
//				String row = Arrays.stream(entry.getKey()).mapToObj((a) -> Integer.toString(a))
//						.collect(Collectors.joining(","));
//				writer.append(row).append(",");
//
//				String vals = Arrays. entry.getValue().stream()
//						.flatMap((a) -> Arrays.stream(a).mapToObj((o) -> Double.toString(o)))
//						.collect(Collectors.joining(","));
//				writer.append(vals).append("\n");
//			}
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
}