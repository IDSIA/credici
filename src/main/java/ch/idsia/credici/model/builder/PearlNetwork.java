package ch.idsia.credici.model.builder;

import java.util.function.IntFunction;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.utility.table.DoubleTable;

public class PearlNetwork {
	public static final int gender = 0;
	public static final int treatment = 1;
	public static final int recovery = 2;

	public static String[] names2 = new String[] { "gender(0)", "treatment(1)", "recovery(2)", "U_gender(3)", "U_treatment(4)", "U_recovery(5)" };
	public static String[] names = new String[] { "gender_0", "treatment_1", "recovery_2", "U_gender_3", "U_treatment_4", "U_recovery_5" };
	public static String[] alternativeNames = new String[] { "Z", "X", "Y", "U_z", "U_x", "U_y" };
	
	private int u_gender=3;
	private int u_treatment=4;
	private int u_recovery=5;
	
//	Gender (Z)	Treatment (X)	Recovery (Y)	#
//	0	0	0	2
//	0	0	1	114
//	0	1	0	41
//	0	1	1	313
//	1	0	0	107
//	1	0	1	13
//	1	1	0	109
//	1	1	1	1
	public DoubleTable createData() {
		DoubleTable table = new DoubleTable(new int[] { gender, treatment, recovery });
		table.add(new int[] { 0, 0, 0 }, 2d);
		table.add(new int[] { 0, 0, 1 }, 114d);
		table.add(new int[] { 0, 1, 0 }, 41d);
		table.add(new int[] { 0, 1, 1 }, 313d);
		table.add(new int[] { 1, 0, 0 }, 107d);
		table.add(new int[] { 1, 0, 1 }, 13d);
		table.add(new int[] { 1, 1, 0 }, 109d);
		table.add(new int[] { 1, 1, 1 }, 1d);
		table.setNames(names);
		return table;
	}

	public static IntFunction<String> getNaming() {
		return (i)-> names[i];
	}
	

	public static IntFunction<String> getAlternativeNaming() {
		return (i)-> alternativeNames[i];
	}
	
	private StructuralCausalModel createModel() {
		StructuralCausalModel model = new StructuralCausalModel();
		model.addVariable(gender, 2);
		model.addVariable(treatment, 2);
		model.addVariable(recovery, 2);

		model.addVariable(u_gender, 2, true);
		model.addVariable(u_treatment, 2, true);
		model.addVariable(u_recovery, 2, true);

		model.addParents(gender, u_gender);
		model.addParents(treatment, gender, u_treatment);
		model.addParents(recovery,  gender, treatment, u_recovery);

		return model;
	}
	
	public StructuralCausalModel createCanonicalModel() {
		return Canonical.LOG.apply(createModel());
	}
}
