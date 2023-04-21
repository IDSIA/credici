package ch.idsia.credici.learning.component;

import ch.idsia.credici.Table;
import ch.idsia.credici.learning.inference.ComponentInference;
import ch.idsia.credici.model.StructuralCausalModel;

/**
 * A credal builder based on the new concept of Table
 */
public class EMCCFactory {

	private EMCCConfig config = new EMCCConfig(); 

	public void data(Table data) { this.config.setData(data); }
	public void model(StructuralCausalModel model) { this.config.setModel(model); }
	public void threads(int threads) { this.config.setThreads(threads); }
	public void inference(ComponentInference inference) { this.config.setInference(inference); } 
	public void maxIterations(int iters) { this.config.setMaxIterations(iters); }
	public void runs(int runs) { this.config.setRuns(runs); }

	public EMCC build() {
		EMCC emcc = new EMCC(config);
		return emcc;
	}
}
