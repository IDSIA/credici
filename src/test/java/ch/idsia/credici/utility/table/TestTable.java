package ch.idsia.credici.utility.table;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import gnu.trove.map.hash.TIntIntHashMap;

public class TestTable {

	@Test
	public void getWeights() {
		Table table = new Table(new int[] { 4, 2, 1 });
		table.add(new int[] { 0, 0, 0 }, 1.5);
		table.add(new int[] { 0, 1, 0 }, 0.5);
		table.add(new int[] { 0, 2, 1 }, 0.5);
		table.add(new int[] { 0, 2, 1 }, 0.5);
		table.add(new int[] { 1, 1, 0 }, 2.1);
		table.add(new int[] { 1, 2, 1 }, 0.4);

		double[] data = table.getWeights(new int[] { 1, 2 }, new int[] { 2, 3 });
		assertArrayEquals(new double[] { 1.5, 0, 2.6, 0, 0, 1.4 }, data, 0.0001);
	}

	@Test
	public void add() {
		Table table = new Table(new int[] { 4, 2, 1 });
		table.add(new TIntIntHashMap(new int[] { 4, 5, 1 }, new int[] { 2, 3, 1 }));
		var e = table.iterator().next();
		assertArrayEquals(new int[] { 2, 0, 1 }, e.getKey());

		// add another matching row (no new row added)
		table.add(new TIntIntHashMap(new int[] { 4,6, 2, 1 }, new int[] { 2,4, 0, 1 }));
		
		// iterate over the rows
		var i = table.iterator();
		e = i.next();
		assertArrayEquals(new int[] { 2, 0, 1 }, e.getKey());
		assertEquals(2.0, e.getValue(),0.00001);
		
		// no more elements
		assertFalse(i.hasNext());
	}
}
