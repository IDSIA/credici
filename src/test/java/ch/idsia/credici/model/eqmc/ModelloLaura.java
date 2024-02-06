package ch.idsia.credici.model.eqmc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

public class ModelloLaura {

	public static void main(String[] args) throws IOException, InterruptedException {
		main2(args);
	}

	public static void main2(String[] args) throws IOException, InterruptedException {

		String datafile = "/Users/dhuber/Development/credici/src/test/java/ch/idsia/credici/model/eqmc/data.csv";
		TreeMap<String, Integer> cols = new TreeMap<>();
		DoubleTable data = DoubleTable.readTable(datafile, 0, ",", cols);

		int[] sizes = data.getSizes();
		int[] columns = data.getColumns();
		TIntSet[] states = data.getStates();

		for (int i = 0; i < columns.length; ++i) {
			TIntSet s = states[i];

			int mn = Arrays.stream(s.toArray()).min().getAsInt();
			int mx = Arrays.stream(s.toArray()).max().getAsInt();

			if (s.size() != mx + 1) {
				System.out.println("WOW");
			}
			sizes[i] = 2;
		}

		BigInteger m = BigInteger.valueOf(1);

		DoubleTable target = new DoubleTable(columns);
		for (var row : data.entries()) {
			int[] key = row.getKey().clone();
			for (int i = 0; i < columns.length; ++i) {
				key[i] = key[i] % sizes[i];
			}
			target.add(key,row.getValue());
		}
		
		data = target.subtable(columns);
		
		TIntList cls = new TIntArrayList();
		StructuralCausalModel model = new StructuralCausalModel("laura");
		int Lifesyle = model.addVariable(cols.size(), 10, true);
		
		int LDL = cols.get("LDL"); // [LDL|Lifestyle:sex:age]
		model.addVariable(columns[LDL], sizes[LDL], VarType.ENDOGENOUS);
		model.addParents(LDL, Lifesyle);
		cls.add(LDL);
		
		m = m.multiply(BigInteger.valueOf(sizes[LDL]));

		int BMI = cols.get("BMI"); // [BMI|Lifestyle:sex:age]
		model.addVariable(columns[BMI], sizes[BMI], VarType.ENDOGENOUS);
		model.addParents(BMI, Lifesyle);
		m = m.multiply(BigInteger.valueOf(sizes[BMI]));
		cls.add(BMI);
		
		int Pressure = cols.get("Pressure");
		model.addVariable(columns[Pressure], sizes[Pressure], VarType.ENDOGENOUS);
		model.addParents(Pressure, Lifesyle, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[Pressure]).pow(sizes[BMI]));
		cls.add(Pressure);
		
		int TG = cols.get("TG");
		model.addVariable(columns[TG], sizes[TG], VarType.ENDOGENOUS);
		model.addParents(TG, Lifesyle, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[TG]).pow(sizes[BMI]));
		cls.add(TG);
		
		int HDL = cols.get("HDL");
		model.addVariable(columns[HDL], sizes[HDL], VarType.ENDOGENOUS);
		model.addParents(HDL, Lifesyle, TG);
		m = m.multiply(BigInteger.valueOf(sizes[HDL]).pow(sizes[TG]));
		cls.add(HDL);
		
		int FBS = cols.get("FBS");
		model.addVariable(columns[FBS], sizes[FBS], VarType.ENDOGENOUS);
		model.addParents(FBS, Lifesyle, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[FBS]).pow(sizes[BMI]));
		cls.add(FBS);
		
		int Diabetes = cols.get("Diabetes");
		model.addVariable(columns[Diabetes], sizes[Diabetes], VarType.ENDOGENOUS);
		model.addParents(Diabetes, Lifesyle, BMI, FBS);
		m = m.multiply(BigInteger.valueOf(sizes[Diabetes]).pow(sizes[BMI] * sizes[FBS]));
		cls.add(Diabetes);
		
		data = data.subtable(cls.toArray());//.scale();
		
		
		
		DetailedDotSerializer.saveModel("out.png", new Info().model(model));
		TIntIntMap s = new TIntIntHashMap();
		s.put(Lifesyle, 10);

		EQEMLearner eq = new EQEMLearner(model, data, s, true);
		eq.setConfig(new Config().deterministic(false).numPSCMRuns(0).numRun(100).numIterations(100000000));
