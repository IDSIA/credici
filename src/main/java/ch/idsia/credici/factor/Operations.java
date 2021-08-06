package ch.idsia.credici.factor;

import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorFactory;

import java.util.List;

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

	public static void main(String[] args) {
		VertexFactor f = VertexFactorFactory.factory().domain(Strides.as(0,2), Strides.as(1,2)).get();
		f = addVertexAt(f,new double[]{0.5,0.5},0);
		f = addVertexAt(f,new double[]{0.5,0.4},0);
		f = addVertexAt(f,new double[]{0.5,0.5},1);
		f = addVertexAt(f,new double[]{0.2,0.4},1);

		System.out.println(f);
	}

}
