package ch.idsia.credici.utility.apps;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import ch.idsia.credici.learning.eqem.ComponentEM;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.credici.utility.table.ListTable;
import ch.idsia.credici.utility.table.MinMaxTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

public class ModelloLaura {

	public static void main(String[] args) throws IOException, InterruptedException {
		main_binary(args);
	}

	static int[] where(int[] needle, int[] haystack) {
		int[] ret = new int[needle.length];
		for (int i = 0; i < needle.length; ++i) {
			ret[i] = ArraysUtil.indexOf(needle[i], haystack);
		}
		return ret;
	}

	/**
	 * 
	 * @param model
	 * @param data
	 * @param left_cols
	 * @param lifestyle
	 * @param diabetes
	 * @param mapping   - a mapping between left and star worlds. Keys are the non
	 *                  starred variables in the data
	 * @return map of row -> [ diabetes, diabetes_star ]
	 */
	public static TreeMap<int[], double[]> query(StructuralCausalModel model, DoubleTable data, int[] left_cols,
			int exo, int target, TIntIntMap mapping, int state, boolean useRealDiabetes) {
		int[] seq = new MinFillOrdering().apply(model);

		TreeMap<int[], BayesianFactor> pUs = new TreeMap<int[], BayesianFactor>(Arrays::compare);
		TreeMap<int[], BayesianFactor> pDiabetes = new TreeMap<int[], BayesianFactor>(Arrays::compare);
		TreeMap<int[], double[]> pDiabetes_star = new TreeMap<int[], double[]>(Arrays::compare);

		DoubleTable left = data.subtable(left_cols);
		for (var row : left.entries()) {
			int[] states = row.getKey();
			double count = row.getValue();

			TIntIntMap ev_no_dia = new TIntIntHashMap(left_cols, states);
			ev_no_dia.remove(target);

			VE<BayesianFactor> ve = new VE<>(seq);
			ve.setEvidence(ev_no_dia);
			ve.setFactors(model.getFactors());
			ve.setNormalize(true);
			var pdia = ve.run(target);
			pDiabetes.put(states, pdia);

			ve = new VE<>(seq);
			if (useRealDiabetes) {
				ve.setEvidence(new TIntIntHashMap(left_cols, states));
			} else {
				ve.setEvidence(ev_no_dia);
			}
			ve.setFactors(model.getFactors());
			ve.setNormalize(true);

			var pU = ve.run(exo);
			pUs.put(states, pU);
		}

		for (var entry : data.mapIterable()) {
			TIntIntMap row = entry.getKey();

			var left_states = Arrays.stream(left_cols).map(row::get).toArray();

			var pU = pUs.get(left_states);

			model.copy();
			model.setFactor(exo, pU);

			// build evidence
			final TIntIntMap evidence = new TIntIntHashMap(left_cols, left_states);
			mapping.forEachEntry((s, t) -> {
				evidence.put(s, row.get(t));
				return true;
			});
			evidence.remove(target);

			VE<BayesianFactor> ve = new VE<>(seq);
			ve.setEvidence(evidence); // full evidence
			ve.setFactors(model.getFactors());
			ve.setNormalize(true);
			BayesianFactor pDia_star = ve.run(target);

			var pDia = pDiabetes.get(left_states);

			int[] row_arr = Arrays.stream(data.getColumns()).map(row::get).toArray();

			if (pDiabetes_star.containsKey(row_arr))
				System.out.println("WTF");

			double[] values = new double[] { pDia.getValue(state), pDia_star.getValue(state),
					pDia_star.getValue(state) - pDia.getValue(state) };
			pDiabetes_star.put(row_arr, values);

		}
		return pDiabetes_star;
	}

	private static double[] minmax(BayesianFactor factor, int state, double[] current) {
		double value = factor.getValue(state);

		if (current == null) {
			return new double[] { value, value };
		} else {
			current[0] = Math.min(current[0], value);
			current[1] = Math.max(current[1], value);
			return current;
		}
	}