//		TreeMap<String, TDoubleList>

		var cc = eq.run((sol) -> {

			if (sol.stage().success()) {
				System.out.println();
				System.out.println(sol.iterations() +" " + sol.loklikelihood() + " - " + sol.llmax());
			} else {
				System.out.println("FAIL");
			}
		}
		);

		var iter = cc.exaustiveIterator();
		iter.next();
//	("[Lifestyle][age][sex]
//	[LDL|Lifestyle:sex:age]
//	[BMI|Lifestyle:sex:age]
//	[Pressure|Lifestyle:sex:age:BMI]
//	[TG|Lifestyle:sex:BMI]
//	[HDL|Lifestyle:sex:TG]
//	[FBS|Lifestyle:age:BMI]
//	[Diabetes|Lifestyle:BMI:FBS]")
	}

	public static void main1(String[] args) throws IOException, InterruptedException {

		String datafile = "/Users/dhuber/Development/credici/src/test/java/ch/idsia/credici/model/eqmc/data.csv";
		TreeMap<String, Integer> cols = new TreeMap<>();
		DoubleTable data = DoubleTable.readTable(datafile, 0, ",", cols);
		int[] sizes = data.getSizes();
		int[] columns = data.getColumns();
		TIntSet[] states = data.getStates();

		for (int i = 0; i < columns.length; ++i) {
			TIntSet s = states[i];

			int mn = Arrays.stream(s.toArray()).min().getAsInt();
			int mx = Arrays.stream(s.toArray()).max().getAsInt();
			int sz = sizes[i];
			if (s.size() != mx + 1) {
				System.out.println("WOW");
			}
		}

		BigInteger m = BigInteger.valueOf(1);

		StructuralCausalModel model = new StructuralCausalModel("laura");
		int Lifesyle = model.addVariable(cols.size(), 2, true);
		int UAge = model.addVariable(cols.size() + 1, 2, true);
		int USex = model.addVariable(cols.size() + 2, 2, true);

		int age = cols.get("age");
		model.addVariable(columns[age], sizes[age], VarType.ENDOGENOUS);
		model.addParent(age, UAge);

		int sex = cols.get("sex");
		model.addVariable(columns[sex], sizes[sex], VarType.ENDOGENOUS);
		model.addParent(sex, USex);

		int LDL = cols.get("LDL"); // [LDL|Lifestyle:sex:age]
		model.addVariable(columns[LDL], sizes[LDL], VarType.ENDOGENOUS);
		model.addParents(LDL, Lifesyle, age, sex);

		m = m.multiply(BigInteger.valueOf(sizes[LDL]).pow(sizes[age] * sizes[sex]));

		int BMI = cols.get("BMI"); // [BMI|Lifestyle:sex:age]
		model.addVariable(columns[BMI], sizes[BMI], VarType.ENDOGENOUS);
		model.addParents(BMI, Lifesyle, age, sex);
		m = m.multiply(BigInteger.valueOf(sizes[BMI]).pow(sizes[age] * sizes[sex]));

		int Pressure = cols.get("Pressure");
		model.addVariable(columns[Pressure], sizes[Pressure], VarType.ENDOGENOUS);
		model.addParents(Pressure, Lifesyle, age, sex, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[Pressure]).pow(sizes[age] * sizes[sex] * sizes[BMI]));

		int TG = cols.get("TG");
		model.addVariable(columns[TG], sizes[TG], VarType.ENDOGENOUS);
		model.addParents(TG, Lifesyle, sex, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[TG]).pow(sizes[BMI] * sizes[sex]));

		int HDL = cols.get("HDL");
		model.addVariable(columns[HDL], sizes[HDL], VarType.ENDOGENOUS);
		model.addParents(HDL, Lifesyle, sex, TG);
		m = m.multiply(BigInteger.valueOf(sizes[HDL]).pow(sizes[TG] * sizes[sex]));

		int FBS = cols.get("FBS");
		model.addVariable(columns[FBS], sizes[FBS], VarType.ENDOGENOUS);
		model.addParents(FBS, Lifesyle, age, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[FBS]).pow(sizes[age] * sizes[BMI]));

		int Diabetes = cols.get("Diabetes");
		model.addVariable(columns[Diabetes], sizes[Diabetes], VarType.ENDOGENOUS);
		model.addParents(Diabetes, Lifesyle, BMI, FBS);
		m = m.multiply(BigInteger.valueOf(sizes[Diabetes]).pow(sizes[BMI] * sizes[FBS]));

		TIntIntMap s = new TIntIntHashMap();
		s.put(Lifesyle, 10);
		s.put(USex, sizes[sex]);
		s.put(UAge, sizes[age]);

		EQEMLearner eq = new EQEMLearner(model, data, s, true);
		eq.setConfig(new Config().deterministic(false).numPSCMRuns(0).numRun(100).numIterations(10000));
