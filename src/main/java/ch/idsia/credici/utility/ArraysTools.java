package ch.idsia.credici.utility;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ArraysTools {

    public static String toString(int[] arr, String sep){
        return Arrays.toString(arr)
                .replace(",", sep).replace("[", "")
                .replace("]", "")
                .trim();
    }

    public static String toString(double[] arr, String sep){
        return Arrays.toString(arr)
                .replace(",", sep).replace("[", "")
                .replace("]", "")
                .trim();
    }

    public static int[] repeat(int num, int size){
        return IntStream.range(0,size).map(i->num).toArray();
    }
    public static int[] ones(int size){
        return repeat(1, size);
    }
    public static int[] zeros(int size){
        return repeat(0, size);
    }
}
