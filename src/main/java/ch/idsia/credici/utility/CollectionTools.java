package ch.idsia.credici.utility;

import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;

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
}
