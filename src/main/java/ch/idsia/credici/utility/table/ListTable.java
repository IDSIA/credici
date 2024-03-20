package ch.idsia.credici.utility.table;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ListTable<T extends Number, O> extends DataTable<List<T>, O> {

	@SuppressWarnings("serial")
	public ListTable(int[] columns) {
		super(columns, null, null, (a, b) -> new ArrayList<T>(a.size() + b.size()) {
				{
					addAll(a);
					addAll(b);
				}
		}, null);
	}

	public void add(int[] row, T value) {
		super.add(row, Arrays.asList(value));
	}
	
	public void addAll(Map<int[], T> data) {
		for (var entry : data.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
	}
	
	public void toCSV(String filename, Function<Integer, String> naming) {
		try (FileWriter writer = new FileWriter(filename)) {
			for (int col : columns) {
				writer.append(naming.apply(col)).append(',');
			}
			writer.append("\n");

			for (var entry : dataTable.entrySet()) {
				String row = Arrays.stream(entry.getKey()).mapToObj((a) -> Integer.toString(a))
						.collect(Collectors.joining(","));
				writer.append(row).append(",");
				String vals = entry.getValue().stream().map(Object::toString).collect(Collectors.joining(","));
				writer.append(vals).append("\n");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
