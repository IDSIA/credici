package edu.ucla.belief.ace;
import java.util.*;
import java.io.*;

/**
 * An abstract base class for an inference engine based on an arithmetic circuit
 * compiled for a network (set of potentials) and capable of answering repeated
 * probabilistic queries efficiently.
 * <p>
 * If there are M variables, then we identify them with the ints in [0, M).
 * Each variable has a name and a finite number of values.  If a variable has N
 * values, then those values are 0, 1, ..., N-1.  The variable name, number of
 * values, and order of values will be the same as in the network from which the
 * variable originated.  
 * <p>
 * If there are M potentials, then we identify them with the ints in [0, M).
 * Each potential has a name and a finite number of positions.  If a potential
 * has N positions, then those positions are 0, 1, ..., N-1.  In the network  
 * from which the potential originated, the potential was associated with a list
 * of network variables.  If this network was a Bayesian network, then the
 * potential's name is the same as the name of the last associated variable.
 * The positions correspond to instantiations of the associated variables in a
 * standard way.  For example, if the variables are A, B, C, in that order, and
 * each of these variables has two values, then the positions correspond to
 * instantiations as follows:
 * <code>
 *   Pos  A  B  C
 *   ---  -  -  -
 *     0  0  0  0
 *     1  0  0  1
 *     2  0  1  0
 *     3  0  1  1
 *     4  1  0  0
 *     5  1  0  1
 *     6  1  1  0
 *     7  1  1  1
 * </code> * 
 * <p>
 * For given evidence e, we can compute the "value" of the AC by performing a
 * bottom-up traversal of the circuit, called evaluation, in time that is linear
 * in the size of the circuit.
 * <p>
 * When there is a need to evaluate multiple times, it is possible to improve
 * efficiency by refraining from evaluating parts of the circuit known to have
 * not changed since the previous evaluation.  This behavior is not implemented
 * currently in this class.
 * <p>
 * Some of the leaves in the AC represent constants.  It is often possible to
 * simplify such a circuit in many cases to make it smaller.  This behavior
 * is not implemented currently in this class.
 * <p>
 * This class implements all of the functions needed by all subclasses.  The
 * purpose of each subclass is to selectively make functions in this class
 * public according to the operations that should be available for the
 * corresponding compile kind.
 * <p>
 * This is an internal class: you cannot use it directly.
 *
 * @author Mark Chavira
 */

abstract class OnlineEngine {

  //============================================================================
  // Fields that deal with the source domain and mapping between the source
  // domain and the ac domain.  Each field in this section is set when the
  // literal map is read and does not change thereafter.
  //============================================================================

  // The compilation kind.  This field defines which of MAX and SUM nodes are
  // allowed to appear in the circuit and which types of inference are allowed.
  // The subclass instantiated will match this kind.
  protected enum CompileKind {
    ALWAYS_SUM,
    ALWAYS_MAX,
    SOMETIMES_SUM_SOMETIMES_MAX
  };
  protected CompileKind fCompileKind;
  
  // The mathematical space for the weights.
  protected enum Space {NORMAL, LOG_E};
  protected Space fSpace;

  // A map from name to source variable (potential).
  protected HashMap<String,Integer> fNameToSrcVar;
  protected HashMap<String,Integer> fNameToSrcPot;
  protected String[] fSrcVarToName;
  protected String[] fSrcPotToName;

  // A map from source variable (potential) to value (position) to indicator
  // (parameter) literal.  Some values (positions) will not have a corresponding
  // indicator (parameter) literal.  We map to 0 for these.
  protected int[][] fSrcVarToSrcValToIndicator;
  protected int[][] fSrcPotToSrcPosToParameter;

  // Mappings from ac variable to default weight, source variable, and source
  // value.  The last four maps are for model enumeration only.
  protected double[] fAcVarToDefaultNegWeight;
  protected double[] fAcVarToDefaultPosWeight;
  protected int[] fAcVarToNegSrcVar;
  protected int[] fAcVarToPosSrcVar;
  protected int[] fAcVarToNegSrcVal;
  protected int[] fAcVarToPosSrcVal;
  
