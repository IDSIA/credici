package neurips21.triangolo;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import jdk.jshell.spi.ExecutionControl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class runQuery {
	public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException, InterruptedException {


		File dir = new File("./EMmodels");
		File[] files = dir.listFiles((d, name) -> name.endsWith(".uai"));


		System.out.println("Read files");
		List<StructuralCausalModel> points = Stream.of(files).map(f -> readModel(f)).collect(Collectors.toList());

		int effect = 0;
		//int cause = points.get(0).getParents(effect)[0];

		CausalMultiVE inf = new CausalMultiVE(points);


		int X[] = points.get(0).getEndogenousVars();

		for(int cause : ArraysUtil.difference(X, new int[]{effect})) {
			System.out.println(cause);
			VertexFactor pns = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
			System.out.println(pns);
		}

	}

	public static StructuralCausalModel readModel(File f) {
		try {
			System.out.println(f.getPath());
			return (StructuralCausalModel) IO.readUAI(f.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
