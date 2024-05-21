package edu.ucla.belief.ace;
import java.util.*;

/**
 * An example of using the Ace evaluator API.  The files simple.net.lmap and
 * simple.net.ac must have been compiled using the SOP kind and must be in the
 * directory from which this program is executed.
 * <code>
 * Usage java edu.ucla.belief.Test
 * </code>
 * 
 * @author Mark Chavira
 */

public class Test {

  /**
   * The main program.
   * 
   * @param args command line parameters - ignored.
   * @throws Exception if execution fails.
   */
  
  public static void main(String[] args) throws Exception {
    // Create the online inference engine.  An OnlineEngine reads from disk
    // a literal map and an arithmetic circuit compiled for a network.
    OnlineEngineSop g = new OnlineEngineSop(
        "simple.net.lmap",
        "simple.net.ac",
        true);
    
    // Obtain some objects representing variables in the network.  We are not
    // creating network variables here, just retrieving them by name from the
    // OnlineEngine.
    int a = g.varForName("A");
    int b = g.varForName("B");
    
    // Construct two sets of evidence.  The first sets network variable A to
    // its second value.  The second does the same and also sets network
    // variable B to its first value.
    Evidence e1 = new Evidence(g);
    e1.varCommit(a, 1);
    Evidence e2 = new Evidence(g);
    e2.varCommit(a, 1);
    e2.varCommit(b, 0);

    // Perform online inference in the context of the first evidence set by
    // invoking OnlineEngine.evaluate().  Doing so will compute probability of
    // evidence.  Inference runs in time that is linear in the size of the
    // arithmetic circuit.
    g.evaluate(e1);

    // Now retrieve the result of inference.  The following method invocation
    // performs no inference, simply looking up the requested value that was
    // computed by OnlineEngine.evaluate().
    double pe1 = g.evaluationResults();
    
    // Now perform online inference in the context of the second evidence set.
    // This time, also differentiate.  Answers to many additional queries then
    // become available.  Inference time will still be linear in the size of the
    // arithmetic circuit, but the constant factor will be larger.
    g.evaluate(e2);
    g.differentiate();
    
    // Once again retrieve results without performing inference.  We get
    // probability of evidence, derivatives, marginals, and posterior marginals
    // for both variables and potentials.  OnlineEngine.variables() returns an
    // unmodifiable set of all network variables and OnlineEngine.potentials()
    // returns an unmodifiable set of all network potentials.
    double pe2 = g.evaluationResults();
    double[][] varPartials = new double[g.numVariables()][];
    double[][] varMarginals = new double[g.numVariables()][];
    double[][] varPosteriors = new double[g.numVariables()][];
    double[][] potPartials = new double[g.numPotentials()][];
    double[][] potMarginals = new double[g.numPotentials()][]; 
    double[][] potPosteriors = new double[g.numPotentials()][];
    for (int v = 0; v < g.numVariables(); ++v) {
      varPartials[v] = g.varPartials(v);
      varMarginals[v] = g.varMarginals(v);
      varPosteriors[v] = g.varPosteriors(v);
    }
    for (int pot = 0; pot < g.numPotentials(); ++pot) {
      potPartials[pot] = g.potPartials(pot);
      potMarginals[pot] = g.potMarginals(pot);
      potPosteriors[pot] = g.potPosteriors(pot);
    }
    
    // Finally, display the results to standard out.
    System.out.println("Pr(e1) = " + pe1);
    System.out.println("Pr(e2) = " + pe2);
    for (int v = 0; v < g.numVariables(); ++v) {
      System.out.println(
          "(PD wrt " + g.nameForVar(v) + ")(e2) = " +
          Arrays.toString(varPartials[v])); 
    }
    for (int v = 0; v < g.numVariables(); ++v) {
      System.out.println(
          "Pr(" + g.nameForVar(v) + ", e2) = " +
          Arrays.toString(varMarginals[v]));
    }
    for (int v = 0; v < g.numVariables(); ++v) {
      System.out.println(
          "Pr(" + g.nameForVar(v) + " | e2) = " +
          Arrays.toString(varPosteriors[v]));
    }
    for (int p = 0; p < g.numPotentials(); ++p) {
      System.out.println(
          "(PD wrt " + g.nameForPot(p) + ")(e2) = " +
          Arrays.toString(potPartials[p])); 
    }
    for (int p = 0; p < g.numPotentials(); ++p) {
      System.out.println(
          "Pr(" + g.nameForPot(p) + ", e2) = " +
          Arrays.toString(potMarginals[p]));
    }
    for (int p = 0; p < g.numPotentials(); ++p) {
      System.out.println(
          "Pr(" + g.nameForPot(p) + " | e2) = " +
          Arrays.toString(potPosteriors[p]));
    }
  }
}
