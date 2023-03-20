package edu.ucla.belief.ace;

/**
 * An object that stores weights for AC use by an
 * {@link OnlineEngine OnlineEngine}.  An evidence object is linked to a
 * specific {@link OnlineEngine OnlineEngine}.  This object performs no
 * inference.  It merely stores weights in a form that can be used
 * efficiently by the engine.  When evidence is constructed, it assigns all
 * weights to defaults, as stored in the literal map.  After construction,
 * weights may be changed.
 * <p>
 * There are two types of weights in the AC.  Each value x of each variable X
 * may have a corresponding indicator weight in the AC.  There will exist such
 * a weight iff X is not a part of evidence used *during compilation*.  When
 * such a weight exists, it is initially set to its default value, typically
 * 1.0, as defined in the literal map.  The default value typically indicates
 * that X=x is possible.  If the weight is changed to 0.0, then that is
 * equivalent to saying that X=x is not possible. Asserting traditional
 * evidence X=x on a variable is equivalent to setting weights for all the
 * variable's values to 0.0 except for the weight corresponding to x, which
 * is set to its default value.
 * <p>
 * Each parameter P in the network may have a corresponding parameter weight
 * in the AC.  There will exist such a weight iff the particular compilation
 * strategy does not commit the parameter to a specific value to make
 * compilation more efficient.  The main point is that different strategies
 * will commit different variables, and so different strategies will make
 * different sets of parameter weights available.  For example, tabular
 * variable elimination will not produce any parameter weights, but logical
 * compilation using the -cd06 encoding will produce a parameter weight for
 * each non-zero parameter that is unique within its CPT (there is no other
 * parameter having equal value).  When such a weight exists, it is initially
 * set to its default value, typically the value of the corresponding
 * parameter, as defined in the literal map.  This weight may be changed
 * however, which is equivalent to changing the value of the parameter in the
 * network.
 * <p>
 * In some cases, it is advantageous to utilize multiple evidence objects.
 * For example, if one wanted to isolate online inference in order to obtain
 * accurate timing, one could construct all evidence sets to be used in
 * queries before performing online inference.  As another example, consider
 * a situation where we execute a loop, and in each iteration, we compute
 * probability of evidence for each of N identical systems, where each system
 * has its own set of observations.  Since the systems are identical, we can
 * use the same OnlineEngine for each.  Moreover, rather than reconstruct the
 * evidence from scratch for each system in each iteration, we could maintain
 * one evidence object for each system, and in each iteration, only
 * <em>update</em> each evidence object with <em>changes</em> in the
 * corresponding system, which would be simpler and somewhat more efficient.
 * In other cases, a single evidence set may work best.  For example, if
 * performing a branch-and-bound search for an instantiation of MAP
 * variables, one may wish to commit a single assignment and retract a single
 * assignment at each search node.
 * <p>
 * A major goal when performing arithmetic circuit inference is efficiency.
 * When the arithmetic circuit is sufficiently large, the time to set up
 * evidence will be insignificant, even when care is not taken.  However,
 * when an arithmetic circuit is small compared to the number of variables, the
 * time to set up evidence may be more significant.  If the time to set up
 * evidence turns out to be significant, then there are several things that
 * could be done to improve the situation.  First, try to reduce the number of
 * invocations of
 * {@link OnlineEngine#varForName(String) OnlineEngine.varForName(String)} (
 * {@link OnlineEngine#potForName(String) OnlineEngine.potForName(String)}),
 * calling it at most once for a given variable (potential) if at all
 * possible during initialization rather than once (or multiple times) inference
 * is performed.  It may also help to reduce the number of calls to methods
 * within this class, especially {@link #Evidence(OnlineEngine)
 * Evidence(OnlineEngine)} and {@link #retractAll() retractAll()}, each of which
 * executes in time that is linear in the number of AC variables, which can be
 * as large as the Bayesian network from which the AC was produced.  Strategies
 * for improving efficiency include making use of multiple evidence objects
 * where doing so will help, updating evidence instead of creating it from
 * scratch where doing so will help, and reusing evidence objects instead of
 * creating new ones where doing so will help.
 *
 * @author Mark Chavira
 */

public class Evidence {

  //=========================================================================
  // Fields
  //========================================================================= 

  // The linked OnlineEngine.
  private OnlineEngine fEngine;

  // A map from arithmetic circuit variable to current weight of its negative
  // (positive) literal.  Map from 0 is undefined.
  protected double[] fVarToCurrentNegWeight;
  protected double[] fVarToCurrentPosWeight;
  
  // The zero value, which will be 0 or -inf depending on whether we are working
  // working in log_e space.
  private final double ZERO;

  //=========================================================================
  // Initialization
  //=========================================================================  

  /**
   * Constructs empty evidence that may be used with the given OnlineEngine
   * by assigning each weight its default as stored in the literal map.
   * Weights may subsequently be changed.  This constructor runs in time that
   * is linear in the number of arithmetic circuit variables, which can be as
   * large as the network from which the arithmetic circuit was compiled.
   * 
   * @param engine the given engine.
   */  
  public Evidence(OnlineEngine engine) {
    fEngine = engine;
    fVarToCurrentNegWeight =
      new double[fEngine.fAcVarToDefaultNegWeight.length];
    fVarToCurrentPosWeight =
      new double[fEngine.fAcVarToDefaultPosWeight.length];
    ZERO = engine.fCalculator.zero();
    retractAll();
  }
  