  //============================================================================
  // Fields that define the circuit.  The cleanest way to do this would be to
  // have an object for each node, with a field for each attribute, including a
  // pointer to each child node.  However, to save space, we use parallel
  // arrays.  Nodes are 0..(numNodes-1).  Nodes are in a reverse topological
  // order.  Edges are 0..(numEdges-1).  Edges outgoing a particular node are
  // consecutive, with edges for a node A preceding those for a node B if A < B.
  // AC variables are 1..numVars.  Each field in this section (if it is not
  // constant) is set when the engine is constructed and does not change
  // thereafter.
  //============================================================================

  // Constants that identify a node's type.  We are storing one of these for
  // each node, so to save space, we use a byte to identify a type rather than
  // an enumerated value, which would occupy four times the space.
  protected static final byte CONSTANT = 0;
  protected static final byte LITERAL = 1;
  protected static final byte MULT = 2;
  protected static final byte ADD = 3;
  protected static final byte MAX = 4;

  // A map from node to its type.
  protected byte[] fNodeToType;
  
  // A map from node n to the index of its last edge plus one.  If n == 0, then
  // fNodeToLastEdge[n] == 0; if  n is a sink and n != 0, then
  // fNodeToLastEdge[n] == fNodeToLastEdge[n-1]; if n is not a sink, then
  // fNodeToLastEdge[n] is the last edge outgoing n plus one.
  protected int[] fNodeToLastEdge;

  // A map from node to its literal.  A mapping for a node that is not a literal
  // is undefined.
  protected int[] fNodeToLit;
  
  // A map from edge to the node the edge enters.
  protected int[] fEdgeToTailNode;
  
  // A map from circuit variable to the one node that contains the variable's
  // negative (positive) literal or to -1 if no node contains it.  Mapping from
  // 0 is undefined.
  protected int[] fAcVarToNegLitNode;
  protected int[] fAcVarToPosLitNode;

  //=========================================================================
  // Fields that allow us to use c2d
  //=========================================================================

  // This type is a hack that helps us to use c2d as a compilation engine.  The
  // problem is that we would like each internal node labeled as MULT, ADD, or
  // MAX, but c2d will label as AND or OR.  AND nodes are easy, because they
  // always map directly to MULT.  However, we do not know, based on the output
  // of c2d, whether a given OR node should be mapped to ADD or to MAX.  To
  // solve this problem, we make use of the fact that c2d labels each OR node
  // with an AC variable, and we store a map from AC variable to elimination
  // operation in the literal map.  Note that some AC variables are guaranteed
  // not to label an OR node, and so these variables may map to INVALID rather
  // than to ADD or MAX.
  protected enum ElimOp {ADD, MAX, INVALID};

  //============================================================================
  // Fields used for performing inference.  The fields in this section may
  // change each time inference is performed.
  //============================================================================

  // Whether inference results are available.
  protected boolean fEvaluateResultsAvailable;
  protected boolean fDifferentiateResultsAvailable;
  protected boolean fEnumerateResultsAvailable;
  
  // The calculator, which knows how to perform computations in a particular
  // mathematical space.
  protected Calculator fCalculator;
  
  // If counting results available, then a map from each node n to n's count.
  protected long[] fNodeToCount;

  // Information about the last enumerate instantiation returned.
  protected long fEnumerateLastIndex;
  protected HashMap<Integer, Integer> fEnumerateInstantiation;
  protected Map<Integer, Integer> fEnumerateUnmodifiableInstantiation;

  //=========================================================================
  // Initializing the engine.
  //=========================================================================

  // The constructor.
  protected OnlineEngine(
      BufferedReader lmReader,
      BufferedReader acReader,
      boolean closeReaders,
      CompileKind requiredCompileKind,
      boolean enableDifferentiation,
      boolean enableEnumeration)
      throws Exception {
    // Read the literal map.
    ElimOp[] logicVarToElimOp = readLiteralMap(lmReader, requiredCompileKind);
    if (closeReaders) {lmReader.close();}
    
    // Read the AC.
    readCircuit(acReader, logicVarToElimOp);
    if (closeReaders) {acReader.close();}

    // No inference results are yet available.
    fEvaluateResultsAvailable = false;
    fDifferentiateResultsAvailable = false;
    fEnumerateResultsAvailable = false;

    // Allocate data structures for inference.
    if (enableEnumeration) {
      fNodeToCount = new long[numAcNodes()];
      fEnumerateLastIndex = -1;
      fEnumerateInstantiation = new HashMap<Integer, Integer>();
      fEnumerateUnmodifiableInstantiation =
        Collections.unmodifiableMap(fEnumerateInstantiation);
    }
    switch (fSpace) {
      case NORMAL:
        fCalculator = new CalculatorNormal(numAcNodes(), enableDifferentiation);
        break;
      case LOG_E:
        fCalculator = new CalculatorLogE(numAcNodes(), enableDifferentiation);
        break;
    }
  }
  
