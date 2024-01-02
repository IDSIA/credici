package ch.idsia.credici.utility.table;

import java.util.Arrays;
import java.util.function.Function;

import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

class TIntMapConverter {

	/**
	 * Convert from the specified map to an array of int using the specified columns
	 * order
	 * 
	 * @param map     the map to be converted
	 * @param columns the order of the columns
	 * @return the converted integer array
	 */
	public static int[] from(TIntIntMap map, int[] columns) {
		return Arrays.stream(columns).map(map::get).toArray();
	}

	/**
	 * A curried version of the from method that returns a version of from with
	 * fixed columns
	 * 
	 * @param columns the order to be fixed
	 * @return the function
	 */
	public static Function<TIntIntMap, int[]> curriedFrom(int[] columns) {
		return map -> Arrays.stream(columns).map(map::get).toArray();
	}

	/**
	 * Convert an array of maps to an array of int arrays sorted by columns.
	 * 
	 * @param map     the input data
	 * @param columns the order of the columns
	 * 
	 * @return the output data
	 */
	public static int[][] from(TIntIntMap[] map, int[] columns) {
		return Arrays.stream(map).map(curriedFrom(columns)).toArray(int[][]::new);
	}

	/**
	 * Discover the set of columns assuming that not all are specified
	 * 
	 * @param data
	 * @return
	 */
	protected static int[] cols(TIntIntMap[] data) {
		TIntSet columns = new TIntHashSet();
		for (TIntIntMap row : data) {
			columns.addAll(row.keys());
		}
		return columns.toArray();
	}	
}