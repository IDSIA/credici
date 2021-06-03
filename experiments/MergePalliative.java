import com.google.common.primitives.Doubles;
import edu.neurips.causalem.IO;
import edu.neurips.causalem.inference.CausalInference;
import edu.neurips.causalem.inference.CausalMultiVE;
import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.utility.CollectionTools;
import jdk.jshell.spi.ExecutionControl;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MergePalliative {
	public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException, InterruptedException {

		String modelFolder = "./models/palliative_care/em/";
		int effect = 0;
		int trueState = 0;
		int falseState = 1;
		int initialEMruns = 1;
		boolean plotCSV = true;

		// Read the precise models from EM
		File dir = new File(modelFolder);
		File[] files = dir.listFiles((d, name) -> name.endsWith(".uai"));

		RandomUtil.setRandomSeed(0);
		int[] idx = CollectionTools.shuffle(IntStream.range(0, files.length).toArray());

		System.out.println("Read files");
		List<StructuralCausalModel> points =
				IntStream.of(idx).mapToObj(i -> files[i])
					.map(f -> readModel(f)).collect(Collectors.toList());

		System.out.println("Number of EM models: "+points.size());


		// Get the variables names
		int X[] = points.get(0).getEndogenousVars();
		String[] labels = {"Death", "Symptoms", "PPreference", "FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"};
		int[] C = new int[]{3,7, 9};

		System.out.println(
				"numPoints," + IntStream.of(C)
						.mapToObj(c -> labels[c] + "_l" + "," + labels[c] + "_u")
						.collect(Collectors.joining(",")));


		// Compute PNS for an increasing number of EM runs
		for(int n=initialEMruns; n<=points.size(); n++) {

			CausalMultiVE inf = new CausalMultiVE(points.subList(0, n));

			for (int cause : C) {


				try {
					double[] pns = calculatePNS(inf, cause, effect, trueState, falseState);
					if (!plotCSV) {
						System.out.println("PNS(" + labels[cause] + ", " + labels[effect] + ")");
						System.out.println(Arrays.toString(pns));
					} else {
						System.out.print(n+"," + Arrays.stream(pns).mapToObj(d -> String.valueOf(d)).collect(Collectors.joining(",")));
					}
				} catch (Error e) {
					System.out.println("Error with " + labels[cause]);
					e.printStackTrace();
				} catch (Exception e) {
					System.out.println("Exception with " + labels[cause]);
					e.printStackTrace();
				}
			}
			System.out.println("");
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


	private static double[] calculatePNS(CausalInference inf, int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
		VertexFactor p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect, trueState, falseState);
		double[] v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		return new double[]{v[0], v[v.length-1]};

	}

}
