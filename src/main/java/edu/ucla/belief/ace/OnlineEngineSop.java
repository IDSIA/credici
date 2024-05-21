package edu.ucla.belief.ace;
import java.io.*;

/**
 * An OnlineEngine capable of answering repeated sum-of-product (SOP)
 * probabilistic queries efficiently.  Supported SOP queries currently
 * include:
 * <ul>
 * <li> the SOP value
 * <li> partial derivatives
 * <li> marginals
 * <li> posterior marginals
 * </ul>
 * For a SOP engine compiled from a Bayesian network, the evaluation result is
 * equivalent to probability of evidence.
 * <p>
 * After we have evaluated, we can also compute answers to many additional
 * queries by performing a second top-down pass, called differentiation, in time
 * that is linear in the size of the circuit.  These queries include, for each
 * value of each network variable, and for each position of each potential, a
 * partial derivative, a marginal, and a posterior marginal.
 * <p>
 * The implementation for this class is trivial.  Each method invokes a method
 * in the superclass.  The purpose of this class is to define which operations
 * are available for the particular compile kind by selectively making
 * operations in the superclass public.
 *
 * @author Mark Chavira
 */

public class OnlineEngineSop extends OnlineEngine {
  
  //=========================================================================
  // Construction
  //=========================================================================
  
  /**
   * Reads a literal map from the file having the first given name, reads an AC
   * from the file having the second given name, and constructs an online engine
   * linked to the read literal map and read AC.  Runs in time that is linear in
   * the size of the circuit.  However, the constant factor is large, since data
   * is read from disk.
   * 
   * @param lmFilename the first given name.
   * @param acFilename the second given name.
   * @param enableDifferentiation if true, then differentiation can be performed
   *   on the engine but the engine will require more memory.
   * @throws Exception if fails.
   */
  
  public OnlineEngineSop(
      String lmFilename,
      String acFilename,
      boolean enableDifferentiation)
      throws Exception {
    super(
        new BufferedReader(new FileReader(lmFilename)),
        new BufferedReader(new FileReader(acFilename)),
        true,
        CompileKind.ALWAYS_SUM,
        enableDifferentiation,
        false);
  }
  
  /**
   * Reads a literal map from the first given reader, reads an AC from the
   * second given reader, and constructs an online engine linked to the read
   * literal map and read AC.  Runs in time that is linear in the size of the
   * circuit.  However, the constant factor is large, since data is read from
   * disk.  Does not close the given readers.
   * 
   * @param lmReader the first given name.
   * @param acReader the second given name.
   * @param enableDifferentiation if true, then differentiation can be performed
   *   on the engine but the engine will require more memory.
   * @throws Exception if fails.
   */
  
  public OnlineEngineSop(
      BufferedReader lmReader,
      BufferedReader acReader,
      boolean enableDifferentiation)
      throws Exception {
    super(
        lmReader,
        acReader,
        false,
        CompileKind.ALWAYS_SUM,
        enableDifferentiation,
        false);
  }
  
  //============================================================================
  // Performing Inference:
  //============================================================================
  
  /**
   * Differentiates the circuit.  Evaluation must have been performed
   * previously.
   * 
   * @throws Exception if fails.
   */ 

  public void differentiate() throws Exception {
    super.differentiate(false);
  }
  
  /**
   * Returns whether differentiation results are available.
   * 
   * @return whether differentiation results are available.
   */
  
  public boolean differentiationResultsAvailable() {
    return super.differentiateResultsAvailable();
  }
  
  /**
   * For each value x of the given network variable, returns the partial
   * derivative of the AC with respect to the x's indicator or Double.NaN if
   * the value is not a query value.  The result is in the form of a double[]
   * that maps network variable value to partial.  This method performs no
   * inference.  It merely retrieves the results of inference performed
   * previously during differentiation.  This method runs in time that is linear
   * in the domain size of the given network variable.
   *  
   * @param v the given network variable.
   * @return the partials.
   * @throws Exception if fails.
   */
  
  public double[] varPartials(int v) throws Exception {
    return differentiationResults(fSrcVarToSrcValToIndicator[v], 0);
  }
 
  /**
   * For each value x of the given network variable, returns x's marginal P(x,e)
   * or Double.NaN if the value is not a query value.  The result is in the form
   * of a double[] that maps network variable value to marginal.  This method
   * performs no inference.  It merely retrieves the results of inference
   * performed previously during differentiation.  This method runs in time that
   * is linear in the domain size of the given network variable.
   *  
   * @param v the given network variable.
   * @return the marginals.
   * @throws Exception if fails.
   */
  
  public double[] varMarginals(int v) throws Exception {
    return differentiationResults(fSrcVarToSrcValToIndicator[v], 1);
  }

  /**
   * For each value x of the given network variable, returns x's posterior
   * P(x|e) or Double.NaN if the value is not a query value.  The result is in
   * the form of a double[] that maps network variable value to posterior.  This
   * method performs no inference.  It merely retrieves the results of inference
   * performed previously during differentiation.  This method runs in time that
   * is linear in the domain size of the given network variable.
   *  
   * @param v the given network variable.
   * @return the posteriors.
   * @throws Exception if fails.
   */
  
  public double[] varPosteriors(int v) throws Exception {
    return differentiationResults(fSrcVarToSrcValToIndicator[v], 2);
  }
  
  /**
   * For each position p of the given network potential, returns the partial
   * derivative of the AC with respect to the p's parameter or Double.NaN if the
   * position is not a query position.  The result is in the form of a double[]
   * that maps each position to partial.  This method performs no inference.  It
   * merely retrieves the results of inference performed previously during
   * differentiation.  This method runs in time that is linear in the number of
   * positions of the given network potential.
   * 
   * @param pot the given network potential.
   * @return the partials.
   * @throws Exception if fails.
   */
  
  public double[] potPartials(int pot) throws Exception {
    return differentiationResults(fSrcPotToSrcPosToParameter[pot], 0);
  }

  /**
   * For each position p of the given network potential, returns p's marginal or
   * Double.NaN if p is not a query position.  The result is in the form of a
   * double[] that maps each position to marginal.  This method performs no
   * inference. It merely retrieves the results of inference performed
   * previously during differentiation.  This method runs in time that is linear
   * in the number of positions of the given network potential.
   * 
   * @param pot the given network potential.
   * @return the marginals.
   * @throws Exception if fails.
   */
  
  public double[] potMarginals(int pot) throws Exception {
    return differentiationResults(fSrcPotToSrcPosToParameter[pot], 1);
  }
  
  /**
   * For each position p of the given network potential, returns p's posterior
   * or Double.NaN if p is not a query position.  The result is in the form of a
   * double[] that maps each position to posterior.  This method performs no
   * inference. It merely retrieves the results of inference performed
   * previously during differentiation.  This method runs in time that is linear
   * in the number of positions of the given network potential.
   * 
   * @param pot the given network potential.
   * @return the marginals.
   * @throws Exception if fails.
   */
  
  public double[] potPosteriors(int pot) throws Exception {
    return differentiationResults(fSrcPotToSrcPosToParameter[pot], 2);
  }
}
