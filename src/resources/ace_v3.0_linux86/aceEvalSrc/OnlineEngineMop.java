package edu.ucla.belief.ace;
import java.io.*;
import java.util.*;

/**
 * An OnlineEngine capable of answering repeated max-of-product (MOP)
 * probabilistic queries efficiently.  Supported MOP queries currently
 * include:
 * <ul>
 * <li> the MOP value
 * <li> partial derivatives
 * <li> marginals
 * <li> posterior marginals
 * <li> the number of MOP instantiations
 * <li> the set of MOP instantiations (or a subset)
 * </ul>
 * For a MOP engine compiled from a Bayesian network, the evaluation result is
 * equivalent to the MPE probability.
 * <p>
 * After we have evaluated, we can also differentiate in time that is linear in
 * the size of the circuit.  Afterward, we can additionally retrieve, for each
 * value of each network variable, and for each position of each potential, a
 * partial derivative, a marginal, and a posterior marginal.
 * <p>
 * After we have evaluated, we can also enumerate in time that is linear in the
 * size of the circuit.  Afterward, we can additionally retrieve the number of
 * MOP instantiations, and we can compute the ith instantiation.  When computing
 * multiple instantiations, work performed by the previous invocation will not
 * be redone, so it is most efficient to compute instantiations in order.  For
 * example:
 * <code>
 *   mopInstantiation.evaluate();
 *   mopInstantiation.enumerate();
 *   for (int i = 0; i &lt; mopEngine.count(); ++i) {
 *     HashMap&lt;Variable, Integer\&gt; m = mopEngine.instantiation(i);
 *     }
 *   }
 * </code>
 * <p>
 * The implementation for this class is trivial.  Each method invokes a method
 * in the superclass.  The purpose of this class is to define which operations
 * are available for the particular compile kind by selectively making
 * operations in the superclass public.
 *
 * @author Mark Chavira
 */

public class OnlineEngineMop extends OnlineEngine {
  
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
   * @param acFilename the first given name.
   * @param lmFilename the second given name.
   * @param enableDifferentiation if true, then differentiation can be performed
   *   on the engine but the engine will require more memory.
   * @param enableEnumeration if true, then counting and enumeration can be
   *   performed on the engine but the engine will require more memory.
   * @throws Exception if fails.
   */
  
  public OnlineEngineMop(
      String lmFilename,
      String acFilename,
      boolean enableDifferentiation,
      boolean enableEnumeration)
      throws Exception {
    super(
        new BufferedReader(new FileReader(lmFilename)),
        new BufferedReader(new FileReader(acFilename)),
        true,
        CompileKind.ALWAYS_MAX,
        enableDifferentiation,
        enableEnumeration);
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
   * @param enableEnumeration if true, then counting and enumeration can be
   *   performed on the engine but the engine will require more memory.
   * @throws Exception if fails.
   */
  
  public OnlineEngineMop(
      BufferedReader lmReader,
      BufferedReader acReader,
      boolean enableDifferentiation,
      boolean enableEnumeration)
      throws Exception {
    super(
        lmReader,
        acReader,
        false,
        CompileKind.ALWAYS_MAX,
        enableDifferentiation,
        enableEnumeration);
  }
  
  //============================================================================
  // Differentiation:
  //============================================================================
  
  /**
   * Differentiates the circuit.  Evaluation must have been performed
   * previously.
   * 
   * @throws Exception if fails.
   */ 

  public void differentiate() throws Exception {
    super.differentiate(true);
  }
  
  /**
   * Returns whether differentiation results are available.
   * 
   * @return whether differentiation results are available.
   */
  
  public boolean differentiateResultsAvailable() {
    return super.differentiateResultsAvailable();
  }
  
  /**
   * For each value x of the given network variable, returns the partial
   * derivative of the restricted AC with respect to the x's indicator or
   * Double.NaN if the value is not a query value.  The result is in the form of
   * a double[] that maps network variable value to partial.  This method
   * performs no inference.  It merely retrieves the results of inference
   * performed previously during differentiation.  This method runs in time that
   * is linear in the domain size of the given network variable.
   *  
   * @param v the given network variable.
   * @return the partials.
   * @throws Exception if fails.
   */
  
  public double[] varPartials(int v) throws Exception {
    return differentiationResults(fSrcVarToSrcValToIndicator[v], 0);
  }
 
  /**
   * For each value x of the given network variable, returns x's marginal or
   * Double.NaN if x is not a query value.  The result is in the form of a
   * double[] that maps network variable value to marginal.  This method
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
   * For each value x of the given network variable, returns x's posterior or
   * Double.NaN if x is not a query value.  The result is in the form of a
   * double[] that maps network variable value to posterior.  This method
   * performs no inference.  It merely retrieves the results of inference
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
   * derivative of the restricted AC with respect to x's parameter or
   * Double.NaN if the position is not a query position.  The result is in the
   * form of a double[] that maps each position to partial.  This method
   * performs no inference.  It merely retrieves the results of inference
   * performed previously during differentiation.  This method runs in time that
   * is linear in the number of positions of the given network potential.
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
   * Double.NaN if the position is not a query position.  The result is in the
   * form of a double[] that maps each position to marginal.  This method
   * performs no inference. It merely retrieves the results of inference
   * performed previously during differentiation.  This method runs in time that
   * is linear in the number of positions of the given network potential.
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
  
  //============================================================================
  // Enumeration:
  //============================================================================

  /**
   * Computes the number of mop instantiations and sets up the bookkeeping
   * necessary to iterate through mop instantiations.
   * 
   * @throws Exception if fails.
   */

  public void enumerate() throws Exception { super.enumerate(); }
  
  /**
   * Returns whether enumerate results are available.
   * 
   * @return whether counting results are available.
   */
  
  public boolean enumerateResultsAvailable() {
    return super.enumerateResultsAvailable();
  }

  /**
   * Returns the MOP count computed during the most recent enumerate operation
   * or Long.MAX_VALUE if this count exceeds the range of a long.  This method
   * performs no inference.  It merely retrieves the results of the last
   * enumeration.  This method runs in constant time.
   * 
   * @return the count or Long.MAX_VALUE if the count exceeds the range of a
   *   long.
   */
   
  public long count() throws Exception { return super.count(); }

  /**
   * Returns an unmodifiable map to the index'th mop instantiation, computed
   * during the most recent enumeration.  This method performs no inference.  It
   * merely retrieves the results of inference performed previously.  The method
   * will not repeat work done by the previous call, so it is often fast, if you
   * obtain instantiations in order.  The returned map will be updated each time
   * instantiation() is invoked. 
   * 
   * @param index identifies which mop instantiation to obtain; should be in
   *     [0, countingResults()).
   * @return the instantiation.
   */
  
  public Map<Integer, Integer> instantiation(long index) throws Exception {
    return super.instantiation(index);
  }
}
