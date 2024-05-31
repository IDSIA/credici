package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.utility.table.DoubleTable;

public class PearlNetwork {
	public static final int gender = 0;
	public static final int treatment = 1;
	public static final int recovery = 2;

	
	private int u_gender;
	private int u_treatment;
	private int u_recovery;
	
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
		return table;
	}

	
	private StructuralCausalModel createModel() {
		StructuralCausalModel model = new StructuralCausalModel();
		model.addVariable(gender, 2);
		model.addVariable(treatment, 2);
		model.addVariable(recovery, 2);

		u_gender = model.addVariable(2, true);
		u_treatment = model.addVariable(2, true);
		u_recovery = model.addVariable(2, true);

		model.addParents(gender, u_gender);
		model.addParents(treatment, gender, u_treatment);
		model.addParents(recovery, gender, treatment, u_recovery);

		return model;
	}
	
	public StructuralCausalModel createCanonicalModel() {
		return Canonical.LOG.apply(createModel());
	}
}