  // Reads the literal map from the given reader.  Returns a map from logic
  // variable to elimination operation.
  protected ElimOp[] readLiteralMap(
      Reader r,
      CompileKind requiredCompileKind)
      throws Exception {
    // The field delimiter.
    final String DELIMITER_PATTERN = "\\$";
    final String DELIMITER = "$";

    // Prepare to parse.
    BufferedReader br =
      r instanceof BufferedReader ? (BufferedReader)r : new BufferedReader(r);
    java.util.regex.Pattern p =
      java.util.regex.Pattern.compile(DELIMITER_PATTERN);
    int numLits = Integer.MAX_VALUE;
    int litsFinished = 0;
    ElimOp[] logicVarToElimOp = null;
    fNameToSrcVar = new HashMap<String,Integer>();
    fNameToSrcPot = new HashMap<String,Integer>();
    int nextVar = 0;
    int nextPot = 0;

    // Process each line.
    try{
      while (litsFinished < numLits) {
        // Read the line.  Quit if eof.  Skip if comment (including blank
        // lines).  Otherwise, split into tokens.
        String line = br.readLine();
        if (line == null) {break;} // eof
        if (!line.startsWith("cc" + DELIMITER)) {continue;} // comment
        line = line.trim();
        String[] tokens = p.split(line);
        
        // If the line indicates the compile kind, then it is of the form:
        // "cc" "K" compile kind.
        String type = tokens[1];
        if (type.equals ("K")) {
          fCompileKind = Enum.valueOf(CompileKind.class, tokens[2]);
          if (fCompileKind != requiredCompileKind) {
            throw new Exception(
                "You attempted to read an AC of kind " + requiredCompileKind +
                " but the AC was actually of kind " + fCompileKind);
          }
          continue;
        }
  
        // If the line indicates the mathematical space, then it is of the form:
        // "cc" "S" space
        if (type.equals ("S")) {
          fSpace = Enum.valueOf(Space.class, tokens[2]);
          continue;
        }
  
        // If the line is a literal count, it is of the form:
        // "cc" "N" numLogicVars.
        if (type.equals("N")) {
          int numLogicVars = Integer.parseInt(tokens[2]);
          numLits = numLogicVars * 2;
          fAcVarToDefaultNegWeight = new double[numLogicVars + 1];
          fAcVarToDefaultPosWeight = new double[numLogicVars + 1];
          fAcVarToNegSrcVar = new int[numLogicVars + 1];
          Arrays.fill(fAcVarToNegSrcVar, -1);
          fAcVarToPosSrcVar = new int[numLogicVars + 1];
          Arrays.fill(fAcVarToPosSrcVar, -1);
          fAcVarToNegSrcVal = new int[numLogicVars + 1];
          Arrays.fill(fAcVarToNegSrcVal, -1);
          fAcVarToPosSrcVal = new int[numLogicVars + 1];
          Arrays.fill(fAcVarToPosSrcVal, -1);
          logicVarToElimOp = new ElimOp[numLogicVars + 1];
          continue;
        }
        
        // If the line is a variable count line, then it is of the form:
        // "cc" "v" numVars
        if (type.equals("v")) {
          int varCount = Integer.parseInt(tokens[2]);
          fSrcVarToSrcValToIndicator = new int[varCount][];
          fSrcVarToName = new String[varCount];
          continue;
        }
        
        // If the line is a variable line, then it is of the form:
        // "cc" "V" srcVarName numSrcVals (srcVal)+
        if (type.equals("V")) {
          String vn = tokens[2];
          int valCount = Integer.parseInt(tokens[3]);
          int v = nextVar++;
          fSrcVarToSrcValToIndicator[v] = new int[valCount];
          fNameToSrcVar.put(vn, v);
          fSrcVarToName[v] = vn;
          continue;
        }
        
        if (type.equals("t")) {
          int tabCount = Integer.parseInt(tokens[2]);
          fSrcPotToSrcPosToParameter = new int[tabCount][];
          fSrcPotToName = new String[tabCount];
          continue;
        }
  
        // If the line is a potential line, then it is of the form:
        // "cc" "T" srcPotName parameterCnt.
        if (type.equals("T")) {
          String tn = tokens[2];
          int parmCount = Integer.parseInt(tokens[3]);
          int pot = nextPot++;
          fSrcPotToSrcPosToParameter[pot] = new int[parmCount];
          fNameToSrcPot.put(tn, pot);
          fSrcVarToName[pot] = tn;
          continue;
        }
  
        // The line must be a literal description, which looks like one of the
        // following:
        //   "cc" "I" literal weight elimOp srcVarName srcValName srcVal
        //   "cc" "P" literal weight elimOp srcPotName pos+
        //   "cc" "C" literal weight elimOp
        int l = Integer.parseInt(tokens[2]);
        double weight = Double.parseDouble(tokens[3]);
        if (l > 0) {
          switch (tokens[4].charAt(0)) {
            case '+': logicVarToElimOp[l] = ElimOp.ADD; break;
            case 'X': logicVarToElimOp[l] = ElimOp.MAX; break;
            case 'I': logicVarToElimOp[l] = ElimOp.INVALID; break;
            default: throw new Exception(
                "Invalid elimination operation: " + tokens[4].charAt(0));
          }
        }
        (l < 0 ? fAcVarToDefaultNegWeight : fAcVarToDefaultPosWeight)[Math.abs(l)] = weight;
        if (type.equals("I")) {
          String vn = tokens[5];
          int v = fNameToSrcVar.get(vn);
          int u = Integer.parseInt(tokens[6]);
          fSrcVarToSrcValToIndicator[v][u] = l;
          (l < 0 ? fAcVarToNegSrcVar : fAcVarToPosSrcVar)[Math.abs(l)] = v;
          (l < 0 ? fAcVarToNegSrcVal : fAcVarToPosSrcVal)[Math.abs(l)] = u;
        } else if (type.equals("P")) {
          String tn = tokens[5];
          int pot = fNameToSrcPot.get(tn);
          int pos = Integer.parseInt(tokens[6]);
          fSrcPotToSrcPosToParameter[pot][pos] = l;
        } else if (type.equals("C")) {
        } else {
          throw new Exception(
              "\"cc\" must be followed by K, S, N, V, T, I, P, or C");
        }
        ++litsFinished;
      }
    } finally {
      br.close();
    }
    
    // Now create the variables, the map from variable name to variable, and
    // the map from variable to value to indicator.
    return logicVarToElimOp;
  }
            
