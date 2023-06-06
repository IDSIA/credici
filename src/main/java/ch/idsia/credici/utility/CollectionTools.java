package ch.idsia.credici.utility;

import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CollectionTools {
	public static List<Integer> asList(int[] elements) {
		return Arrays.stream(elements).boxed().collect(Collectors.toList());
	}
	public static List<Double> asList(double[] elements) {
		return Arrays.stream(elements).boxed().collect(Collectors.toList());
	}
	public static int[] shuffle(int[] elements){
		List aux = asList(elements);
		Collections.shuffle(aux, RandomUtil.getRandom());
		return Ints.toArray(aux);
	}
	public static double[] shuffle(double[] elements){
		List aux = asList(elements);
		Collections.shuffle(aux, RandomUtil.getRandom());
		return Doubles.toArray(aux);
	}
	public static int[] toIntArray(List<Integer> list){
		return list.stream().mapToInt(x-> (int)x).toArray();
	}

	public static double[] toDoubleArray(List<Double> list){
		return list.stream().mapToDouble(x-> (double)x).toArray();
	}

	/**
	 * Round non zero values such as the sum is equal to the target.
	 *
	 * @param arr
	 * @param target
	 * @param num_decimals
	 * @return
	 */
	public static double[] roundNonZerosToTarget(double[] arr, double target, boolean newZeros, int num_decimals) {

		double[] data = Arrays.copyOf(arr, arr.length);

		data = ArraysUtil.round(data, num_decimals);

		// Prevents from adding new zeros.
		if(!newZeros) {
			for (int i = 0; i < arr.length; i++) {
				if (data[i] == 0 && arr[i] > 0)
					data[i] = arr[i];
			}
		}

		BigDecimal sum = BigDecimal.valueOf(0.0);
		for (int i = 0; i < data.length; i++) {
			//try {
				sum = sum.add(BigDecimal.valueOf(data[i]));
			//}catch(Exception e){
			//	System.out.println();
			//}
		}
		for (int i = data.length - 1; i >= 0; i--) {
			if (data[i] != 0) {
				sum = sum.subtract(BigDecimal.valueOf(data[i]));
				data[i] = BigDecimal.valueOf(target).subtract(sum).doubleValue();
				break;
			}
		}
		return data;
	}


	public static String[][] toStringMatrix(List<HashMap> table){

		String[] colnames = (String[]) (table.stream()
				.map(r -> (r).keySet())
				.reduce((s1,s2)-> Sets.union(s1,s2)).get().stream().sorted().toArray(String[]::new));


		String[][] out = new String[table.size()+1][];
		out[0] = colnames;

		for(int i=1; i<out.length; i++) {
			HashMap r = (HashMap) table.get(i-1);
			out[i] = Arrays.stream(colnames)
					.map(c -> String.valueOf(r.getOrDefault(c, "")))
					.toArray(String[]::new);
		}


		return out;
	}


	public static void main(String[] args) {

		List table = new ArrayList<>();

		HashMap r1 = new HashMap();
		r1.put("a", true);
		r1.put("b", 1);
		table.add(r1);

		HashMap r2 = new HashMap();
		r2.put("a", false);
		r2.put("c", 45.2);
		table.add(r2);

		for(String[] s : toStringMatrix(table))
			System.out.println(Arrays.toString(s));

	}

	public static int[] choice(int n, int... elements){
		int[] finalElements =  CollectionTools.shuffle(elements);;
		return IntStream.range(0,n).map(i -> finalElements[i]).toArray();
	}

	public static double[] choice(int n, double... elements){
		double[] finalElements =  CollectionTools.shuffle(elements);
		return IntStream.range(0,n).mapToDouble(i -> finalElements[i]).toArray();
	}
}