	public static DoubleTable remap(DoubleTable data, TIntObjectMap<int[]> threshold_lessequal) {
		final int[] columns = data.getColumns();

		int[] chid = IntStream.range(0, columns.length).filter(i -> threshold_lessequal.containsKey(columns[i]))
				.toArray();

		DoubleTable target = new DoubleTable(columns);
		for (var row : data.entries()) {
			int[] key = row.getKey().clone();
			for (int i : chid) {
				int col = columns[i];
				int[] th = threshold_lessequal.get(col);
				int state = 0;
				for (state = 0; state < th.length; ++state)
					if (key[i] <= th[state])
						break;
				key[i] = state;
			}
			target.add(key, row.getValue());
		}
		return target;
	}

	public static DoubleTable binarize(DoubleTable data, TIntIntMap thresholds) {
		final TIntObjectMap<int[]> map = new TIntObjectHashMap<int[]>();

		thresholds.forEachEntry((k, v) -> {
			map.put(k, new int[] { v });
			return true;
		});

		return remap(data, map);
//		final int[] columns = data.getColumns();
//
//		int[] chid = IntStream.range(0, columns.length).filter(i -> thresholds.containsKey(columns[i])).toArray();
//
//		DoubleTable target = new DoubleTable(columns);
//		for (var row : data.entries()) {
//			int[] key = row.getKey().clone();
//			for (int i : chid) {
//				int col = columns[i];
//				int th = thresholds.get(col);
//				key[i] = key[i] <= th ? 0 : 1;
//			}
//			target.add(key, row.getValue());
//		}
//		return target;
	}

	public static DoubleTable filter(DoubleTable data, TIntIntMap conditions) {
		final int[] columns = data.getColumns();

		int[] chid = IntStream.range(0, columns.length).filter(i -> conditions.containsKey(columns[i])).toArray();

		DoubleTable target = new DoubleTable(columns);
		row: for (var row : data.entries()) {
			int[] key = row.getKey().clone();
			for (int i : chid) {
				int col = columns[i];
				if (conditions.get(col) != key[i])
					continue row;
			}
			target.add(key, row.getValue());
		}
		return target;
	}