  // Reads the circuit from the given reader.  The given map should should map
  // each logic variable to its elimination operation.
  protected void readCircuit(
      Reader r,
      ElimOp[] logicVarToElimOp)
      throws Exception {
    // Prepare to parse.
    BufferedReader br =
      r instanceof BufferedReader ? (BufferedReader)r : new BufferedReader(r);
    java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\s+");
    int nextEdge = 0;
    int nextNode = 0;
    
    // Process each line.
    try {
      while (fNodeToType == null || nextNode < fNodeToType.length) {
        // Read the line.  Quit if eof.  Skip if comment or blank line.
        // Otherwise, split into tokens.
        String line = br.readLine();
        if (line == null) {break;} // eof
        if (line.startsWith("c")) {continue;} // comment
        line = line.trim();
        if (line.length() == 0) {continue;} // blank line
        String[] tokens = p.split(line);
        
        // A header line looks like: "nnf" numNodes numEdges numVars
        if (tokens[0].equals("nnf")) {
          int numNodes = Integer.parseInt(tokens[1]);
          int numEdges = Integer.parseInt(tokens[2]);
          int numVars = Integer.parseInt(tokens[3]);
          fNodeToType = new byte[numNodes];
          fNodeToLit = new int[numNodes];
          fNodeToLastEdge = new int[numNodes];
          fEdgeToTailNode = new int[numEdges];
          fAcVarToNegLitNode = new int[numVars + 1];
          fAcVarToPosLitNode = new int[numVars + 1];
          Arrays.fill(fAcVarToNegLitNode, -1);
          Arrays.fill(fAcVarToPosLitNode, -1);
          continue;
        }
        
        // This is not a header line, so it must be a node line, which looks
        // like one of the following:
        //   "A" numChildren child+
        //   "*" numChildren child+
        //   "O" splitVar numChildren child+
        //   "+" numChildren child+
        //   "X" numChildren child+
        //   "L" literal
        char ch = tokens[0].charAt(0);
        int logicVar = ch == 'O' ? Integer.parseInt(tokens[1]) : -1;
        if (ch == 'A' || ch == '*') {
          fNodeToType[nextNode] = MULT;
          for (int chIndex = 2; chIndex < tokens.length; chIndex++) {
            fEdgeToTailNode[nextEdge++] = Integer.parseInt(tokens[chIndex]);
          }
        } else if (ch == 'O' && logicVarToElimOp[logicVar] == ElimOp.ADD ||
                   ch == '+') {
          switch (fCompileKind) {
            case ALWAYS_SUM:
            case SOMETIMES_SUM_SOMETIMES_MAX:
              break;
            case ALWAYS_MAX:
              throw new Exception(
                  "You are attempting to read a SOP AC, but the AC contains " +
                  "at least one MAX node!");
          }
          fNodeToType[nextNode] = ADD;
          int start = ch == 'O' ? 3 : 2;
          for (int chIndex = start; chIndex < tokens.length; chIndex++) {
            fEdgeToTailNode[nextEdge++] = Integer.parseInt(tokens[chIndex]);
          }
        } else if (ch == 'O' && logicVarToElimOp[logicVar] == ElimOp.MAX ||
                   ch == 'x') {
          switch (fCompileKind) {
            case ALWAYS_MAX:
            case SOMETIMES_SUM_SOMETIMES_MAX:
              break;
            case ALWAYS_SUM:
              throw new Exception(
                  "You are attempting to read a MOP AC, but the AC contains " +
                  "at least one ADD node!");
          }
          fNodeToType[nextNode] = MAX;
          int start = ch == 'O' ? 3 : 2;
          for (int chIndex = start; chIndex < tokens.length; chIndex++) {
            fEdgeToTailNode[nextEdge++] = Integer.parseInt (tokens[chIndex]);
          }
        } else if (ch == 'L' || ch == 'l') {
          fNodeToType[nextNode] = LITERAL;
          int l = Integer.parseInt(tokens[1]);
          fNodeToLit[nextNode] = l;
          (l < 0 ? fAcVarToNegLitNode : fAcVarToPosLitNode)[Math.abs(l)] =
            nextNode;
        } else {
          throw new Exception("Unexepected node type " + ch + "!");
        }
        fNodeToLastEdge[nextNode] = nextEdge;
        nextNode++;
      } 
    } finally {
      br.close();
    }
  }