  /**
   * Retracts all changes to weights by resetting each weight to its default,
   * as defined in the literal map.  This method runs in time that is linear
   * in the number of arithmetic circuit variables, which can be as large as
   * the network from which the arithmetic circuit was compiled. 
   */
  public void retractAll() {
    System.arraycopy(
        fEngine.fAcVarToDefaultNegWeight,
        0,
        fVarToCurrentNegWeight,
        0,
        fEngine.fAcVarToDefaultNegWeight.length);
    System.arraycopy(
        fEngine.fAcVarToDefaultPosWeight,
        0,
        fVarToCurrentPosWeight,
        0,
        fEngine.fAcVarToDefaultPosWeight.length);
  }
  
  //=========================================================================
  // Weights on indicators
  //=========================================================================  

  /**
   * Commits the given variable to the given value.  This method corresponds
   * to setting traditional evidence on the given variable.  More specifically,
   * sets the weight of each indicator of the given variable to zero, except for
   * the indicator corresponding to the given value, which is set to its
   * default, as defined in the literal map.  This method runs in time that is
   * linear in the domain size of the given variable.
   * 
   * @param var the given variable.
   * @param val the given value.
   */
  public void varCommit(int var, int val) {
    litSetCurrentWeights(ZERO, fEngine.fSrcVarToSrcValToIndicator[var]);
    litSetCurrentWeightToDefault(fEngine.fSrcVarToSrcValToIndicator[var][val]);
  }
  
  /**
   * Retracts evidence on the given variable.  For the given variable, this
   * method will undo the effect of any invocation of varCommit () or
   * valCommit ().  More specifically, sets the weight of each indicator of
   * the given variable to its default, as stored in the literal map.  This
   * method runs in time that is linear in the domain size of the given
   * variable.
   * 
   * @param var the given variable.
   */
  public void varRetract(int var) {
    litSetCurrentWeightsToDefaults(fEngine.fSrcVarToSrcValToIndicator[var]);
  }
  
  /**
   * Redefines the weight of the indicator defined by the given variable and
   * value to the given weight.
   * 
   * @param var the given variable.
   * @param val the given value.
   * @param weight the given weight.
   */
  public void valCommit(int var, int val, double weight) {
    int l = fEngine.fSrcVarToSrcValToIndicator[var][val];
    litSetCurrentWeight(l, weight);
  }
  
  /**
   * Sets the weight of the indicator defined by the given variable and value
   * to its default weight.
   * 
   * @param var the given variable.
   * @param val the given value.
   */
  public void valRetract(int var, int val) {
    int l = fEngine.fSrcVarToSrcValToIndicator[var][val];
    litSetCurrentWeightToDefault(l);
  }

  //=========================================================================
  // Weights on parameters
  //=========================================================================  
  
  /**
   * Redefines the value of the parameter defined by the given potential and
   * given position.  More specifically, sets the weight of this parameter to
   * the given weight.  This method runs in constant time.
   * 
   * @param pot the given potential.
   * @param pos the given position.
   * @param weight the given weight.
   */
  public void parmCommit(int pot, int pos, double weight) {
    litSetCurrentWeight(fEngine.fSrcPotToSrcPosToParameter[pot][pos], weight);
  }

  /**
   * Retracts any redefinition of the value of the parameter defined by the
   * given potential and given position.  More specifically, sets the weight
   * of this parameter to its default, as defined in the literal map.  This
   * method runs in constant time.
   * 
   * @param pot the given potential.
   * @param pos the given position.
   */
  public void parmRetract(int pot, int pos) {
    litSetCurrentWeightToDefault(fEngine.fSrcPotToSrcPosToParameter[pot][pos]);
  }
  
  //=========================================================================
  // Weights on literals
  //=========================================================================    
  
  /**
   * Returns the default weight of the given literal.
   * 
   * @param lit the given literal.
   * @return the default weight.
   */
  private double litDefaultWeight(int lit) {
    return lit < 0 ?
           fEngine.fAcVarToDefaultNegWeight[-lit] :
           fEngine.fAcVarToDefaultPosWeight[lit];
  }
  
  /**
   * Returns the current weight of the given literal.
   * 
   * @param l the given literal.
   * @return the current weight.
   */
  
  //private double litCurrentWeight(int lit) {
  //  return lit < 0 ?
  //         fVarToCurrentNegWeight[-lit] :
  //         fVarToCurrentPosWeight[lit];
  //}
  
  /**
   * Sets the current weight of the given literal to its default weight.
   * 
   * @param lit the given literal.
   */
  private void litSetCurrentWeightToDefault(int lit) {
    litSetCurrentWeight(lit, litDefaultWeight(lit));
  }
  
  /**
   * Sets the current weight of the given literal to the given weight.
   * 
   * @param lit the given literal.
   * @param the given weight.
   */
  private void litSetCurrentWeight(int lit, double weight) {
    if (lit < 0) {
      fVarToCurrentNegWeight[-lit] = weight;
    } else {
      fVarToCurrentPosWeight[lit] = weight;
    }
  }
  
  /**
   * Sets the current weight of each of the given literals to its default. 
   * 
   * @param lits the given literals.
   */
  private void litSetCurrentWeightsToDefaults(int... lits) {
    for (int lit : lits) {litSetCurrentWeightToDefault(lit);}
  }
  
  /**
   * Sets the current weight of each of the given literals the given weight.
   * 
   * @param weight the given weight.
   * @param lits the given literals.
   */
  private void litSetCurrentWeights(double weight, int... lits) {
    for (int lit : lits) {litSetCurrentWeight(lit, weight);}
  }
  
  //=========================================================================
  // Miscellany
  //=========================================================================    

  public String toString() {
    return java.util.Arrays.toString(fVarToCurrentNegWeight) + "\n" +
    java.util.Arrays.toString(fVarToCurrentPosWeight);
  }
  
  public OnlineEngine engine() { return fEngine; }
}
