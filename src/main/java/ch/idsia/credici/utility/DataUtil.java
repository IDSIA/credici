package ch.idsia.credici.utility;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.data.ReaderCSV;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DataUtil {

	public static BayesianFactor getCounts(TIntIntMap[] data, Strides dom) {
		// sort the variables in the domain
		dom = dom.sort();

		BayesianFactor counts = new BayesianFactor(dom);
		int[] vars = dom.getVariables();

		for (int i = 0; i < dom.getCombinations(); i++) {

			int[] states = dom.statesOf(i);

			TIntIntMap assignament = new TIntIntHashMap();
			for (int j = 0; j < vars.length; j++)
				assignament.put(vars[j], states[j]);

			counts.setValueAt(Stream.of(data)
					.filter(d -> IntStream.of(vars).allMatch(v -> d.get(v) == assignament.get(v)))
					.count(), i);
		}
		return counts;
	}

	public static Pair<TIntIntMap, Long>[] getCounts(TIntIntMap[] data){
		ArrayList counts = new ArrayList();
		for(TIntIntMap instance : DataUtil.unique(data))
			counts.add(new ImmutablePair(instance, Arrays.stream(data).filter(s -> DataUtil.instanceEquals(s,instance)).count()));
		return (Pair<TIntIntMap, Long>[]) counts.toArray(Pair[]::new);
	}

	public static TIntIntMap[] dataFromCounts(BayesianFactor counts){
		return dataFromCounts(counts, true);
	}

	public static TIntIntMap[] dataFromCounts(BayesianFactor counts, boolean shuffle){

		Strides dom = counts.getDomain();
		int[] vars = dom.getVariables();


		List data = new ArrayList();

		for (int i = 0; i < dom.getCombinations(); i++) {

			int[] states = dom.statesOf(i);
			int n = (int) counts.getValue(states);

			for(int k=0; k<n; k++){
				TIntIntMap assignament = new TIntIntHashMap();
				for (int j = 0; j < vars.length; j++)
					assignament.put(vars[j], states[j]);
				data.add(assignament);
			}
		}

		if(shuffle)
			Collections.shuffle(data);

		return (TIntIntMap[])data.toArray(TIntIntMap[]::new);
	}



	public static BayesianFactor getJointProb(TIntIntMap[] data, Strides dom) {
    	return getCounts(data,dom).scalarMultiply(1.0/data.length);
	}


	public static BayesianFactor getCondProb(TIntIntMap[] data, Strides left, Strides right){
		if(ArraysUtil.intersection(left.getVariables(), right.getVariables()).length > 0)
				throw new IllegalArgumentException("Overlapping domains");
		BayesianFactor joint = getCounts(data, left.concat(right));
		BayesianFactor jointRight = getCounts(data, right);
		return joint.divide(jointRight);
	}



	public static HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap(StructuralCausalModel model, TIntIntMap[] data ){

		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();

		for(int[] right : model.endoConnectComponents()) {
			int[] left = model.getEndegenousParents(right);
			BayesianFactor p = null;
			if(left.length>0)
				p = DataUtil.getCondProb(data,model.getDomain(right),model.getDomain(left));
			else
				p = DataUtil.getJointProb(data,model.getDomain(right));
			empirical.put(Arrays.stream(right).boxed().collect(Collectors.toSet()), p);
		}

	 	return empirical;
	}

	public static void toCSV(String filename, TIntIntMap... data) throws IOException {

		int[] dataVars = data[0].keys();
		String[] varNames = IntStream.of(dataVars).mapToObj(i -> String.valueOf(i)).toArray(String[]::new);

		int[][] dataArray = Stream.of(ObservationBuilder.toDoubles(data, dataVars))
				.map(d -> ArraysUtil.toIntVector(d)).toArray(int[][]::new);

		new WriterCSV(dataArray, filename)
				.setVarNames(varNames).write();
	}

	public static TIntIntMap[] fromCSV(String filename) throws IOException, CsvException {
		ReaderCSV reader = new ReaderCSV(filename).read();
		return ObservationBuilder.observe(reader.getVarNames(), reader.getData());
	}

	public static boolean instanceEquals(TIntIntMap s1, TIntIntMap s2){
		if(ArraysUtil.difference(s1.keys(), s2.keys()).length != 0)
			return false;
		return Arrays.stream(s1.keys()).allMatch(v -> s1.get(v) == s2.get(v));
	}

	public static TIntIntMap[] unique(TIntIntMap[] data){
		ArrayList out = new ArrayList();
		for(TIntIntMap instance: data){
			if(!out.stream().anyMatch(s-> DataUtil.instanceEquals((TIntIntMap) s, instance)) ){
				out.add(instance);
			}
		}
		return (TIntIntMap[]) out.toArray(TIntIntMap[]::new);
	}

	public static TIntIntMap select(TIntIntMap d, int... keys){
		TIntIntMap dnew = new TIntIntHashMap();
		for(int k : keys){
			if(d.containsKey(k)){
				dnew.put(k,d.get(k));
			}
		}
		return dnew;
	}
	public static TIntIntMap remove(TIntIntMap d, int... keys){
		TIntIntMap dnew = new TIntIntHashMap(d);
		for(int k : keys){
			if(d.containsKey(k)){
				dnew.remove(k);
			}
		}
		return dnew;
	}
	public static TIntIntMap[] SampleCompatible(StructuralCausalModel model, int dataSize, int maxIter){

		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());
		boolean isComp = false;

		try {
			for(int j=0; j<maxIter; j++) {
				isComp = model.isCompatible(data, 5);
				if(isComp) break;
				data = model.samples(dataSize, model.getEndogenousVars());
			}
		}catch (Exception e){}

		if(isComp) return data;
		return null;
	}

}


