package edu.ucla.belief.ace;

/**
 * A base class for the part of an OnlineEngine that depends upon the
 * mathematical space in which we are working (e.g., normal space or log_e
 * space) to prevent underflow.
 * <p>
 * This is an internal class: you cannot use it directly.
 * 
 * @author Mark Chavira
 */

abstract class Calculator {

  //============================================================================
  // Private
  //============================================================================
  
  // If evaluation results were computed, then a map from node to whether the
  // node has a single zero child; otherwise a map from node to garbage.
  protected boolean[] fNodeToForceValueZero;

  // The constructor.
  protected Calculator(int numNodes) {
    fNodeToForceValueZero = new boolean[numNodes];
  }

  // Returns whether two doubles are close to equal.
  protected static boolean close (double p1, double p2) {
    if (p1 == 0.0) {return p2 == 0.0;}
    if (p2 == 0.0) {return false;}
    if (Double.isInfinite(p1)) {return p1 == p2;}
    if (Double.isInfinite(p2)) {return false;}
    double temp = 1.0 - p1/p2;
    return (temp > 0) ? (temp <= 0.00000000001) : (temp >= -0.00000000001);
  }

  //============================================================================
  // Evaluation
  //============================================================================

  /**
   * Evaluates the circuit under the given evidence.
   * 
   * @param e the given evidence.
   */
  protected abstract void evaluate(
      int numNodes,
      byte[] nodeToType,
      int[] nodeToLit,
      int[] nodeToLastEdge,
      int[] edgeToTailNode,
      Evidence ev)
      throws Exception;

  /**
   * Returns the value of the given node.
   * 
   * @param n the given node.
   * @return the corresponding value.
   */
  protected abstract double nodeValue(int n);
  
  //============================================================================
  // Differentiation
  //============================================================================
  
  /**
   * Differentiates the circuit.
   */
  protected abstract void differentiate(
      boolean mop,
      int numNodes,
      byte[] nodeToType,
      int[] nodeToLastEdge,
      int[] edgeToTailNode)
      throws Exception;

  /**
   * Returns the partial derivative of the given node.
   * 
   * @param n the given node.
   * @return the corresponding partial.
   */
  protected abstract double nodePartial(int n);
  
  /**
   * Returns the marginal of the given node (partial times value).
   * 
   * @param n the given node.
   * @return the corresponding marginal.
   */
  protected abstract double nodeMarginal(int n);
  
  /**
   * Returns the posterier of the given node (partial times value divided by
   * value of root).
   * 
   * @param n the given node.
   * @param numNodes the number of nodes.
   * @return the corresponding posterior.
   */
  protected abstract double nodePosterior(int n, int numNodes);
  
  //============================================================================
  // Support for counting
  //============================================================================

  /**
   * Returns whether the values of the two given nodes are close, as would be
   * the case if they should be equal, but are not, because of rounding errors.
   * 
   * @param n1 the first given node.
   * @param n2 the second given node.
   */
  protected abstract boolean valuesAreClose(int n1, int n2);

  /**
   * Returns whether the values of the two given nodes are equal.
   * 
   * @param n1 the first given node.
   * @param n2 the second given node.
   */
  protected abstract boolean valuesAreEqual(int n1, int n2);

  //============================================================================
  // Support for other
  //============================================================================

  /**
   * Returns the zero value.
   * 
   * @return the zero value.
   */
  protected abstract double zero();
}
