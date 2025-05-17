package ch.idsia.credici.utility;

import java.util.*;
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


    public static double[][] removeDuplicateRows(double[][] data) {
        // Use a LinkedHashSet to maintain the order and remove duplicates
        Set<String> seenRows = new LinkedHashSet<>();
        List<double[]> uniqueRows = new ArrayList<>();

        for (double[] row : data) {
            // Convert row to a String for easy comparison
            String rowKey = Arrays.toString(row);

            // Add to unique rows if not already in the set
            if (seenRows.add(rowKey)) {
                uniqueRows.add(row);
            }
        }

        // Convert List to a 2D array
        return uniqueRows.toArray(new double[0][0]);
    }
}
