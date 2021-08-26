package ch.idsia.credici.factor;


import ch.idsia.crema.core.Domain;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianDefaultFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;

/**
 * Author:  Rafael Cabañas
 */
public class BayesianFactorFactoryMutableExt {
	private double[] data = null;
	private double[] logData = null;

	private Strides domain = Strides.empty();

	private BayesianFactorFactoryMutableExt() {
	}

	public static BayesianFactorFactoryMutableExt factory() {
		return new BayesianFactorFactoryMutableExt();
	}

	public BayesianFactorFactoryMutableExt domain(Domain domain) {
		this.domain = Strides.fromDomain(domain);
		return data();
	}

	public BayesianFactorFactoryMutableExt domain(int[] domain, int[] sizes) {
		this.domain = new Strides(domain, sizes);
		return data();
	}

	public BayesianFactorFactoryMutableExt data() {
		this.data = new double[domain.getCombinations()];
		return this;
	}

	public BayesianFactorFactoryMutableExt data(double[] data) {
		final int expectedLength = domain.getCombinations();
		if (data.length > expectedLength)
			throw new IllegalArgumentException("Invalid length of data: expected " + expectedLength + " got " + data.length);

		if (this.data == null)
			this.data = new double[expectedLength];

		// TODO: do we want to allow to assign LESS data than expected? For now yes
		System.arraycopy(data, 0, this.data, 0, data.length);
		return this;
	}

	public BayesianFactorFactoryMutableExt logData(double[] data) {
		final int expectedLength = domain.getCombinations();
		if (data.length != expectedLength)
			throw new IllegalArgumentException("Invalid length of data: expected " + expectedLength + " got " + data.length);

		if (this.logData == null)
			this.logData = new double[expectedLength];

		// TODO: see data(double[])
		System.arraycopy(data, 0, this.logData, 0, data.length);
		return this;
	}

	public BayesianFactorFactoryMutableExt data(int[] domain, double[] data) {
		int[] sequence = ArraysUtil.order(domain);

		// this are strides for the iterator so we do not need them one item longer
		int[] strides = new int[domain.length];
		int[] sizes = new int[domain.length];

		int[] this_variables = this.domain.getVariables();
		int[] this_sizes = this.domain.getSizes();
		int[] this_strides = this.domain.getStrides();

		// with sequence we can now set sorted_domain[1] = domain[sequence[1]];
		for (int index = 0; index < this_variables.length; ++index) {
			int newindex = sequence[index];
			strides[newindex] = this_strides[index];
			sizes[newindex] = this_sizes[index];
		}

		final int combinations = this.domain.getCombinations();
		IndexIterator iterator = new IndexIterator(strides, sizes, 0, combinations);

		double[] target = data.clone();

		for (int index = 0; index < combinations; ++index) {
			int other_index = iterator.next();
			target[other_index] = data[index];
		}

		return data(target);
	}

	public BayesianFactorFactoryMutableExt value(double value, int... states) {
		data[domain.getOffset(states)] = value;
		return this;
	}

	public BayesianFactorFactoryMutableExt valueAt(double d, int index) {
		data[index] = d;
		return this;
	}

	public BayesianFactorFactoryMutableExt set(double value, int... states) {
		return value(value, states);
	}

	public BayesianDefaultFactor get() {
		return new BayesianDefaultFactor(domain, data);
	}

	public double[] getData() {
		return data;
	}

	public Strides getDomain() {
		return domain;
	}


}
