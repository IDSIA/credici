package ch.idsia.credici.factor;

import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.OperableFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorFactory;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class Operations {

	public static VertexFactor addVertexAt(VertexFactor f, double[] values, int parentsState){
		VertexFactorFactory factory = VertexFactorFactory.factory().domain(f.getDataDomain(), f.getSeparatingDomain());

		for(int i=0; i< f.getSeparatingDomain().getCombinations(); i++){
			List<double[]> vertices = null;
			if(f.getVertices(i) != null) {
				vertices = List.of(f.getVertices(i));
				for (double[] v : vertices)
					factory.addVertex(v, i);
			}

			if(i==parentsState)
				factory.addVertex(values, i);

		}

		return factory.get();

	}


	public static BayesianFactor sampleVertex(VertexFactor f){

		int left_comb = f.getSeparatingDomain().getCombinations();

		int idx[] = IntStream.range(0,left_comb)
				.map(i-> RandomUtil.getRandom().nextInt(f.getVerticesAt(i).length))
				.toArray();

		double[] data =
				Doubles.concat(
						IntStream.range(0,left_comb)
								.mapToObj(i -> f.getVerticesAt(i)[RandomUtil.getRandom().nextInt(f.getVerticesAt(i).length)])
								.toArray(double[][]::new)
				);


		Strides newDomain = f.getDataDomain().concat(f.getSeparatingDomain());
		return BayesianFactorBuilder.as(newDomain, data);
	}



	public static VertexFactor as(Strides left, Strides right, double[][][] data){

		VertexFactorFactory factory = VertexFactorFactory.factory().domain(left, right);

		for(int j=0; j<right.getCombinations(); j++)
			for(double[] v : data[j])
				factory.addVertex(v, j);

		return factory.get();

	}

	/**
	 * Combine all the factors
	 *
	 * @param factors
	 * @return
	 */
	public static GenericFactor combineAll(GenericFactor... factors){
		if(factors.length<1)
			throw new IllegalArgumentException("wrong number of factors");
		else if(factors.length==1)
			return factors[0].copy();

		GenericFactor out = factors[0];
		for(int i=1; i<factors.length; i++){
			out = ((OperableFactor)out).combine((OperableFactor) factors[i]);
		}
		return out;

	}

	public static GenericFactor combineAll(Collection<BayesianFactor> factors){
		return combineAll(factors.toArray(BayesianFactor[]::new));
	}

	public static BayesianFactor scalarMultiply(BayesianFactor f, double k) {

		BayesianFactorFactory factory = BayesianFactorFactory.factory().domain(f.getDomain());
		double data[] = f.getData();
		for (int i = 0; i < f.getData().length; i++) {
			data[i] = data[i] * k;
		}
		return factory.data(data).get();
	}


	public static BayesianFactor renameDomain(BayesianFactor f, int... new_vars){
		return BayesianFactorFactory
				.factory()
				.domain(new Strides(new_vars, f.getDomain().getSizes()))
				.data(f.getData()).get();

	}


	public static void main(String[] args) {
		VertexFactor f = VertexFactorFactory.factory().domain(Strides.as(0,2), Strides.as(1,2)).get();
		f = addVertexAt(f,new double[]{0.5,0.5},0);
		f = addVertexAt(f,new double[]{0.5,0.4},0);
		f = addVertexAt(f,new double[]{0.1,0.9},1);
		f = addVertexAt(f,new double[]{0.2,0.4},1); // [parent][vertex][state]

		System.out.println(f);

		System.out.println(as(f.getDataDomain(), f.getSeparatingDomain(), f.getData()));

		System.out.println(sampleVertex(f));

	}

}
