package ch.idsia.credici.utility.table;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class TestTable  {

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
		assertArrayEquals(new double[] { 1, 2, 3 }, data, 0.0001);
	}
}
