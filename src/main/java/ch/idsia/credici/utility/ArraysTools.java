package ch.idsia.credici.utility;

import java.util.Arrays;

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
}
