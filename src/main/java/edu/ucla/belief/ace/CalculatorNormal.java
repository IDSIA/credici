package edu.ucla.belief.ace;
import java.util.*;

/**
 * A calculator that works in normal space.
 * <p>
 * This is an internal class: you cannot use it directly.
 * 
 * @author Mark Chavira
 */

class CalculatorNormal extends Calculator {

  //============================================================================
  // Private
  //============================================================================

  // If evaluation results were computed, then a map from node to raw value,
  // which might need to be adjusted by the force to zero value; otherwise, a
  // map from node to garbage.
  private double[] fNodeToValue;
  
  // If differentiation is not enabled, then null; otherwise if differentiation
  // results were computed, then a map from node to derivative; otherwise, a
  // map from node to garbage.
  private double[] fNodeToDerivative;

  // The zero and one values.
  private static final double ZERO = 0.0;
  private static final double ONE = 1.0;
  
  /**
   * The constructor.
   * 
   * @param numNodes the number of nodes.
   * @param enableDifferentiation whether to allocate data structures for
   *   differentiation.
   */
  protected CalculatorNormal(int numNodes, boolean enableDifferentiation) {
    super(numNodes);
    fNodeToValue = new double[numNodes];
    if (enableDifferentiation) {fNodeToDerivative = new double[numNodes];}
  }

  //============================================================================
  // Evaluation
  //============================================================================

  @Override
  protected void evaluate(
      int numNodes,
      byte[] nodeToType,
      int[] nodeToLit,
      int[] nodeToLastEdge,
      int[] edgeToTailNode,
      Evidence ev)
      throws Exception {
    double[] negValues = ev.fVarToCurrentNegWeight;
    double[] posValues = ev.fVarToCurrentPosWeight;
    for (int n = 0; n < numNodes; n++) {
      switch (nodeToType[n]) {
        case OnlineEngine.MULT:
          int mLast = nodeToLastEdge[n];
          double mt = ONE;
          int numZeros = 0;
          for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < mLast; e++) {
            int ch = edgeToTailNode[e];
            double chVal = nodeValue(ch);
            if (chVal == ZERO) {
              if (++numZeros > 1) {mt = ZERO; break;}
            } else {
              mt *= chVal;
              if (mt == ZERO) {throw new UnderflowException();}
            }
          }
          fNodeToForceValueZero[n] = numZeros == 1;
          fNodeToValue[n] = mt;
          break;
        case OnlineEngine.ADD:
          int aLast = nodeToLastEdge[n];
          double at = ZERO;
          for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < aLast; e++) {
            int ch = edgeToTailNode[e];
            double chVal = nodeValue(ch);
            at += chVal;
          }
          fNodeToValue[n] = at;
          break;
        case OnlineEngine.MAX:
          int xLast = nodeToLastEdge[n];
          double xt = ZERO;
          for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < xLast; e++) {
            int ch = edgeToTailNode[e];
            double chVal = nodeValue(ch);
            if (chVal > xt) {xt = chVal;}
          }
          fNodeToValue[n] = xt;
          break;
        case OnlineEngine.LITERAL:
          int l = nodeToLit[n];
          fNodeToValue[n] = l < 0 ? negValues[-l] : posValues[l];
          break;
        case OnlineEngine.CONSTANT:
          break;
        default:
          throw new Exception("Unexpected node type!");
      }
    }
  }
  
  @Override
  protected double nodeValue(int n) {
    return fNodeToForceValueZero[n] ? ZERO : fNodeToValue[n];
  }
  
  //============================================================================
  // Differentiation
  //============================================================================

  @Override
  protected void differentiate(
      boolean mop,
      int numNodes,
      byte[] nodeToType,
      int[] nodeToLastEdge,
      int[] edgeToTailNode)
      throws Exception {
    Arrays.fill(fNodeToDerivative, ZERO);
    fNodeToDerivative[numNodes - 1] = ONE;
    for (int n = numNodes - 1; n >= 0; n--) {
      switch (nodeToType[n]) {
        case OnlineEngine.MULT:
          int mLast = nodeToLastEdge[n];
          double mt = fNodeToDerivative[n];
          if (mt == ZERO) {continue;}
          double mv = fNodeToValue[n];
          if (mv == ZERO) {continue;}
          mt *= mv;
          if (mt == ZERO) {throw new UnderflowException();}
          if (fNodeToForceValueZero[n]) { // exactly one zero
            for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < mLast; e++) {
              int ch = edgeToTailNode[e];
              double chVal = nodeValue(ch);
              if (chVal == ZERO) {
                if (mop) {
                  if (mt > fNodeToDerivative[ch]) {fNodeToDerivative[ch] = mt;}
                } else {
                  fNodeToDerivative[ch] += mt;
                }
                break;
              }
            }
          } else { // no zeros
            for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < mLast; e++) {
              int ch = edgeToTailNode[e];
              double chVal = nodeValue(ch);
              double contribution = mt / chVal;
              if (mop) {
                if (contribution > fNodeToDerivative[ch]) {
                  fNodeToDerivative[ch] = contribution;
                }
              } else {
                fNodeToDerivative[ch] += contribution;
              }
            }
          }
          break;
        case OnlineEngine.ADD:
          if (mop) {throw new Exception("Did not expect an addition node!");}
          int aLast = nodeToLastEdge[n];
          double at = fNodeToDerivative[n];
          for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < aLast; e++) {
            int ch = edgeToTailNode[e];
            fNodeToDerivative[ch] += at;
          }
          break;
        case OnlineEngine.MAX:
          if (!mop) {throw new Exception("Did not expect a max node!");}
          int xLast = nodeToLastEdge[n];
          double xt = fNodeToDerivative[n];
          for (int e = n == 0 ? 0 : nodeToLastEdge[n - 1]; e < xLast; e++) {
            int ch = edgeToTailNode[e];
            if (xt > fNodeToDerivative[ch]) {fNodeToDerivative[ch] = xt;}
          }
          break;
        case OnlineEngine.LITERAL:
        case OnlineEngine.CONSTANT:
          continue;
      }
    }
  }
  
  @Override
  protected double nodePartial(int n) { return fNodeToDerivative[n]; }
  
  @Override
  protected double nodeMarginal(int n) {
    return fNodeToDerivative[n] * nodeValue(n);
  }
  
  @Override
  protected double nodePosterior(int n, int numNodes) {
    double overallValue = nodeValue(numNodes - 1);
    return (overallValue == ZERO) ?
           ZERO :
           nodeValue(n) * fNodeToDerivative[n] / overallValue;
  }
  
  //============================================================================
  // Support for counting
  //============================================================================

  @Override
  protected boolean valuesAreClose(int n1, int n2) {
    return close(nodeValue(n1), nodeValue(n2));
  }
  
  @Override
  protected boolean valuesAreEqual(int n1, int n2) {
    return nodeValue(n1) == nodeValue(n2);
  }

  //============================================================================
  // Support for other
  //============================================================================

  @Override
  protected double zero() { return ZERO; }
}