//		TreeMap<String, TDoubleList>

		var cc = eq.run((sol) -> {

			if (sol.stage().success()) {
				System.out.println();
				System.out.println(sol.loklikelihood() + " - " + sol.llmax());
			} else {
				System.out.println("FAIL");
			}
		}
//				
//				d.set(Math.min(llmax - sol.loglikelihood, dd));
//				if (llmax - sol.loglikelihood < 0.01) {
//					cc.addResult(sol.model);	
//					accepted.incrementAndGet();
//				} else {
//					System.out.println("reject " + llmax + " " + sol.loglikelihood);
//					rejected.incrementAndGet();
//				}
//			}
//
//			counts.compute(sol.stage, (stage, cnt)->cnt == null ? 1: cnt+1);
//		}
		);

		var iter = cc.exaustiveIterator();
		iter.next();
//	("[Lifestyle][age][sex]
//	[LDL|Lifestyle:sex:age]
//	[BMI|Lifestyle:sex:age]
//	[Pressure|Lifestyle:sex:age:BMI]
//	[TG|Lifestyle:sex:BMI]
//	[HDL|Lifestyle:sex:TG]
//	[FBS|Lifestyle:age:BMI]
//	[Diabetes|Lifestyle:BMI:FBS]")
	}

	public static void main3(String[] args) throws IOException, InterruptedException {

		String datafile = "/Users/dhuber/Development/credici/src/test/java/ch/idsia/credici/model/eqmc/data.csv";
		TreeMap<String, Integer> cols = new TreeMap<>();
		DoubleTable data = DoubleTable.readTable(datafile, 0, ",", cols);
		int[] sizes = data.getSizes();
		int[] columns = data.getColumns();

		BigInteger m = BigInteger.valueOf(1);

		StructuralCausalModel model = new StructuralCausalModel("laura");
		int Lifesyle = model.addVariable(cols.size(), 2, true);
		int BMI = cols.get("BMI"); // [BMI|Lifestyle:sex:age]
		model.addVariable(columns[BMI], sizes[BMI], VarType.ENDOGENOUS);
		model.addParents(BMI, Lifesyle);

		int FBS = cols.get("FBS");
		model.addVariable(columns[FBS], sizes[FBS], VarType.ENDOGENOUS);
		model.addParents(FBS, Lifesyle, BMI);
		m = m.multiply(BigInteger.valueOf(sizes[FBS]).pow(sizes[BMI]));

		int Diabetes = cols.get("Diabetes");
		model.addVariable(columns[Diabetes], sizes[Diabetes], VarType.ENDOGENOUS);
		model.addParents(Diabetes, Lifesyle, BMI, FBS);
		m = m.multiply(BigInteger.valueOf(sizes[Diabetes]).pow(sizes[BMI] * sizes[FBS]));

		DoubleTable dta = data.subtable(new int[] { BMI, FBS, Diabetes });

		TIntIntMap s = new TIntIntHashMap();
		s.put(Lifesyle, 100);

		EQEMLearner eq = new EQEMLearner(model, dta, s, true);
		eq.setConfig(new Config().deterministic(false).numPSCMRuns(0).numRun(100).numIterations(10000));
		var cc = eq.run();
		var iter = cc.exaustiveIterator();
		iter.next();

	}
}
