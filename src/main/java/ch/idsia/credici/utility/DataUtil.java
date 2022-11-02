package ch.idsia.credici.utility;

import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.experiments.AsynIsCompatible;
import ch.idsia.crema.data.ReaderCSV;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.InvokerWithTimeout;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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

		if(right==null || right.getVariables().length==0)
			return getJointProb(data, left);
		if(ArraysUtil.intersection(left.getVariables(), right.getVariables()).length > 0)
				throw new IllegalArgumentException("Overlapping domains");
		BayesianFactor joint = getCounts(data, left.concat(right));
		BayesianFactor jointRight = getCounts(data, right);
		return joint.divide(jointRight).replaceNaN(0.0);
	}





	public static HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap(StructuralCausalModel model, TIntIntMap[] data ){
		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();

		for(int u: model.getExogenousVars()) {
			BayesianFactor fu = null;
			for (Object dom_ : model.getEmpiricalDomains(u)) {
				HashMap dom = (HashMap) dom_;
				int left = (int) dom.get("left");
				int[] right = (int[]) dom.get("right");

				Strides leftDom = model.getDomain((int) dom.get("left"));
				Strides rightDom = model.getDomain((int[]) dom.get("right"));

				BayesianFactor f = DataUtil.getCondProb(data, leftDom, rightDom);
				if (fu == null)
					fu = f;
				else
					fu = fu.combine(f);

			}
			empirical.put(Arrays.stream(model.getEndogenousChildren(u)).boxed().collect(Collectors.toSet()), fu);
		}

	 	return empirical;
	}


	public static TIntObjectMap<BayesianFactor> getCFactorsSplittedMap(StructuralCausalModel model, TIntIntMap[] data ){
		TIntObjectMap<BayesianFactor> cfactors = new TIntObjectHashMap<>();

		for (HashMap dom : model.getAllCFactorsSplittedDomains()) {
			int left = (int) dom.get("left");
			int[] right = (int[]) dom.get("right");

			Strides leftDom = model.getDomain((int) dom.get("left"));
			Strides rightDom = model.getDomain((int[]) dom.get("right"));
			BayesianFactor f = DataUtil.getCondProb(data, leftDom, rightDom);
			cfactors.put(left, f);
		}
		return cfactors;
	}

	public static TIntObjectMap<BayesianFactor> getCFactorsSplittedMap(StructuralCausalModel model, TIntIntMap[] data, int... exoVars){
		TIntObjectMap<BayesianFactor> cfactors = new TIntObjectHashMap<>();

		for (HashMap dom : model.getCFactorsSplittedDomains(exoVars)) {
			int left = (int) dom.get("left");
			Strides leftDom = model.getDomain((int) dom.get("left"));
			Strides rightDom = model.getDomain((int[]) dom.get("right"));
			BayesianFactor f = DataUtil.getCondProb(data, leftDom, rightDom);
			cfactors.put(left, f);
		}
		return cfactors;
	}


	public static void toCSV(String filename, TIntIntMap... data) throws IOException {

		int[] dataVars = data[0].keys();
		String[] varNames = IntStream.of(dataVars).mapToObj(i -> String.valueOf(i)).toArray(String[]::new);

		int[][] dataArray = Stream.of(ObservationBuilder.toDoubles(data, dataVars))
				.map(d -> ArraysUtil.toIntVector(d)).toArray(int[][]::new);

		new WriterCSV(dataArray, filename)
				.setVarNames(varNames).write();
	}

	public static void toCSV(String filename, String[][] data) throws IOException {
		CSVWriter writer = (CSVWriter) new CSVWriterBuilder(new FileWriter(filename))
				.withSeparator(',')
				.withQuoteChar(CSVWriter.NO_QUOTE_CHARACTER)
				.build();
		writer.writeAll(List.of(data));
		writer.close();

	}

	public static void toCSV(String filename, List<HashMap> data) throws IOException {
		toCSV(filename, CollectionTools.toStringMatrix(data));
	}


	public static TIntIntMap[] fromCSV(String filename) throws IOException, CsvException {
		ReaderCSV reader = new ReaderCSV(filename).read();
		return ObservationBuilder.observe(reader.getVarNames(), reader.getData());
	}

	public static List<HashMap<String, String>> fromCSVtoStrMap(String filename) throws IOException, CsvException {

		CSVReader reader = new CSVReaderBuilder(new FileReader(filename))
				.withCSVParser(new CSVParserBuilder().withSeparator(',').build())
				.build();

		String[] varnames = reader.readNext();
		List data = new ArrayList();

		for(String[] values : reader.readAll()){
			HashMap line = new HashMap();
			for(int i=0; i< varnames.length; i++){
				line.put(varnames[i], values[i]);
			}

			data.add(line);
		}
		return data;
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

	public static TIntIntMap[] selectColumns(TIntIntMap[] data, int... keys){
		return Arrays.stream(data).map(d -> DataUtil.select(d, keys)).toArray(TIntIntMap[]::new);
	}

	public static TIntIntMap[] selectByValue(TIntIntMap[] data, TIntIntMap selection){
		return Arrays.stream(data).filter( s -> {
				for(int k: selection.keys())
					if(selection.get(k)!=s.get(k)) return false;
				return true;
			}
		).toArray(TIntIntMap[]::new);
	}

	public static TIntIntMap[] removeColumns(TIntIntMap[] data, int... keys){
		return Arrays.stream(data).map(d -> DataUtil.remove(d, keys)).toArray(TIntIntMap[]::new);
	}

	public static TIntIntMap[] SampleCompatible(StructuralCausalModel model, int dataSize, int maxIter){

		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());
		boolean isComp = false;

		try {
			for(int j=0; j<maxIter; j++) {
				//AsynIsCompatible.setArgs(model, data);
				//		return new InvokerWithTimeout<Boolean>().run(AsynIsCompatible::run, timeout).booleanValue();
				isComp = model.isCompatible(data, 5);
				if(isComp) break;
				data = model.samples(dataSize, model.getEndogenousVars());
			}
		}catch (Exception e){
		}


		if(isComp) return data;
		return null;
	}


	public static TIntIntMap[] SampleCompatible(StructuralCausalModel model, int dataSize, int maxIter, long timeout){

		TIntIntMap[] data = null;
		boolean isComp = false;



		try {
			for(int j=0; j<maxIter; j++) {
				data = model.samples(dataSize, model.getEndogenousVars());
				AsynIsCompatible.setArgs(model, data);
				isComp = new InvokerWithTimeout<Boolean>().run(AsynIsCompatible::run, timeout).booleanValue();
				if(isComp) break;
			}
		}catch (Exception e){
		}catch (Error e){
		}



		if(isComp) return data;
		return null;
	}

	public static TIntIntMap[] renameVars(TIntIntMap[]data, int[] oldVars, int[] newVars){
		final TIntIntMap map = ObservationBuilder.observe(oldVars, newVars);
		return Arrays.stream(data).map(t -> {
			TIntIntMap newTuple = new TIntIntHashMap();
			for(int v : map.keys())
				newTuple.put(map.get(v), t.get(v));
			return newTuple;
		}).toArray(TIntIntMap[]::new);
	}

	public static TIntIntMap[] vconcatBinary(TIntIntMap[] data1, TIntIntMap[] data2){
		return Stream.concat(Stream.of(data1), Stream.of(data2)).toArray(TIntIntMap[]::new);
	}
	public static TIntIntMap[] vconcat(TIntIntMap[]...datasets){
		TIntIntMap[] out = datasets[0];
		for(int i=1; i<datasets.length; i++)
			out = vconcatBinary(out, datasets[i]);
		return out;
	}


	public static ObservationBuilder observe(int...varsAndValues){
		if(varsAndValues.length % 2 != 0)
			throw new IllegalArgumentException("Number of arguments should be pair");

		return ObservationBuilder.observe(
				IntStream.range(0, varsAndValues.length).filter(i -> i%2==0).map(i -> varsAndValues[i]).toArray(),
				IntStream.range(0, varsAndValues.length).filter(i -> i%2==1).map(i -> varsAndValues[i]).toArray()
		);
	}

	public static void main(String[] args) throws IOException {

		List<String[]> data = new ArrayList<>();
		data.add(new String[]{"cause", "effect"});
		data.add(new String[]{"0", "0"});

		toCSV("test_ig.csv", data.toArray(String[][]::new));

	}


}





