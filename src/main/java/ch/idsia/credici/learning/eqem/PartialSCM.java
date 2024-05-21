package ch.idsia.credici.learning.eqem;

import java.util.Arrays;

import ch.idsia.credici.model.StructuralCausalModel;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A partially locked SCM
 */
public class PartialSCM {
	private TIntObjectMap<double[]> fixed;
	private TIntSet locked;
	
	public PartialSCM() {
		locked = new TIntHashSet();
		fixed = new TIntObjectHashMap<double[]>();
	}
		
	public void reset() {
		
	}
	
	/**
	 * is variable fully locked?
	 * @param variable
	 * @return
	 */
	boolean isLocked(int variable) {
		if (locked.contains(variable)) return true;
		if (fixed.get(variable) == null) return false;
		
		// if we are fully locked there are no NaNs. So if any is nan we return false;
		return !Arrays.stream(fixed.get(variable)).anyMatch(Double::isNaN);
	}
	
	boolean isLocked(int variable, int offset) {
		if (locked.contains(variable)) return true;
		
		double[] data = fixed.get(variable);
		
		if (data != null) {
			return !Double.isNaN(data[offset]);
		}
		return false;
	}
}
