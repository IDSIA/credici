package ch.idsia.credici.learning.eqem.fixing;

import org.apache.commons.lang3.tuple.Triple;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.DoubleTable;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;

public interface EquationFixing {
	public Triple<Integer, Integer, Integer>  choose(StructuralCausalModel model, DoubleTable data, TIntObjectMap<TIntSet> endoLocked);
	
	// get fixeable variable-offset-state triplets that will not
	// render the observation of the dataset impossible. 
	// if none available then fail

	// each fixed columns is compatible with a subset of rows. 
	
	// each row of the dataset identifies a number of compatible columns
	
	/**
	 * Find argmax of the data in the specified array using strides and size to
	 * advance.
	 * 
	 * The data can be normal or in log space.
	 * 
	 * @param data   double[] the source data
	 * @param offset int start offset 
	 * @param stride int item stride
	 * @param size   int number of items
	 * @return the state of the variable with max value
	 */
	static int argmax(double[] data, int offset, int stride, int size) {
		int index = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < size; ++i) {
			int idx = offset + stride * i;
			if (data[idx] > max) {
				max = data[idx];
				index = i;
			}
		}
		return index;
	}

}
