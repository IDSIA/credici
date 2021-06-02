package edu.neurips.causalem.utility;

import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionTools {
	public static List<Integer> asList(int[] elements) {
		return Arrays.stream(elements).boxed().collect(Collectors.toList());
	}
	public static int[] shuffle(int[] elements){
		List aux = asList(elements);
		Collections.shuffle(aux, RandomUtil.getRandom());
		return Ints.toArray(aux);
	}

	public static int[] toIntArray(List<Integer> list){
		return list.stream().mapToInt(x-> (int)x).toArray();
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


}
