package edu.ucla.belief.ace;
import java.io.*;

/**
 * An OnlineEngine capable of answering repeated probabilistic queries for an
 * AC that has MIXED addition and maximization nodes, in addition to
 * multiplication nodes.  Supported MIXED queries currently include:
 * <ul>
 * <li> the MIXED value
 * </ul>
 * <p>
 * The implementation for this class is trivial.  Each method invokes a method
 * in the superclass.  The purpose of this class is to define which operations
 * are available for the particular compile kind by selectively making
 * operations in the superclass public.
 * 
 * @author Mark Chavira
 */

public class OnlineEngineMixed extends OnlineEngine {
  
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
   * @throws java.lang.Exception if the engine cannot be instantiated.
   */
  
  public OnlineEngineMixed(
      String lmFilename,
      String acFilename)
      throws Exception {
    super(
        new BufferedReader(new FileReader(lmFilename)),
        new BufferedReader(new FileReader(acFilename)),
        true,
        CompileKind.SOMETIMES_SUM_SOMETIMES_MAX,
        false,
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
   * @throws java.lang.Exception if the engine cannot be instantiated.
   */
  
  public OnlineEngineMixed(
      BufferedReader lmReader,
      BufferedReader acReader)
      throws Exception {
    super(
        lmReader,
        acReader,
        false,
        CompileKind.SOMETIMES_SUM_SOMETIMES_MAX,
        false,
        false);
  }
}
