package ch.idsia.credici.utility.experiments;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Python {

	public static String mapToDict(HashMap map) {

		String[] items = new String[map.size()];

		for(int i = 0; i< map.size(); i++){
			String k = (String) map.keySet().toArray()[i];
			Object v = map.get(k);

			items[i] = k+"=";
			if(v instanceof int[]) {
				items[i] += Arrays.toString((int[]) v);
			}else if(v instanceof long[]) {
					items[i] += Arrays.toString((long[]) v);
			}else if(v instanceof double[]) {
				items[i] += Arrays.toString((double[]) v);
			}else if(v instanceof int[][] ){
				items[i] += "["+ Stream.of((int[][]) v)
						.map(r -> Arrays.toString(r))
						.collect(Collectors.joining(", "))+"]";
			}else if(v instanceof double[][]) {
				items[i] += "[" + Stream.of((double[][]) v)
						.map(r -> Arrays.toString(r))
						.collect(Collectors.joining(", ")) + "]";
			}else if (v instanceof Integer || v instanceof Double || v instanceof Float || v instanceof Long) {
				items[i] += String.valueOf(v);
			}else if (v instanceof Boolean) {
				if (((Boolean) v).booleanValue()) items[i] += "True";
				else items[i] += "False";
			}else if(v instanceof String){
				items[i] += "'"+String.valueOf(v)+"'";
			}else if(v instanceof HashMap){
				items[i]+=mapToDict((HashMap) v);
			}else{
				throw new IllegalArgumentException("Error converting "+k+" to dict");
			}
		}

		return "dict("+Stream.of(items).collect(Collectors.joining(", "))+")";

	}

	public static void main(String[] args) {

		double[] psnExact = new double[]{0.0, 0.055186000000000006};

		String name = "output";

		HashMap map = new HashMap();
		map.put("innerPoints", new int[]{0,0,0,0,0});
		map.put("psnExact", psnExact);
		map.put("psnEM", new double[][]{psnExact, psnExact});

		map.put("entero", 0);
		map.put("real", 4.3);
		map.put("booleano", true);
		map.put("string", "cadena");

		HashMap emptyMap = new HashMap();
		map.put("stats", emptyMap);



		System.out.println(mapToDict(map));


	}
}