	public static void main_binary(String[] args) throws IOException, InterruptedException {

		String datafile = args[0];// "/Users/dhuber/Development/credici/src/test/java/ch/idsia/credici/model/eqmc/data_smaller.csv";
		int stcnt = Integer.parseInt(args[1]); // 128
		int iter = Integer.parseInt(args[2]); // 200
		double eps = Double.parseDouble(args[3]); // 0.000001
		String file = args[4];
		int ssex = Integer.parseInt(args[5]);
		int sage = Integer.parseInt(args[6]);
		int BMI_states = Integer.parseInt(args[7]);

		boolean useRealDiabetes = "WithDia".equals(args[8]);

		System.out.println(Arrays.toString(args));
		System.out.println("-------------------------------------");

		final TreeMap<String, Integer> cols = new TreeMap<>();
		DoubleTable fulldata = DoubleTable.readTable(datafile, 0, ",", cols);

		TreeMap<Integer, String> colNames = new TreeMap<Integer, String>();
		cols.entrySet().forEach(v -> colNames.put(v.getValue(), v.getKey()));

		String[] names = { "LDL", "BMI", "Pressure", "Diabetes", "TG", "HDL", "FBS", "BMI_final", "FBS_final", "age",
				"sex" };
		String[] names_inf = { "LDL", "BMI", "Pressure", "Diabetes", "TG", "HDL", "FBS" };
		String[] names_star = { "LDL", "BMI", "Pressure", "Diabetes", "TG", "HDL", "FBS", "BMI_final", "FBS_final" };

		int[] interest = Arrays.stream(names).map(cols::get).mapToInt(Integer::intValue).toArray();
		int[] interest_inf = Arrays.stream(names_inf).map(cols::get).mapToInt(Integer::intValue).toArray();
		int[] interest_query = Arrays.stream(names_star).map(cols::get).mapToInt(Integer::intValue).toArray();

		var data = fulldata.subtable(interest);

		TIntIntMap thresholds = new TIntIntHashMap();
		thresholds.put(cols.get("age"), 1);
		thresholds.put(cols.get("Pressure"), 1);
		thresholds.put(cols.get("TG"), 1);

		thresholds.put(cols.get("LDL"), 2);

		thresholds.put(cols.get("HDL"), 0);
		thresholds.put(cols.get("FBS"), 0);
		thresholds.put(cols.get("FBS_final"), 0);
		DoubleTable target = binarize(data, thresholds);

//		classe 0: BMI <=1
//		classe 1: BMI = 2
//		classe 2: BMI >=3 #default
		TIntObjectMap<int[]> remap_states = new TIntObjectHashMap<int[]>();
		switch (BMI_states) {
		case 2:
			remap_states.put(cols.get("BMI"), new int[] { 2 });
			remap_states.put(cols.get("BMI_final"), new int[] { 2 });
			break;
		case 3:
			remap_states.put(cols.get("BMI"), new int[] { 1, 2 }); // results in 3 states
			remap_states.put(cols.get("BMI_final"), new int[] { 1, 2 }); // results in 3 states
			break;
		case 5:
			remap_states.put(cols.get("BMI"), new int[] { 1, 2, 3, 4 }); // results in 5 states
			remap_states.put(cols.get("BMI_final"), new int[] { 1, 2, 3, 4 }); // results in 5 states
			break;
		default:
			System.exit(-1);
		}
	
		target = remap(target, remap_states);
	
		int[] sexes = new int[] { 0, 1 };
		int[] ages = new int[] { 0, 1 };

		for (int s : sexes) {
			for (int a : ages) {
				TIntIntMap insta = new TIntIntHashMap();
				insta.put(cols.get("sex"), s);
				insta.put(cols.get("age"), a);

				DoubleTable tmp = filter(target, insta);
				tmp = tmp.subtable(interest_inf);
				System.out.println("Subproblem size " + s + " " + a + " is " + tmp.size());
			}
		}

		TIntIntMap insta = new TIntIntHashMap();
		insta.put(cols.get("sex"), ssex);
		insta.put(cols.get("age"), sage);
		target = filter(target, insta);

		// ###############################################################
		// learning

		data = target.subtable(interest_inf);

		int[] sizes = data.getSizes();
		int[] columns = data.getColumns();
		TIntSet[] states = data.getStates();

		for (int i = 0; i < columns.length; ++i) {
			TIntSet s = states[i];

			int mn = Arrays.stream(s.toArray()).min().getAsInt();
			int mx = Arrays.stream(s.toArray()).max().getAsInt();

			System.out.println("Variable " + columns[i] + " has states " + sizes[i]);
		}

		StructuralCausalModel model = new StructuralCausalModel("laura");

		int LDL = cols.get("LDL"); // [LDL|Lifestyle:sex:age]
		int iLDL = ArraysUtil.indexOf(LDL, columns);
		model.addVariable(LDL, sizes[iLDL], VarType.ENDOGENOUS);

		int BMI = cols.get("BMI"); // [BMI|Lifestyle:sex:age]
		int iBMI = ArraysUtil.indexOf(BMI, columns);
		model.addVariable(BMI, sizes[iBMI], VarType.ENDOGENOUS);

		int Pressure = cols.get("Pressure");
		int iPressure = ArraysUtil.indexOf(Pressure, columns);
		model.addVariable(Pressure, sizes[iPressure], VarType.ENDOGENOUS);

		int TG = cols.get("TG");
		int iTG = ArraysUtil.indexOf(TG, columns);
		model.addVariable(TG, sizes[iTG], VarType.ENDOGENOUS);

		int HDL = cols.get("HDL");
		int iHDL = ArraysUtil.indexOf(HDL, columns);
		model.addVariable(HDL, sizes[iHDL], VarType.ENDOGENOUS);

		int FBS = cols.get("FBS");
		int iFBS = ArraysUtil.indexOf(FBS, columns);
		model.addVariable(FBS, sizes[iFBS], VarType.ENDOGENOUS);

		final int Diabetes = cols.get("Diabetes");
		int iDiabetes = ArraysUtil.indexOf(Diabetes, columns);
		model.addVariable(Diabetes, sizes[iDiabetes], VarType.ENDOGENOUS);

		int Lifesyle = model.addVariable(cols.size(), true);
		// colNames.put(Lifesyle, "Lifestyle");

		model.addParents(LDL, Lifesyle);
		model.addParents(BMI, Lifesyle);
		model.addParents(Pressure, Lifesyle, BMI);
		model.addParents(TG, Lifesyle, BMI);
		model.addParents(HDL, Lifesyle, TG);
		model.addParents(FBS, Lifesyle, BMI);
		model.addParents(Diabetes, Lifesyle, BMI, FBS);

		for (int v : model.getEndogenousVars()) {
			System.out.println("Size " + v + " " + model.getSize(v));
		}
//		data = target.subtable(model.getEndogenousVars(true));// .scale();

//		DetailedDotSerializer.saveModel("out.png", new Info().model(model).nodeName(colNames::get));

		TIntIntMap s = new TIntIntHashMap();
		s.put(Lifesyle, stcnt);

		var config = new Config().deterministic(false).numPSCMRuns(0).numRun(iter).numIterations(100000000).llEPS(eps);
		EQEMLearner eq = new EQEMLearner(model, data, s, true, config);

		var log = Logger.getLogger(ModelloLaura.class);
		log.info("Run EMCC");

		eq.setDebugLoggerGenerator((c) -> (igen) -> {
			var i = igen.get();
			StructuralCausalModel mm = (StructuralCausalModel) i.getModel();
//			if (i.getIterations() % 1000 == 0) {
				int ite = i.getIterations() + 1;
				System.out.println(i.getTime() + " " + i.getIterations() + " " + mm.getData(ComponentEM.LL_DATA));
//			}
		});

		// ###############################################################
		// getting ready to query

		final var query_data = target.subtable(interest_query);

		final TIntIntMap query_mapping = new TIntIntHashMap();
		query_mapping.put(cols.get("BMI"), cols.get("BMI_final"));
		query_mapping.put(cols.get("FBS"), cols.get("FBS_final"));

		// ###############################################################
		// learn and query
		MinMaxTable table = new MinMaxTable(query_data.getColumns(), 3);
		ListTable<Double, Double> intermediate = new ListTable<Double, Double>(query_data.getColumns());
		
		var cc = eq.run((sol) -> {
			if (sol.getStage().success()) {
				System.out.println();
				System.out.println(sol.getIterations() + " " + sol.getLogLikelihood() + " vs " + sol.getLLmax());
				if (sol.isAccepted()) {
					System.out.println("Accepted");
					// hell yeah
					StructuralCausalModel scm = (StructuralCausalModel) sol.getModel();
					
					var q = query(scm, query_data, interest_inf, Lifesyle, Diabetes, query_mapping, 1, useRealDiabetes);
					
					q.entrySet().stream().forEach((e) -> intermediate.add(e.getKey(), e.getValue()[1]));
					
					table.addAll(q);
				}
				//intermediate.toCSV("F_" + sol.getIterations() + ".csv", colNames::get);
			} else {
				System.out.println("FAIL");
			}
		});

		table.toCSV(file, colNames::get, "Pdiabetes", "Pdiabetes_star", "Pdelta");

//("[Lifestyle][age][sex]
//[LDL|Lifestyle:sex:age]
//[BMI|Lifestyle:sex:age]
//[Pressure|Lifestyle:sex:age:BMI]
//[TG|Lifestyle:sex:BMI]
//[HDL|Lifestyle:sex:TG]
//[FBS|Lifestyle:age:BMI]
//[Diabetes|Lifestyle:BMI:FBS]")
	}
}
