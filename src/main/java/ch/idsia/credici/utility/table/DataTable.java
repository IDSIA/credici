package ch.idsia.credici.utility.table;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import gnu.trove.set.TIntSet;


public class DataTable<T> {
	private Map<TIntSet, T> data;

	public DataTable() {
		
	}
	public static void main(String[] args) {
		int[] o = {1,2,3};
		System.out.println(Arrays.hashCode(o));
		int[] p = {2,2,3};
		p[0]--;
		System.out.println(Arrays.hashCode(p));
	}
	
}