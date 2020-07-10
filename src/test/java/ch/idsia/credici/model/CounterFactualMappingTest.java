package ch.idsia.credici.model;

import ch.idsia.credici.model.counterfactual.WorldMapping;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class CounterFactualMappingTest {

	WorldMapping cfmapping;
	StructuralCausalModel merged;

	@Before
	public void init(){

		StructuralCausalModel cmodel = new StructuralCausalModel();

		cmodel.addVariables(2,2);
		cmodel.addVariable(2,true);
		cmodel.addParent(0,2);
		cmodel.addParent(1,0);
		cmodel.addParent(1,2);

		cmodel.fillWithRandomFactors(2);
		StructuralCausalModel[] models = {CausalOps.intervention(cmodel, 0,0, false), CausalOps.intervention(cmodel, 1,1, false)};

		merged = cmodel.merge(models);
		cfmapping = merged.getMap();
		System.out.println();
		//////
	}

	@Test
	public void testGetVariables() {
		assertTrue(Arrays.equals(cfmapping.getVariablesIn(1), new int[]{2,3,4}));
		assertTrue(Arrays.equals(cfmapping.getVariables(), new int[]{0,1,2,3,4,5,6}));
	}

	@Test
	public void testGetWorld() {
		assertTrue(cfmapping.getWorld(3)==1);
		assertTrue(cfmapping.getWorld(2)==cfmapping.ALL);
		assertTrue(cfmapping.varInWorld(5,2));
		assertTrue(cfmapping.varInWorld(5,2));
		assertTrue(!cfmapping.varInWorld(0,2));
	}

	@Test
	public void testEquiv() {
		assertTrue(cfmapping.getEquivalentVars(0,3)==0);
	}

	@Test
	public void testRemoveSet() {
		cfmapping.remove(3);
		assertTrue(Arrays.equals(cfmapping.getVariablesIn(1), new int[]{2,4}));
		assertTrue(cfmapping.getEquivalentVars(1,0)==cfmapping.None);
		cfmapping.set(3,1,0);
		assertTrue(Arrays.equals(cfmapping.getVariablesIn(1), new int[]{2,3,4}));
		assertTrue(cfmapping.getEquivalentVars(1,0) == 3);
	}


	@Test
	public void testMapDomains(){
		assertTrue(merged.correctFactorDomains());
	}





}
