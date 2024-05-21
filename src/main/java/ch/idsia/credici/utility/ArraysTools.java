package ch.idsia.credici.utility;

import java.util.Arrays;
import java.util.stream.IntStream;

import ch.idsia.credici.learning.eqem.fixing.EquationFixing;

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
    
    
    public static int argmax(int[] data, int... except) {
    	Arrays.sort(except);
    	
    	int max = Integer.MIN_VALUE;
    	int idx = -1;
    	int skip = 0;
    	
    	for (int i = 0; i < data.length; ++i) {
    		if (skip < except.length && i == except[skip]) {
    			++skip;
    			continue;
    		}
    		
    		if (max < data[i]) { 
    			idx = i; 
    			max = data[i];
    		}
    	}
    	return idx;
    }
    
    
    public static int argmax(double[] data) {
    	double max = Double.NEGATIVE_INFINITY;
    	int idx = -1;
    	for (int i = 0; i < data.length; ++i) {
    		if (max < data[i]) { 
    			idx = i; 
    			max = data[i];
    		}
    	}
    	return idx;
    }
    
    
    public static void main(String[] args) {
		int[] data = new int[] { 2,7, 1,4,5,2,9,10 };
		System.out.println(argmax(data, 6,1));
	}
}
