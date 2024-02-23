package ch.idsia.credici.utility.table;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Function;

final class Bound {
	final double lower[], upper[];

	public Bound(int size) {
		upper = DoubleStream.generate(() -> Double.NEGATIVE_INFINITY).limit(size).toArray();
		lower = DoubleStream.generate(() -> Double.POSITIVE_INFINITY).limit(size).toArray();
	}

	public Bound(double[] value) {
		lower = upper = value;
	}

	Bound(double[] lower, double[] upper) {
		this.lower = lower;
		this.upper = upper;
	}

	public Bound extend(double[] value) {
		double[] l = lower.clone();
		double[] h = upper.clone();

		for (int i = 0; i < l.length; ++i) {
			l[i] = Math.min(lower[i], value[i]);
			h[i] = Math.max(upper[i], value[i]);
		}
		return new Bound(l, h);
	}

	public Bound extend(Bound value) {
		double[] l = lower.clone();
		double[] h = upper.clone();

		for (int i = 0; i < l.length; ++i) {
			l[i] = Math.min(lower[i], value.lower[i]);
			h[i] = Math.max(upper[i], value.upper[i]);
		}
		return new Bound(l, h);
	}

	Stream<double[]> stream() {
		return IntStream.range(0, lower.length).mapToObj((i) -> new double[] { lower[i], upper[i] });
	}
}

public class MinMaxTable extends DataTable<Bound, Double> {

	public MinMaxTable(int[] columns, int values) {
		super(columns, null, null, (current, adding) -> current.extend(adding), Bound[]::new);
	}

	public void add(int[] key, double[] values) {
		this.add(key, new Bound(values));
	}

	public void addAll(Map<int[], double[]> data) {
		for (var entry : data.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}

	public void toCSV(String filename, Function<Integer, String> naming, String... extra) {
		try (FileWriter writer = new FileWriter(filename)) {
			for (int col : columns) {
				writer.append(naming.apply(col)).append(',');
			}

			String rest = Arrays.stream(extra)
					.flatMap((name) -> Arrays.stream(new String[] { name + "_min", name + "_max" }))
					.collect(Collectors.joining(","));
			writer.append(rest);
			writer.append("\n");

			for (var entry : dataTable.entrySet()) {
				String row = Arrays.stream(entry.getKey()).mapToObj((a) -> Integer.toString(a))
						.collect(Collectors.joining(","));
				writer.append(row).append(",");

				String vals = entry.getValue().stream().flatMap((a) -> Arrays.stream(a).mapToObj((o) -> Double.toString(o)))
						.collect(Collectors.joining(","));
				writer.append(vals).append("\n");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