  //============================================================================
  // Miscellaneous convenience methods
  //============================================================================

  protected int root() {return fNodeToType.length - 1;}
  protected int numAcNodes() {return fNodeToType.length;}
  protected int first(int n) {return (n == 0) ? 0 : fNodeToLastEdge[n-1];}

  //============================================================================
  // Retrieving variables and potentials
  //============================================================================

  /**
   * Returns the number of variables.
   * 
   * @return the variables.
   */
  
  public int numVariables() { return fSrcVarToSrcValToIndicator.length; }
  
  /**
   * Returns the number of potentials.
   * 
   * @return the variables.
   */
  
  public int numPotentials() { return fSrcPotToSrcPosToParameter.length; }

  /**
   * Returns the variable having the given name.  This method is fairly
   * efficient, merely performing a lookup in a HashMap using a string key.
   * However, there is some overhead, so if at all possible, all calls to
   * this method should be made during initialization, and the method should
   * be called at most once per variable rather than once (or multiple times)
   * per evidence set.  This method runs in time that is linear in the size
   * of the given name.
   * 
   * @param n the given name.
   * @return the corresponding variable.
   */
  
  public int varForName(String n) { return fNameToSrcVar.get(n); }
  
  /**
   * Returns the potential having the given name.  This method is fairly
   * efficient, merely performing a lookup in a HashMap using a string key.
   * However, there is some overhead, so if at all possible, all calls to
   * this method should be made during initialization, and the method should
   * be called at most once per potential rather than once (or multiple
   * times) per evidence set.  This method runs in time that is linear in the
   * size of the given name.
   * 
   * @param n the given name.
   * @return the corresponding potential.
   */
  
