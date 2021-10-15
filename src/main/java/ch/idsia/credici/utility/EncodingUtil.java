package ch.idsia.credici.utility;

import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Booleans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EncodingUtil {

	// big endian
	public static long boolArrayToDecimal(boolean[] arr){
		long index = 0;
		for(int i=0; i<arr.length; i++)
			if(arr[i]) index += Math.pow(2,arr.length-1-i);
		return index;
	}
	public static boolean[] decimalToBoolArray(long index, int size){

		String binStr = Integer.toBinaryString((int) index);

		int n = size - binStr.length();
		if(n<0) throw new IllegalArgumentException("Wrong size value");

		for(int i=0; i<n; i++)
			binStr = "0"+binStr;

		return Booleans.toArray(binStr.chars().mapToObj(c -> (((char)c)=='1')).collect(Collectors.toList()));
	}
	public static List<boolean[]> getPossibleMasks(int size) {
		return IntStream.range(0, (int) (Math.pow(2, size)))
				.mapToObj(i -> decimalToBoolArray((long) i, size))
				.collect(Collectors.toList());
	}

	public static List<boolean[]> getPossibleMasks(int trueSize, int falseSize){
		return getPossibleMasks(falseSize+trueSize)
				.stream()
				.filter(m -> Booleans.asList(m).stream().filter(i -> i).count() == trueSize)
				.collect(Collectors.toList());
	}


	public static List<boolean[]> getRandomMask(int trueSize, int falseSize) {

		List elements = new ArrayList<>();
		for (int i = 0; i < trueSize; i++)
			elements.add(true);
		for (int i = 0; i < falseSize; i++)
			elements.add(false);

		Collections.shuffle(elements);
		return elements;
	}

	public static List<boolean[]> getRandomSeqMask(int size) {
		List out = new ArrayList();
		List idx = IntStream.range(0, size).boxed().collect(Collectors.toList());
		Collections.shuffle(idx);
		boolean[] m = new boolean[size];

		for (int i : idx.stream().mapToInt(i -> ((Integer) i).intValue()).toArray()) {
			m[i] = true;
			out.add(m.clone());
		}

		return out;
	}


	public static List<int[]> getRandomSeqIntMask(int size) {
		List out = new ArrayList();
		List idx = IntStream.range(0, size).boxed().collect(Collectors.toList());
		Collections.shuffle(idx);
		int[] m = new int[size];

		for (int i : idx.stream().mapToInt(i -> ((Integer) i).intValue()).toArray()) {
			m[i] = 1;
			out.add(m.clone());
		}

		return out;
	}


	public static List<int[]> getRandomSeqIntMask(int size, boolean addZero) {
		List<int[]> m = getRandomSeqIntMask(size);
		if(addZero)
			m.add(0, new int[size]);

		return m;
	}

	public static List<boolean[]> getSequentialMask(int size) {
		List out = new ArrayList();
		boolean[] m = new boolean[size];
		for (int i =0; i<size; i++) {
			m[i] = true;
			out.add(m.clone());
		}
		return out;
	}



}
