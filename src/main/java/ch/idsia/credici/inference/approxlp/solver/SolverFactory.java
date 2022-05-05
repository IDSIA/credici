package ch.idsia.credici.inference.approxlp.solver;


public class SolverFactory {
	public static int defaultLib = 0;
	
	public static LinearSolver getInstance(int lib) {
		if (lib == 0) 
			return new ch.idsia.credici.inference.approxlp.solver.commons.Simplex();
		else if (lib == 1) 
			return new ch.idsia.credici.inference.approxlp.solver.lpsolve.Simplex();
		else 
			return null;
	}
	
	public static LinearSolver getInstance() {
		return getInstance(defaultLib);
	}
}