  public int potForName(String n) { return fNameToSrcPot.get(n); }

  /**
   * Returns the name of the given variable.
   * 
   * @param v the given variable.
   * @return v's name.
   */
  
  public String nameForVar(int v) { return fSrcVarToName[v]; }

  /**
   * Returns the name of the given potential.
   * 
   * @param p the given potential.
   * @return p's name.
   */
  
  public String nameForPot(int p) { return fSrcPotToName[p]; }

  //============================================================================
  // Evaluation
  //============================================================================
  
  /**
   * Evaluates the circuit under the given evidence.
   * 
   * @param e the given evidence.
   * @throws Exception if the evaluation fails.
   */
  
  public void evaluate(Evidence e) throws Exception {
    fCalculator.evaluate(
        numAcNodes(),
        fNodeToType,
        fNodeToLit,
        fNodeToLastEdge,
        fEdgeToTailNode,
        e);
    fEvaluateResultsAvailable = true;
    fDifferentiateResultsAvailable = false;
    fEnumerateResultsAvailable = false;
  }
  
  /**
   * Returns whether evaluation results are available.
   * 
   * @return whether evaluation results are available.
   */
  
  public boolean evaluateResultsAvailable() {
    return fEvaluateResultsAvailable;
  }
  
  /**
   * Returns value computed during the most recent evaluation.  The method
   * performs no inference.  It merely retrieves the results of inference
   * performed previously during evaluation.  This method runs in constant time.
   * 
   * @return the probability of evidence.
   * @throws Exception if the evaluation fails.
   */
  
  public double evaluationResults() throws Exception {
    if (!fEvaluateResultsAvailable) {
      throw new Exception("Must evaluate first!");
    }
    return fCalculator.nodeValue(root());
  }
  
  //============================================================================
  // Differentiation
  //============================================================================

  // Differentiates the circuit.
  protected void differentiate(boolean mop) throws Exception {
    if (!fEvaluateResultsAvailable) {
      throw new Exception("Must evaluate prior to differentiating!");
    }
    if (fDifferentiateResultsAvailable) {return;}
    fCalculator.differentiate(
        mop,
        numAcNodes(),
        fNodeToType,
        fNodeToLastEdge,
        fEdgeToTailNode);
    fDifferentiateResultsAvailable = true;
  }
  
  // Returns whether differentiation results are available.
  protected boolean differentiateResultsAvailable() {
    return fDifferentiateResultsAvailable;
  }
  
  // Helps Retrieve the results of differentiation.  Inputs a list of literals,
  // performs a couple error checks, and returns a parallel list of the partials
  // or marginals or posteriors.  Any literal 0 maps to Double.NaN.  The input
  // kind should be 0 partials, 1 for marginals, and 2 for posteriors.
  protected double[] differentiationResults(
      int[] lits,
      int kind)
      throws Exception {
    if (!fDifferentiateResultsAvailable) {
      throw new Exception("Must differentiate first!");
    }
    if (lits == null) {
      throw new Exception(
          "Attempting to get differentiation results for non-query object!");
    }
    double[] ans = new double[lits.length];
    for (int litIndex = 0; litIndex < ans.length; litIndex++) {
      int l = lits[litIndex];
      if (l == 0) {
        ans[litIndex] = Double.NaN; // not a query literal
        continue;
      }
      int n = l < 0 ? fAcVarToNegLitNode[-l] : fAcVarToPosLitNode[l];
      if (n == -1) {
        ans[litIndex] = fCalculator.zero(); // node not in ac
        continue;
      }
      ans[litIndex] =
        kind == 0 ? fCalculator.nodePartial(n) :
        kind == 1 ? fCalculator.nodeMarginal(n) :
        fCalculator.nodePosterior(n, numAcNodes());
    }
    return ans;
  }  

  //============================================================================
  // Enumeration
  //============================================================================

