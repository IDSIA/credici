package ch.idsia.credici.model.transform;


import org.junit.Test;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

public class TestCComponents {

	@Test
	public void testApplyStructuralCausalModelDoubleTable() {
		StructuralCausalModel model = new StructuralCausalModel("test");
		int x0 = model.addVariable(2);
		int x1 = model.addVariable(2);
		int x2 = model.addVariable(2);
		int x3 = model.addVariable(2);
		
		int u1 = model.addVariable(4, true);
		int u2 = model.addVariable(4, true);
		
		model.addParent(x1, x0);
		model.addParent(x1, u1);
		model.addParent(x0, u1);

		model.addParent(x2, x0);
		model.addParent(x3, x1);
		
		model.addParent(x3, x2);
		model.addParent(x2, u2);
		model.addParent(x3, u2);
		
		CComponents cc = new CComponents();
		List<StructuralCausalModel> x = cc.apply(model);
		
		for (var m :x) {
			DetailedDotSerializer.saveModel(m, null, "/Users/dhuber/m.png");
			m.getEmpiricalNet();
		}
	}

	@Test
	public void x() {
		assertEquals(1, 1);
	}
}