  // Counts the circuit.
  protected void enumerate() throws Exception {
    if (!fEvaluateResultsAvailable) {
      throw new Exception("Must evaluate prior to counting!");
    }
    if (fEnumerateResultsAvailable) { return; }
    fEnumerateLastIndex = -1;
    fEnumerateInstantiation.clear();
    int numNodes = numAcNodes();
    for (int n = 0; n < numNodes; n++) {
      switch (fNodeToType[n]) {
        case MULT:
          long mCount = 1;
          int mLast = fNodeToLastEdge[n];
          for (int e = first(n); e < mLast; e++) {
            int ch = fEdgeToTailNode[e];
            long newCount = mCount * fNodeToCount[ch];
            mCount = newCount < mCount ? Long.MAX_VALUE : newCount; 
          }
          fNodeToCount[n] = mCount;
          break;
        case MAX:
          long xCount = 0;
          int xLast = fNodeToLastEdge[n];
          for (int e = first(n); e < xLast; e++) {
            int ch = fEdgeToTailNode[e];
            if (fCalculator.valuesAreClose(n, ch)) {
              long newCount = xCount + fNodeToCount[ch];
              xCount = newCount < xCount ? Long.MAX_VALUE : newCount;
            }
          }
          fNodeToCount[n] = xCount;
          break;
        case LITERAL:
        case CONSTANT:
          fNodeToCount[n] = 1;
          break;
        case ADD:
          throw new Exception("Did not expect addition node!"); 
      }
    }
    fEnumerateResultsAvailable = true;
  }

  // Returns whether counting results are available.
  protected boolean enumerateResultsAvailable() {
    return fEnumerateResultsAvailable;
  }

  // Returns the MOP count computed during the most recent counting operation
  // or Long.MAX_VALUE if this count exceeds the range of a long.
  protected long count() throws Exception {
    if (!fEnumerateResultsAvailable) {
      throw new Exception("Must enumerate first!");
    }
    return fNodeToCount[root()];
  }

  // The recursive part of enumerate().
  protected void enumerateRecurse(int root, long lastIndex, long index)
      throws Exception {
    if (lastIndex == index) { return; }  // Results valid already
    switch (fNodeToType[root]) {
      case MULT:
        int mLast = fNodeToLastEdge[root];
        for (int e = first(root); e < mLast; e++) {
          int ch = fEdgeToTailNode[e];
          long chCount = fNodeToCount[ch];
          int chIndex = (int)(index % chCount);
          index /= chCount;
          int chLastIndex;
          if (lastIndex != -1) {
            chLastIndex = (int)(lastIndex % chCount);
            lastIndex /= chCount;
          } else {
            chLastIndex = -1;
          }
          enumerateRecurse(ch, chLastIndex, chIndex);
        }
        break;
      case MAX:
        int xLast = fNodeToLastEdge[root];
        for (int e = first(root); e < xLast; e++) {
          int ch = fEdgeToTailNode[e];
          if (!fCalculator.valuesAreClose(root, ch)) { continue; }
          long chCount = fNodeToCount[ch];
          if (index < chCount) {
            enumerateRecurse(
                ch,
                (lastIndex < chCount ? lastIndex : -1),  // ok if lastIndex == -1
                index);
            return;
          }
          index -= chCount;
          lastIndex -= chCount;
        }
        throw new Exception("Should not get here.");
      case LITERAL:
        int lit = fNodeToLit[root];
        int v = (lit < 0 ? fAcVarToNegSrcVar[-lit] : fAcVarToPosSrcVar[lit]);
        if (v != -1) {
          int u = (lit < 0 ? fAcVarToNegSrcVal[-lit] : fAcVarToPosSrcVal[lit]);
          fEnumerateInstantiation.put(v, u);
        }
        break;
      case CONSTANT:
        break;
      case ADD:
        throw new Exception("Did not expect addition node!"); 
    }
  }

  protected Map<Integer, Integer> instantiation(long index) throws Exception {
    if (!fEnumerateResultsAvailable) {
      throw new Exception("Must enumerate first!");
    }
    long totalCount = fNodeToCount[root()];
    if (index < 0 || index >= totalCount) {
      throw new Exception("Enumerate index is out of range; it is " +
                          index + " and should be in [0, " + totalCount + ")");
    }
    enumerateRecurse(root(), fEnumerateLastIndex, index);
    fEnumerateLastIndex = index;
    return fEnumerateUnmodifiableInstantiation;
  }
}
