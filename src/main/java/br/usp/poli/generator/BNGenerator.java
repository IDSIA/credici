package br.usp.poli.generator;
//================================================================
//     Copyright (c) 2005, Escola Polit�cnica-USP
//                     All Rights Reserved
//================================================================
//
// NAME : BNGenerator
// @DATE        : 18/06/2002 (first version)
//              : 26/09/2002 (v 0.1) with new invertion operation (multi2)
//              : 14/02/2003 (v 0.2) with Induced Width constraint
//              : 04/08/2004 (v 0.3) with constraint on incoming and outgoing arcs
//		        : 14/09/2005 (v 0.4) with generation of binary credal networks,
//                                     option to fix the number of states of variables
//                                     and generation from fixed structure
//				: 31/03/2006 (v 0.5) some additions...
//
// @AUTHOR      : Jaime Shinsuke Ide
//                jaime.ide@poli.usp.br or jaime.ide@gmail.com
//===============================================================

/* The BNGenerator distribution is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation (either
 * version 2 of the License or, at your option, any later version),
 * provided that this notice and the name of the author appear in all
 * copies.
 * If you're using the software, please notify jaime.ide@poli.usp.br so
 * that you can receive updates and patches. BNGenerator is distributed
 * "as is", in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with the BNGenerator distribution. If not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

// This class generate a random and uniform Bayesian network.
// For that, we have 2 steps:
// 1) Generate the structure of the BN
// 2) Generate the distribution function of each node.

// The remark "// testUnif" at the end of a line signify that this comand line
// is for testing uniformity of the generator. It is a experimental test and just for
// developers.

/** Version 0.3 notes **********************************
* 1) Little bug fixed: int seed= (int)(100000*random.nextFloat());
* 2) Two new constraints were added
*/
/** Version 0.4 notes **********************************
* 1) You can fix the number of states (values) with option >> -fixed_maxVal      
* 2) It is possible to generate sample of random networks from a fixed structure. >> -generateFrom graph.xml
*       Because of this, you need the BNJ package to load the network.
* 3) Now, BNGenerator generates Credal Networks!!! It is also a CNGenerator.
*       It is possible to generate credal networks in "CIF"(Credal Interchange Format) format.
*       A structure is randomly generated and a set of vertex is generated for each node.       
*/

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.DataInputStream;
import java.io.File;
import java.util.Vector;

import ch.idsia.crema.utility.RandomUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.random.MersenneTwister;

import com.opencsv.CSVWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;
import java.lang.reflect.Field;
import java.util.StringTokenizer;

import embayes.EmBayes;
import embayes.data.*;
import embayes.data.impl.*;
import embayes.infer.*;
import embayes.learn.*;

// to generate from xml file
import BNJ.impl.*;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import BNJ.*;

//import jb.graph.*;


public class BNGenerator {
    
    private int nNodes;
    private int maxDegree;
    private int maxInDegree;
    private int maxOutDegree;
    private int maxArcs;
    private int randP=0; // 0 is the default parent node
    private int randS=1; // 1 is the default son node
    private int[][] parentMatrix;
    private int[][] sonMatrix;
    private int[] nStates;
    //private Random random = new Random();
    private Random random = RandomUtil.getRandom();
    int seed= (int)(100000*random.nextFloat());
    private MersenneTwister rand= new MersenneTwister(seed+1);
    private MersenneTwister randSampleArc= new MersenneTwister(seed+2);
    private MersenneTwister randPolytree= new MersenneTwister(seed+3);
    private MersenneTwister randMulti= new MersenneTwister(seed+4);
    DFGenerator df = new DFGenerator();
    int numberStates=15000;							// for testing Uniformity
    int[] distribution = new int[numberStates];		// for testing Uniformity
    long[] matrixEq = new long[numberStates];			// for testing Uniformity
    Matrix repository[] = new Matrix[numberStates];	// for testing Uniformity
    int lastPosition=0;								// for testing Uniformity
    double expectedFrequency=0, quiSquare=0;                     // for uniformity test
    private boolean fixed_nValue = false;
    private int nPointProb=3; // default number of points used to generate credal sets
		
    EmBayes ejb = new EmBayes();
    private DataFactory dataFactory;
    private InferFactory inferFactory;
    int iwOfEachGraphType[]= new int[numberStates]; // IW
    int iwDistribution[]= new int[100]; // IW
    
    String lin="bnLink";
    StringTokenizer st = new StringTokenizer(lin);
    
	private float lowerP=0; // default interval (added on March 2006)
	private float upperP=1; // default interval (added on March 2006)

	/**
     * Default Constructor
     **/
    public BNGenerator(int nVertices, int maxDeg) {
        parentMatrix = new int[nVertices][maxDeg+2];
        sonMatrix = new int[nVertices][maxDeg+2];
        nStates = new int[nVertices];
    }
    public float getUpperP() {
        return upperP;
    }
    public void setUpperP(float upperP) {
        this.upperP = upperP;
    }
    public float getLowerP() {
        return lowerP;
    }
    public void setLowerP(float lowerP) {
        this.lowerP = lowerP;
    }
    public int getnPointProb() {
        return nPointProb;
    }
    public void setnPointProb(int nPointProb) {
        this.nPointProb = nPointProb;
    }
    public boolean isFixed_nValue() {
        return fixed_nValue;
    }
    public void setFixed_nValue(boolean fixed_nValue) {
        this.fixed_nValue = fixed_nValue;
    }
    public DataFactory getDataFactory() {
        return dataFactory;
    }
    public void setDataFactory(DataFactory dataFactory) {
        this.dataFactory = dataFactory;
    }
    public InferFactory getInferFactory() {
        return inferFactory;
    }
    public void setInferFactory(InferFactory inferFactory) {
        this.inferFactory = inferFactory;
    }
    public int getMaxArcs() {
        return maxArcs;
    }
    public void setMaxArcs(int maxArcs) {
        this.maxArcs = maxArcs;
    }
    public int getMaxOutDegree() {
        return maxOutDegree;
    }
    public void setMaxOutDegree(int maxOutDegree) {
        this.maxOutDegree = maxOutDegree;
    }
    public int getMaxInDegree() {
        return maxInDegree;
    }
    public void setMaxInDegree(int maxInDegree) {
        this.maxInDegree = maxInDegree;
    }
    public int getMaxDegree() {
        return maxDegree;
    }
    public void setMaxDegree(int maxDegree) {
        this.maxDegree = maxDegree;
    }
    public int getnNodes() {
        return nNodes;
    }
    public void setnNodes(int nNodes) {
        this.nNodes = nNodes;
    }
    /**
     * Auxiliar constructor
     **/
    public BNGenerator() {
    }
    
    /**
     *	MAIN ROUTINE
     **/
    public static void main(String argv[]) throws Exception {
        int nGraphs=1;
        //int totalDegree=10;
        String testU="no"; // default 	// for testing Uniformity
        String structure="multi"; // default structure
        String format="xml"; // default format
        String baseFileName = "Graph"; // default file name
        Integer line = null; // Default value should also be provided
        Integer nbNodes=null;
        int maxValues=2; //  Default is binary nodes.
        boolean fixed_nVal = false;
        int nIterations=0;
        int maxInducedWidth=-1; // this value means that there is no induced width constraint
        int numberNodes=4;
        int numberMaxDegree=3;
        int numberMaxInDegree=3;
        int numberMaxOutDegree=3;
        int numberMaxArcs=6;
        int nPoints=3; // default number of points used to generate credal sets
		BNGenerator bn;
        
        // auxiliar variables
        boolean maxDegreeWasSet = false;
        boolean maxInDegreeWasSet = false;
        boolean maxOutDegreeWasSet = false;
        boolean maxArcsWasSet = false;
        boolean generate=true;
        boolean loadNetwork=false; // auxiliar variables to load structure from xml file
        String auxName = "default"; // auxiliar variables to load structure from xml file
        BNGenerator auxBN = new BNGenerator();
		float lowerP=0;
        float upperP=1;

        if( argv.length<1)  {
            System.out.println("A default Bayesian network will be generated. Enter with your options!");
            auxBN.printHelp();
        }
        //// Data acquisition ////
        if( argv.length > 0 ) {
            for( int i = 0; i < argv.length; i++ ) {
                String param = argv[i];
                //******* Generated graph informations *******************************
                // a) Structural informations
                // dag structure type
                if (param.compareTo("-structure") == 0){
                    i++;
                    structure = argv[i];
                    if ( (structure.compareTo("singly") !=0) && (structure.compareTo("multi") !=0)
                    // next optins are experimental
                    && (structure.compareTo("multi2")
                    !=0 && (structure.compareTo("pmmixed") !=0)  && (structure.compareTo("testeJ") !=0) && (structure.compareTo("testeMIW") !=0)
                    && (structure.compareTo("testeMIW2") !=0)&& (structure.compareTo("testeMixedE") !=0)))   {
                        System.out.println("Wrong structure type! Valid structure: singly and multi");
                        System.exit(0);
                    }
                }
                // number of nodes
                else if (param.compareTo("-nNodes") == 0){
                    i++;
                    nbNodes = new Integer(argv[i]);
                    numberNodes = nbNodes.intValue();
                    if (numberNodes < 4)    {
                        System.out.println();
                        System.out.println("Number of nodes must be larger than 3!");
                        System.out.println("-nNodes was set to 4.");
                        numberNodes=4;
                    }
                }
                // maximum degree for all nodes
                else if (param.compareTo("-maxDegree") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    numberMaxDegree = line.intValue();
                    if (numberMaxDegree < 3)    {
                        System.out.println();
                        System.out.println("Degree of nodes must be larger than 2!");
                        System.out.println("-maxDegree was set to 3.");
                        numberMaxDegree=3;
                    }
                    maxDegreeWasSet=true;
                }
                // maximum number of incoming arcs
                else if (param.compareTo("-maxInDegree") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    numberMaxInDegree = line.intValue();
                    maxInDegreeWasSet=true;
                    if (numberMaxInDegree < 1)    {
                        System.out.println();
                        System.out.println("Value of maxInDegree has no sense. At least, one node will have a parent!");
                        System.out.println("-maxInDegree was set to 2.");
                        numberMaxInDegree=2;
                    }
                    if (numberMaxInDegree==1)    {
                        System.out.println();
                        System.out.println("Note that incoming arcs equal to 1 (-maxInDegree=1) just has sense in polytrees!");
                    }
                }
                // maximum number of outgoing arcs
                else if (param.compareTo("-maxOutDegree") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    numberMaxOutDegree = line.intValue();
                    maxOutDegreeWasSet=true;
                    if (numberMaxOutDegree < 1)    {
                        System.out.println();
                        System.out.println("Value of maxOutDegree has no sense. At least, one node will have a children!");
                        System.out.println("-maxOutDegree was set to 2.");
                        numberMaxOutDegree=2;
                    }
                    if (numberMaxOutDegree==1)    {
                        System.out.println();
                        System.out.println("Note that outgoing arcs equal to 1 (-maxOutDegree=1) has sense in polytrees!");
                    }
                }
                // maximum number of total number of arcs in the network
                else if (param.compareTo("-maxArcs") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    numberMaxArcs = line.intValue();
                    maxArcsWasSet=true;
                }
                // maxim allowed induced width
                else if (param.compareTo("-maxIW") == 0){	// for testing Uniformity
                    i++;
                    line = new Integer(argv[i]);
                    maxInducedWidth=line.intValue();
                    if (maxInducedWidth < 2)  {
                        System.out.println();
                        System.out.println("The minimum permited induced width is 2!");
                        System.out.println("-maxIW was set to 2.");
                        maxInducedWidth=2;
                    }
                }// end of if & else�s
                // b) Distributions informations
                // set maximum number of values of each node
                else if (param.compareTo("-maxVal") == 0){
                    i++;
                    line = new Integer(argv[i]);
                    maxValues=line.intValue();
                }
                // make the number of values for each node fixed
                else if (param.compareTo("-fixed_maxVal") == 0) {
                    //i++;
                    fixed_nVal = true;
                }
                
                //******* Generating process informations *******************************
                // number of graphs to generate
                else if (param.compareTo("-nBNs") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    nGraphs = line.intValue();
                }
                // number of transitions (optional) Default is 4*nNodes^2
                else if (param.compareTo("-nTransitions") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    nIterations = line.intValue();
                }
                // format of the saved network
                else if (param.compareTo("-format") == 0){
                    i++;
                    format = argv[i];
                    if ( (format.compareTo("xml") !=0) 
                        && (format.compareTo("cif") !=0)
                        &&(format.compareTo("xml_interval") !=0)
                        &&(format.compareTo("cif_interval") !=0)
						&&(format.compareTo("cif_bin") !=0) 
                        &&(format.compareTo("java") !=0)
                        &&(format.compareTo("dag") !=0) 
                        &&(format.compareTo("xmljava") !=0) 
                        &&(format.compareTo("loadEmB") !=0)) {
                        System.out.println("Wrong output format") ;
                        auxBN.printHelp();
                        System.exit(0);
                    }
                }
                // base file name to use
                else if (param.compareTo("-fName") == 0){
                    i++;
                    baseFileName = argv[i];
                }
                //
                else if (param.compareTo("-generateFrom") == 0){	// for testing Uniformity
                    i++;
                    auxName = argv[i];
                    
                    loadNetwork=true;
                }
                // set the number of points of the local credal set generated
                else if (param.compareTo("-nPoints") == 0) {
                    i++;
                    line = new Integer(argv[i]);
                    nPoints = line.intValue();
                }
				// set the interval of the generated probabilities 
                else if (param.compareTo("-minProb") == 0) {
                    i++;
					line = new Integer(argv[i]);
                    lowerP = (float)(0.1*((double)line.intValue()));
                    upperP = 1-lowerP;
                }
				// option to performing chi-square test
                else if (param.compareTo("-testUnif") == 0){	// for testing Uniformity
                    i++;
                    testU = argv[i];
                }
                // help option
                else if (param.compareTo("-help") == 0){
                    i++;
                    generate=false;
                    System.out.println();
                    System.out.println("BNGenerator - a generator for random Bayesian network. Version (0.4)");
                    auxBN.printHelp();
                    System.exit(0);
                } else    {
                    System.out.println("Probably, you have typed a wrong option! Please, verify your entry.") ;
                    auxBN.printHelp();
                    System.exit(0);
                }
            } // end of for
            System.out.println();
        } // end of if(argv.length>0)
        //.. end of data acquisition
        
        // If the option "-generateFrom (name)" is set, then networks is generated with
        // the same structure and same number of values of each variable
        // of the specified "name.xml" file.
        if (loadNetwork){
			BNGenerator bnAux = new BNGenerator();      
            if ( format.compareTo("xml") ==0) { 
				bnAux.generateXmlFromFile(nGraphs, auxName);
                System.out.println(nGraphs+" Bayesian networks were generated from the file "+auxName+".xml!");
			} else if (format.compareTo("cif") ==0) {
					bnAux.setnPointProb(nPoints);
					bnAux.generateCifFromFile(nGraphs, auxName);
                    System.out.println(nGraphs+" credal networks were generated from the file "+auxName+".xml! Each local credal set has "+nPoints+" point probabilities.");
					}	else System.out.println("Please, specify the -format that the networks will be generated (cif or xml).");
			System.exit(0);
            
        }
        
        // set default values, if it was not set
        if (!maxDegreeWasSet)   numberMaxDegree = numberNodes-1;
        if (!maxInDegreeWasSet)   numberMaxInDegree=numberMaxDegree;
        if (!maxOutDegreeWasSet)   numberMaxOutDegree=numberMaxDegree;
        if (!maxArcsWasSet)   numberMaxArcs = numberNodes*numberMaxDegree/2;	// Set a default maximum number of arcs (all combinations)
        
        // verify some incoherent data
        if (maxArcsWasSet)  {
            int auxNumber=numberNodes*numberMaxDegree/2;
            if (numberMaxArcs > auxNumber)  {
                System.out.println();
                System.out.println("Note that the value maxArcs="+numberMaxArcs+" has no sense.");
                System.out.println("Because the maximum possible number of arcs is "+auxNumber+" for nNodes="+numberNodes+" and maxDegree="+numberMaxDegree);
                numberMaxArcs=auxNumber;
            }
            if  (numberMaxArcs < (numberNodes-1) )  {
                System.out.println();
                System.out.println("maxArcs was corrected to "+numberNodes+"(minimum value for maxArcs)");
                System.out.println("Note that for maxArcs="+(numberNodes-1)+", the graph is a polytree. Choose 'poly' structure.");
                numberMaxArcs=numberNodes;
            }
        }
        if (maxInDegreeWasSet || maxOutDegreeWasSet)  {
            if (structure.compareTo("multi")==0) {
                if (numberMaxOutDegree==1)  {
                    numberMaxOutDegree=2;
                    System.out.println("-maxOutDegree was set to 2.");
                }
                if (numberMaxInDegree==1)  {
                    numberMaxInDegree=2;
                    System.out.println("-maxInDegree was set to 2.");
                }
            }
        }
        if (maxInDegreeWasSet && maxOutDegreeWasSet)  {
            if ((numberMaxInDegree+numberMaxOutDegree)==2)    {
                System.out.println();
                System.out.println("Sum of incoming and outgoing arcs must be larger than 2!");
                System.out.println("-maxOutDegree was set to 2.");
                numberMaxOutDegree=2;
            }
        }
        // Set global variables
        bn = new BNGenerator(numberNodes,numberMaxDegree);
        bn.setnNodes(numberNodes);  // set nNodes
        bn.setMaxDegree(numberMaxDegree);  // set maxDegree
        bn.setMaxInDegree(numberMaxInDegree);  // set maximum number of incoming arcs
        bn.setMaxOutDegree(numberMaxOutDegree);  // set maximum number of outgoing arcs
        bn.setMaxArcs(numberMaxArcs);  // set maxArcs(a global variable)
        bn.setDataFactory(embayes.data.impl.DataBasicFactory.getInstance());
        bn.setInferFactory(embayes.infer.impl.InferBasicFactory.getInstance(bn.getDataFactory()));
        bn.setFixed_nValue(fixed_nVal);
		bn.setnPointProb(nPoints);
		bn.setLowerP(lowerP);
		bn.setUpperP(upperP);
        
        // Determining the number of iterations for
        // the chain to converge is a difficult task.
        // This value follows the DagAlea (see Melancon;Bousque,2000) suggestion,
        // and we verified that this number is satisfatory:
        if (nIterations == 0)
            nIterations = 6*bn.getnNodes()*bn.getnNodes();
        
        boolean exist,conditionSatisfied; // auxiliary variables
        bn.inicializeGraph(); // Inicialize a simple ordered tree as a BN structure
        
        if ( (testU.compareTo("yes") == 0))	{										// for testing Uniformity
            for (int i=0;i<bn.numberStates;i++ )  bn.repository[i] = new Matrix();	// for testing Uniformity
            bn.repository[0].inicializeTable(bn.getnNodes(),bn.getMaxDegree()+2);				// for testing Uniformity
        }
        
        //// Generating process ////
        if ( (structure.compareTo("singly") == 0))	{	//  polytree
            bn.generatePolytree(nGraphs,nIterations,maxValues,testU,format,baseFileName,maxInducedWidth);
        } // end of if(structure=poly)
        if	((structure.compareTo("multi") == 0)&&(maxInducedWidth==-1)) {	// multi-connected graph is the default structure
            bn.generateMultiConnected(nGraphs,nIterations,maxValues,testU,format,baseFileName);
        } // end of if(structure=multi)
        if ((structure.compareTo("multi") == 0)&&(maxInducedWidth!=-1))	{	// generate multi-connected with maxIW constraint
            bn.generateMixedEJ(nGraphs,nIterations,maxValues,testU,format,baseFileName,maxInducedWidth);
        } // end of if(structure=pmmixed)
        
        if ( (testU.compareTo("yes") == 0))	{									// for testing Uniformity
            bn.printDistribution();											// for testing Uniformity
            if (maxInducedWidth!=-1)    bn.printAnalisys1(nGraphs); // IW
            if (maxInducedWidth!=-1)    bn.printAnalisys2(); // IW
            bn.computeQuiSquare(nGraphs);
            System.out.println();
            System.out.println("\tCounting different graphs:"+bn.lastPosition);
            System.out.println("\tqui-Square:"+bn.quiSquare);
            bn.saveDistribution("UTest_","doc",nGraphs,nIterations,structure,maxInducedWidth);	// for testing Uniformity
            //bn.averageQuiSquare(nGraphs,nIterations,maxValues,testU,format,baseFileName,maxInducedWidth);
        }
        bn.printInformations(nGraphs, nIterations, maxValues,fixed_nVal,structure, format, baseFileName,testU,maxInducedWidth);
        //bn.pause();
        //bn.ckeckingCycle(nGraphs,nIterations,maxValues,testU,format,baseFileName,maxInducedWidth);
        System.out.println("Networks generated!");
        System.exit(0);
        
    }   // end of MAIN ROUTINE
    
    
    public void printHelp(){
        System.out.println();
        System.out.println("Type: BNGenerator [-option value]...");
        System.out.println("Available command options:");
        System.out.println("\tStructure of generated graphs:[-structure singly] or [-structure multi]");
        System.out.println("\tNumber of nodes:[-nNodes value]");
        System.out.println("\tMax degree for each node: [-maxDegree value]");
        System.out.println("\tMaximum number of incoming arcs for each node: [-maxInDegree value]");
        System.out.println("\tMaximum number of outgoing arcs for each node: [-maxOutDegree value]");
        System.out.println("\tMaximum total number of arcs: [-maxArcs value]");
        System.out.println("\tMaximum induced-width (tree-width) allowed: [-maxIW value]");
        System.out.println("\tMaximum number of value (state) for each node: [-maxVal value]");
        System.out.println("\tThis option fix the number of values to maxVal: [-fixed_maxVal]");
        System.out.println("\tNumber of generated graphs: [-nBNs value]");
        System.out.println("\tNumber of transitions between samples (default value is 6*nNodes*nNodes): [-nTransitions value]");
        System.out.println("\tSaved file format (xml is the default format): [-format xml] or [-format java]");
        System.out.println("\tTo save a Credal Network (save just the linear \"base\" of the local credal sets): [-format cif]");
        System.out.println("\tNumber of points of the generated local credal set: [-nPoints value]");
		System.out.println("\tTo save 2 binaries Bayesian networks (Low and High) equivalent to a binary credal network: [-format xml_interval]");
        System.out.println("\tTo generate from a fixed network structure name.xml: [-generateFrom name]");
		System.out.println("\tName of saved file: [-fName name]");
    }
    
    public void printInformations(int nG, int nI,int maxV,boolean fixed_nVal,String sty, String f, String fName, String testUnif, int maxIW){
        System.out.println();
        System.out.println("Structural informations of generated graphs:");
        System.out.println("\tNumber of nodes:"+getnNodes());
        System.out.println("\tMax value of states for each node:"+maxV);
        if (fixed_nVal) System.out.println("\t\t(value of states for every node was fixed)");
        System.out.println("\tMax degree for each node:"+getMaxDegree());
        System.out.println("\tMaximum number of incoming arcs for each node:"+getMaxInDegree());
        System.out.println("\tMaximum number of outgoing arcs for each node:"+getMaxOutDegree());
        System.out.println("\tMaximum total number of arcs:"+getMaxArcs()+" of "+(getnNodes()*getMaxDegree()/2)+" possibles" );
        if (maxIW==-1)  System.out.println("\tMaximum induced-width allowed: (without restriction) ");
        else   System.out.println("\tMaximum induced-width allowed:"+maxIW);
        System.out.println("\tStyle of generated graphs:"+sty);
        System.out.println("Generating process informations:");
        System.out.println("\tNumber of generated graphs:"+nG);
        System.out.println("\tNumber of transitions between samples:"+nI);
        System.out.println("\tSaved file format:"+f);
        System.out.println("\tFile name:"+fName);
        System.out.println("\tTesting uniformity:"+testUnif);
    }
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Methods for generating structures //////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    
    void generatePolytree(int nGraphs, int nIterations, int maxValues,
            String testU, String format, String baseFileName, int maxIW)	throws Exception	{
        int inducedWidth=1;  // IW
        int max=0;
        if (maxIW!=-1)  System.out.println("Maximum Induced-width is:"+maxIW); // IW
        for (int g = 0; g < nGraphs; g++) {
            System.out.println("Generating polytree :"+g);
            for (int i=0;i<nIterations;i++ )  {
                //if (verifyCoherency()== true)	{
                boolean exist;
                sampleArc();
                if (followMaxDegree(getMaxDegree()-1))
                    exist=existArc(randP,randS);
                else exist=true;
                
                if (exist==false)	{
                    int remove[]=findArcToRemove(randP,randS);
                    if (randPolytree.nextFloat() > 0.5)	{   // the factor 0.5 is important for uniformity
                        addArc(randS,randP); // note that arc was inverted!! randP and randS are atualized
                        removeArc(remove[0],remove[1]); // old one is removed
                        //@	System.out.println("\t Node was inverted and added");
                        if (followMaxInDegree(getMaxInDegree()))	{
                            if (followMaxOutDegree(getMaxOutDegree()))	{
                                if (maxIW!=-1)   {// induced width contraint was selected and it is necessary to verify
                                    int auxIW=inducedWidth;
                                    loadToEmBayes("loadingBN"); // IW
                                    inducedWidth=getInducedWidth(); // IW
                                    if (inducedWidth>maxIW)  {      // new graph rejected
                                        removeArc();
                                        addArc(remove[0],remove[1]);    // add the arc that was removed
                                        inducedWidth=auxIW;
                                    }
                                }  // end of if(maxIW)
                            } // end of if(followMaxOutDegree)
                            else {
                                removeArc();
                                addArc(remove[0],remove[1]);    // add the arc that was removed
                            }
                        } // end of if(followMaxInDegree)
                        else {
                            removeArc();
                            addArc(remove[0],remove[1]);    // add the arc that was removed
                        }
                        
                    } else	{
                        addArc(randP,randS);
                        removeArc(remove[0],remove[1]);
                        //@	System.out.println("\t Node just added");
                        if (followMaxInDegree(getMaxInDegree()))	{
                            if (followMaxOutDegree(getMaxOutDegree()))	{
                                if (maxIW!=-1)   {// induced width contraint was selected and it is necessary to verify
                                    int auxIW=inducedWidth;
                                    loadToEmBayes("loadingBN"); // IW
                                    inducedWidth=getInducedWidth(); // IW
                                    if (inducedWidth>maxIW)  {      // new graph rejected
                                        removeArc();
                                        addArc(remove[0],remove[1]);    // add the arc that was removed
                                        inducedWidth=auxIW;
                                    }
                                }
                            } // end of if(followMaxOutDegree)
                            else {
                                removeArc();
                                addArc(remove[0],remove[1]);    // add the arc that was removed
                            }
                        } // end of if(followMaxInDegree)
                        else {
                            removeArc();
                            addArc(remove[0],remove[1]);    // add the arc that was removed
                        }
                    }
                    //@	System.out.println("\t Added arc is ("+randP+","+randS+").");
                    //@	System.out.println("\t The arc("+remove[0]+","+remove[1]+") was removed.");
                    
                    
                } // end of if(exist)
                
            } // end of for(iteration)
            
            if ( (testU.compareTo("yes") == 0))	{
                loadToEmBayes("loadingBN"); // IW
                inducedWidth=getInducedWidth(); // IW
                if (inducedWidth>max) max=inducedWidth;
                testInducedWidth(g,inducedWidth);
                testUniformity(inducedWidth);	// testUnif
            } else	{
                if ((format.compareTo("xml") ==0))	{
                    saveBNXML(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("dag") ==0))	{
                    saveDAG(baseFileName,format,g,maxValues, inducedWidth, getMaxDegree(), getMaxInDegree(), getMaxOutDegree(), getMaxArcs());
                }
                if ((format.compareTo("java") == 0)) {
                    saveBNJava(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("xmljava") == 0)) {
                    saveBNXMLJava(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("xml_interval") ==0))	{
                    saveBNXML_interval(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif_bin") ==0))	{
                    saveBinaryCIF(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif") ==0))	{
                    saveCIF(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("cif_interval") ==0))	{
                    saveCIF_interval(baseFileName,format,g,maxValues);
                }

            }
        } // end of for(graphs)
        if ( (testU.compareTo("yes") == 0))  System.out.println("\t Maximum induced-width reached was: "+max); // IW
    } // end of generatePolytree()
    
    
    /* Simple algorithm to generate multiconnected graphs, without Induced Width constraint
     */
    void generateMultiConnected(int nGraphs, int nIterations, int maxValues,
            String testU, String format, String baseFileName)	throws Exception	{
        int inducedWidth=1;
        int totalArcs=getnNodes()-1; // Simple tree has (nNodes-1)arcs
        for (int g = 0; g < nGraphs; g++) {
            //inicializeGraph();	// Inicialize a simple ordered tree as a BN structure
            System.out.println("Generating multi-connected graph:"+g);
            int auxTotal = getMaxArcs();
            // totalArcs=nNodes-1; // totalArcs must be inicialized if graph is inicialized
            for (int i=0;i<nIterations;i++ )  {
                //if (verifyCoherency()== true)	{
                boolean exist,conditionSatisfied; // auxiliary variables
                boolean inverted=false;
                sampleArc();
                exist=existArc(randP,randS);
                if (exist==false)	{
                    addArc(randP,randS);
                    totalArcs++;
                    //System.out.println("\t Node just added");
                }else	{ // arc exists
                    removeArc();
                    totalArcs--;
                    ///System.out.println("\t Arc removed for verifying");
                } // end of else
                //printArcs();
                
                conditionSatisfied=false;
                if (exist==false) {
                    if (totalArcs<=auxTotal)
                        if (followMaxDegree(getMaxDegree()))
                            if (followMaxInDegree(getMaxInDegree()))
                                if (followMaxOutDegree(getMaxOutDegree()))
                                    if(isAcyclic())  conditionSatisfied=true;
                } // end of if(exist)
                else   // if the node has removed, it is necessary just verify if continue connected
                    if (isConnected())   conditionSatisfied=true;
                
                if (conditionSatisfied==false)	{
                    if (exist)  {
                        addArc();
                        totalArcs++;
                        //System.out.println("\t Arc re-added");
                    }else {
                        removeArc();
                        totalArcs--;
                        //System.out.println("\t Arc ad-removed");
                    }
                } // end of if (conditionSatisfied)
                
                //printArcs();
                //} // for coherency
                //else {
                // System.out.println("\t********  MATRIX�S NOT COHERENT!! ********** ");
                //  System.exit(0);
                //} // end of else
                //pause();
                
            } // end of for(iteration)
            
            if ( (testU.compareTo("yes") == 0)&& !((sonMatrix[randP][0]-1) ==3) )	{
                //loadToEmBayes("loadingBN"); // IW
                //inducedWidth=getInducedWidth(); // IW
                testUniformity(inducedWidth);	// testUnif
            } else	{
                if ((format.compareTo("xml") ==0))	{
                    saveBNXML(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("dag") ==0))	{
                    saveDAG(baseFileName,format,g,maxValues, inducedWidth, getMaxDegree(), getMaxInDegree(), getMaxOutDegree(), getMaxArcs());
                }
                if ((format.compareTo("java") == 0)) {
                    saveBNJava(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("xmljava") == 0)) {
                    saveBNXMLJava(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("xml_interval") ==0))	{
                    saveBNXML_interval(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif_bin") ==0))	{
                    saveBinaryCIF(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif") ==0))	{
                    saveCIF(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("cif_interval") ==0))	{
                    saveCIF_interval(baseFileName,format,g,maxValues);
                }
            }
            
        } // end of for(graphs)
        
    } // end of generateMultiConnected()
    
    
    
    
    ////// This method implement the mixed strategy with jump operation (see article)
    void generateMixedEJ(int nGraphs, int nIterations, int maxValues, String testU, String format, String baseFileName, int maxIW)throws Exception{
        //double rate1=0.7; // transition rate
        // double rate2=0.2; // transition rate
        //double rate3=0.1; // transition rate
        inicializeGraph(); // Inicialize a simple ordered tree as a BN structure
        int inducedWidth=1;
        int totalArcs=getnNodes()-1; // Simple tree has (nNodes-1)arcs
        int auxTotal = getMaxArcs();
        double  random;
        System.out.println("**** Maximum Induced-width allowed is:"+maxIW);
        int statistics[]= new int[6];
        for (int g = 0; g < nGraphs; g++) {
            System.out.println("Generating multi-connected(with PMMixed algorithm) graph:"+g);
            /********************************************** Iteraction to obtain one graph *********************/
            for (int i=0;i<nIterations;i++ )  {
                random=rand.nextDouble();
                /********************************************** is polytree******************************/
                if (totalArcs==getnNodes()-1)   {    // this condition verify if graph is a polytree
                    if ( random<=0.1)	{       // AR operation
                        boolean exist;
                        sampleArc();
                        if (followMaxDegree(getMaxDegree()-1))
                            exist=existArc(randP,randS);
                        else exist=true;
                        if (exist==false)	{
                            int remove[]=findArcToRemove(randP,randS);
                            if (randPolytree.nextFloat() > 0.5)	{   // the factor 0.5 is important for uniformity
                                addArc(randS,randP); // note that arc was inverted!! randP and randS are atualized
                                removeArc(remove[0],remove[1]);
                                int auxIW=inducedWidth;
                                loadToEmBayes("loadingBN"); // IW
                                inducedWidth=getInducedWidth(); // IW
                                if (inducedWidth>maxIW)  {      // new graph rejected
                                    removeArc();
                                    addArc(remove[0],remove[1]);    // add the arc that was removed
                                    inducedWidth=auxIW;
                                }
                            } else	{
                                addArc(randP,randS);
                                removeArc(remove[0],remove[1]);
                                int auxIW=inducedWidth;
                                loadToEmBayes("loadingBN"); // IW
                                inducedWidth=getInducedWidth(); // IW
                                if (inducedWidth>maxIW)  {      // new graph rejected
                                    removeArc();
                                    addArc(remove[0],remove[1]);
                                    inducedWidth=auxIW;
                                }
                            }
                        } // end of if(exist)
                    }    // end of if(random>=rate)
                    else if (random<=0.8)	{       // A or R operation
                        
                        boolean exist,conditionSatisfied; // auxiliary variables
                        sampleArc();
                        exist=existArc(randP,randS);
                        conditionSatisfied=false;
                        if (exist==false) {
                            addArc(randP,randS);
                            totalArcs++;
                            if (totalArcs<=auxTotal)
                                if (followMaxDegree(getMaxDegree()))
                                    if(isAcyclic()) {
                                int auxIW=inducedWidth;
                                loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                                inducedWidth=getInducedWidth();
                                if (inducedWidth<=maxIW)   conditionSatisfied=true;
                                else    inducedWidth=auxIW;
                                    } // end of if
                        } // end of if(exist)
                        if (exist==false)	{
                            if (conditionSatisfied==false)   {
                                removeArc();
                                totalArcs--;
                            }
                        } // end of if (conditionSatisfied)
                    } // end of else (A or R operation)
                    else {  // special operation to move for multiconnected
                        int initialPolytreeP[][]=receiveMatrix(parentMatrix,getnNodes(),getMaxDegree()+2);
                        int initialPolytreeS[][]=receiveMatrix(sonMatrix,getnNodes(),getMaxDegree()+2);
                        int auxIW=inducedWidth;
                        boolean passed=false;
                        boolean exist=false;
                        boolean conditionSatisfied=true;
                        random=rand.nextDouble();
                        while   ((random<0.2)&&(conditionSatisfied==true)&&(exist==false))  {
                            sampleArc();
                            exist=existArc(randP,randS);
                            conditionSatisfied=false;
                            if (exist==false) {
                                addArc(randP,randS);
                                totalArcs++;
                                if (totalArcs<=auxTotal)
                                    if (followMaxDegree(getMaxDegree()))
                                        if(isAcyclic()) {
                                    conditionSatisfied=true;
                                        } // end of if
                            } // end of if(exist)
                            random=rand.nextDouble();
                        }   // end of while(random<0.1)
                        if (conditionSatisfied==true)   {
                            loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                            inducedWidth=getInducedWidth();
                            if (inducedWidth<=maxIW){
                                passed=true;
                                conditionSatisfied=true;
                            } else conditionSatisfied=false;
                        }
                        if (conditionSatisfied==false||exist==true)  {
                            statistics[0]++;
                            parentMatrix=receiveMatrix(initialPolytreeP,getnNodes(),getMaxDegree()+2);
                            sonMatrix=receiveMatrix(initialPolytreeS,getnNodes(),getMaxDegree()+2);
                            inducedWidth=auxIW;
                            totalArcs=getnNodes()-1;
                        }
                        if (passed==true&&conditionSatisfied==true&&exist==false&&random>0.1)		statistics[2]++;
                        statistics[4]++;
                    } // end of else(// special opertion to move for multiconnected)
                    
                }   // end of if(polytree)
                
                /********************************************** is multiconnected  ************************/
                else    {       // actual graph is multiconnected, then use A or R operation
                    random=rand.nextDouble();
                    if (random<=0.8){
                        boolean exist,conditionSatisfied; // auxiliary variables
                        int aux=inducedWidth; // aux keep the initial induced-width of the multiconnected graph
                        sampleArc();
                        exist=existArc(randP,randS);
                        if (exist==false)	{
                            addArc(randP,randS);
                            totalArcs++;
                        }else	{ // arc exists
                            removeArc();
                            totalArcs--;
                        } // end of else
                        conditionSatisfied=false;
                        if (exist==false) { // if the arc does not exist, naturaly the arc is added and the remain graph is multiconnected
                            if (totalArcs<=auxTotal)
                                if (followMaxDegree(getMaxDegree()))
                                    if(isAcyclic()) {
                                loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                                inducedWidth=getInducedWidth();
                                if (inducedWidth<=maxIW)   conditionSatisfied=true;
                                else    inducedWidth=aux;
                                    } // end of if
                        } // end of if(exist)
                        else   {// if the node has removed, it is necessary just verify if continue connected and if it is multiconnected
                            if (isConnected()==true)   {
                                loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                                inducedWidth=getInducedWidth();
                                conditionSatisfied=false;
                                if (inducedWidth<=maxIW) {
                                    if (totalArcs==getnNodes()-1)   {    // verify if graph is a polytree
                                        if (random<0.7)    conditionSatisfied=true;    // accept the polytree
                                        else inducedWidth=aux;
                                    }   // end of if(polytree)
                                    else {
                                        conditionSatisfied=true;    // resultant graph is multiconnected => accept
                                    }
                                } else inducedWidth=aux;  // reject the graph
                            }
                        }
                        if (conditionSatisfied==false)	{
                            if (exist)  {
                                addArc();
                                totalArcs++;
                            }else {
                                removeArc();
                                totalArcs--;
                            }
                        } // end of if (conditionSatisfied)
                    } // end of if (rate)
                    else  {   // special operation to move to polytree
                        int initialMulticonnectedP[][]=receiveMatrix(parentMatrix,getnNodes(),getMaxDegree()+2);
                        int initialMulticonnectedS[][]=receiveMatrix(sonMatrix,getnNodes(),getMaxDegree()+2);
                        int auxIW=inducedWidth;
                        boolean passed=false;
                        boolean conditionSatisfied=true;
                        boolean exist=true;
                        int auxTotalArcs=totalArcs;
                        random=rand.nextDouble();
                        while   (random<0.2&&(conditionSatisfied==true)&&(exist==true))  {
                            sampleArc();
                            exist=existArc(randP,randS);
                            conditionSatisfied=false;
                            if (exist==true) {
                                removeArc();
                                totalArcs--;
                                if (isConnected()==true)   {
                                    conditionSatisfied=true;
                                }
                            }   // end of if(exist)
                            random=rand.nextDouble();
                        }   // end of while(random<0.1)
                        if (conditionSatisfied==true)   {
                            loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                            inducedWidth=getInducedWidth();
                            if (inducedWidth<=maxIW){
                                passed=true;
                                conditionSatisfied=true;
                            } else conditionSatisfied=false;
                        }
                        if (conditionSatisfied==false||exist==false||(totalArcs!=getnNodes()-1))  {
                            statistics[1]++;
                            parentMatrix=receiveMatrix(initialMulticonnectedP,getnNodes(),getMaxDegree()+2);
                            sonMatrix=receiveMatrix(initialMulticonnectedS,getnNodes(),getMaxDegree()+2);
                            inducedWidth=auxIW;
                            totalArcs=auxTotalArcs;
                        }
                        if (passed==true&&conditionSatisfied==true&&exist==true&&random>0.1&&(totalArcs==getnNodes()-1))	statistics[3]++;
                        statistics[5]++;
                    } // end of else (special operation)
                    
                }   //end of else(multiconnected);
                
            } // end of for(iteration)
            
            loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
            inducedWidth=getInducedWidth();
            System.out.println("**** The induced width of actual graph, before saving, is:"+inducedWidth);
            
            if ( (testU.compareTo("yes") == 0))	{
                loadToEmBayes("loadingBN"); // loadind network to EmBayes for computing induced-width
                inducedWidth=getInducedWidth();
                if (inducedWidth>maxIW) throw(new Exception("Error at IW verification!!!"));
                testInducedWidth(g,inducedWidth);
                testUniformity(inducedWidth);	// testUnif
            } else	{
                if ((format.compareTo("xml") ==0))	{
                    saveBNXML(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("dag") ==0))	{
                    saveDAG(baseFileName,format,g,maxValues, inducedWidth, getMaxDegree(), getMaxInDegree(), getMaxOutDegree(), getMaxArcs());
                }
                if ((format.compareTo("java") == 0)) {
                    saveBNJava(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("xmljava") == 0)) {
                    saveBNXMLJava(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("xml_interval") ==0))	{
                    saveBNXML_interval(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif_bin") ==0))	{
                    saveBinaryCIF(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("cif") ==0))	{
                    saveCIF(baseFileName,format,g,maxValues);
                }
				if ((format.compareTo("cif_interval") ==0))	{
                    saveCIF_interval(baseFileName,format,g,maxValues);
                }
                if ((format.compareTo("loadEmB") == 0)) {
                    loadToEmBayes("bnLink");
                    ejb.mainEmBayes(ejb);
                }
            }
            
        } // end of for(graphs)
        /*
        System.out.println("1) Do not poly-multi:"+statistics[0]);
        System.out.println("2) Do not multi-poly:"+statistics[1]);
        System.out.println("3) Number of times that happened transition poly_multi:"+statistics[2]);
        System.out.println("4) Number of times that happened transition multi_poly:"+statistics[3]);
        System.out.println("(1)+(3)?:"+statistics[4]);
        System.out.println("(2)+(4)?:"+statistics[5]);
         */
    } // end of generateTest3()
    
	void generateXmlFromFile(int nGraphs, String networkName) throws Exception {
		LinkedList bayesnet;
        LoadFile lf;
        FileIO file;
        lf = new LoadFileImpl();
        file = new FileIOImpl();
        bayesnet = file.load(networkName+".xml");
        BayesNet network= lf.loadBayesNet(bayesnet);
        int pMatrix[][]= new int[network.numberVariables()][network.numberVariables()];
        int nStates[]= new int[network.numberVariables()];
        for (int j=0;j<network.numberVariables();j++) {
            nStates[j]=network.getVariable(j).numberCategories();
            pMatrix[j][0]=1; // initialize pMatrix
        }
        generateParentMatrix(network,pMatrix);
        parentMatrix=pMatrix;        
        for (int g = 0; g < nGraphs; g++) {
			saveBNXML_fromFile(networkName,"xml",g,nStates);
        }
    }

    void generateCifFromFile(int nGraphs, String networkName) throws Exception {
		// set the number of point probabilities of the local credal set
        int nTables=getnPointProb(); 

		LinkedList bayesnet;
        LoadFile lf;
        FileIO file;
        lf = new LoadFileImpl();
        file = new FileIOImpl();
        bayesnet = file.load(networkName+".xml");
        BayesNet network= lf.loadBayesNet(bayesnet);
        int pMatrix[][]= new int[network.numberVariables()][network.numberVariables()];
        int nStates[]= new int[network.numberVariables()];
        for (int j=0;j<network.numberVariables();j++) {
            nStates[j]=network.getVariable(j).numberCategories();
            pMatrix[j][0]=1; // initialize pMatrix
        }
        generateParentMatrix(network,pMatrix);
        parentMatrix=pMatrix;
        
        for (int g = 0; g < nGraphs; g++) {
			saveCIF_fromFile(networkName,g,nStates,nTables) ;
        }
    }
    
    /////////////////////////////////////////////////////////////////////////////////
    //////////////////// Methods for Induced Width (IW) analisys ////////////////////
    ////////////////////////////////////////////////////////////////////////////////
    
/*
 * Return the induced width of the actual loaded network, using order determined by
 * greedy-ordering, used in Bucket eliminaiton of EmBayes
 */
    public int getInducedWidth() {
        Ordering order;
        BucketTree bucketTree;
        CategoricalVariable allVariables[]=getNetwork().getVariables();
        CategoricalVariable auxVars[];
        CategoricalVariable auxVar;
        int inducedWidth=0;
        //int nVar=ejb.network.numberVariables();
        int nVar=getNetwork().numberVariables();
        boolean requisiteVariables[]= new boolean[nVar];
        //Bucket bucket[]= new Bucket[nVar];
        order = getInferFactory().newOrdering();
        for (int i=0;i<nVar;i++)    requisiteVariables[i]=true;
        
        bucketTree = order.generateOrdering(getNetwork(), requisiteVariables);
        //System.out.println("\n Selected ordering: ");
        //for (int i=0;i<bucketTree.numberBuckets();i++)    {
        //    auxVars=bucketTree.getBucket(i).getBucketVariables();
        //    auxVar=auxVars[0];
        //    System.out.println("\n Variable["+i+"] is "+auxVar.getName());
        //}
        inducedWidth=order.getInducedWidth();
        //System.out.println("\n The induced width of loaded network, with above ordering, is: "+inducedWidth);
        return(inducedWidth);
    }
    
    public void printAnalisys2() {
        System.out.println("\n**************** Induced width analisys *************************************");
        System.out.println("******(counting number of different type of graph with same iw)**************");
        System.out.println("*****************************************************************************");
        int aux[]= new int[30]; // suppose that we have graphs with iw<29
        for (int i=0; iwOfEachGraphType[i]!=0;i++)  {
            aux[iwOfEachGraphType[i]]++;
        }
        for (int i=0; i<aux.length;i++)  {
            System.out.println("\t Number of TYPE of graphs with iw="+i+" is: "+aux[i]);
        }
    }
    public void testInducedWidth(int graph,int iw)  {
        iwDistribution[iw]=iwDistribution[iw]+1;
        System.out.println("\t **************** The induced width of graph "+graph+" is: "+iw);
    }
    public void printAnalisys1(int g) {
        System.out.println("\n**************** Induced width analisys ********************");
        System.out.println("******(counting number of graphs with same iw)**************");
        System.out.println("************************************************************");
        for (int i=0; i<iwDistribution.length;i++)  {
            System.out.println("\t Number of graphs with iw="+i+" is: "+iwDistribution[i]+" ( "+((double)iwDistribution[i]/g*100)+"% )" );
        }
    }
    

    BayesNet getNetwork() {

     try {
            Field f1 = EmBayes.class.getDeclaredField("network");
            f1.setAccessible(true);
            BayesNet bn = (BayesNet) f1.get(ejb);
            return bn;
        } catch (Exception nfe) {
            nfe.printStackTrace();
            return null;
        }
    }

    void setNetwork(BayesNet bn) {
      
        try {
            Field f1 = EmBayes.class.getDeclaredField("network");
            f1.setAccessible(true);
            f1.set(ejb, bn);

        } catch (Exception nfe) {
            nfe.printStackTrace();
        }
    }

    public void loadToEmBayes(String name) throws Exception {
        DataFactory factory = embayes.data.impl.DataBasicFactory.getInstance();
        
        BayesNet bn = factory.newBayesNet();
        bn.setName(name);
        setNetwork(bn);
        //ejb.network = factory.newBayesNet();
        //ejb.network.setName(name);
        
        CategoricalVariable variables[]= new CategoricalVariable[getnNodes()];
        CategoricalProbability probabilities[]= new CategoricalProbability[getnNodes()];
        
        String node = "n";
        int aux;
        
        // Get kinship from parentMatrix and set variables and probabilities of network
        for (int j=0;j<getnNodes();j++) {
            //System.out.println("\n Node:"+j);
            //System.out.println("\n number of states:"+nStates[j]);
            String categories[] = new String[nStates[j]];
            //System.out.println("\n state:"+categories[0]);
            ////for (int s=0;s<nStates[j];s++)  categories[s]=("state"+s);
            variables[j] =
                    factory.newCategoricalVariable(node.concat(""+j), new String[] {"state0","state1"});
                    //factory.newCategoricalVariable(node.concat(""+j), categories);
        }
        for (int j=0;j<getnNodes();j++) {
            aux = parentMatrix[j][0]; // aux: is the (number of parents of node j) plus one
            if (aux==1)   { // node j does not have parents
                probabilities[j] =
                        factory.newCategoricalProbability(variables[j],null);
                //factory.newCategoricalProbability(variables[j],inputProbabilities(j, aux));
            } else    {
                probabilities[j] =
                        factory.newCategoricalProbability(variables[j],inputVariables(variables,j,aux),null);
            }
        } //end of for (j)
        //ejb.network.setVariables(variables);
        //ejb.network.setProbabilities(probabilities);
        bn.setVariables(variables);
        bn.setProbabilities(probabilities);
    }
    
    public CategoricalVariable[] inputVariables(CategoricalVariable[] variables, int node, int aux) {
        CategoricalVariable v[] = new CategoricalVariable[aux-1];
        for (int i=1; i<aux; i++)   {
            v[i-1]=variables[parentMatrix[node][i]];
        }
        return (v);
    }
    
    public double[] inputProbabilities(int node, int aux) {
        double prob[] = new double[(int)Math.pow(2,aux)];
        for (int i=0; i<prob.length; i++)   {
            prob[i]=0.5;
        }
        return (prob);
    }
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Methods for verifying conditions ///////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    
    public boolean verifyCoherency()  {
        boolean ok;
        for (int i=0;i<getnNodes();i++)	{
            ok=false;
            for (int j=1;j<parentMatrix[i][0];j++)	{
                for (int jj=1;jj<sonMatrix[parentMatrix[i][j]][0];jj++)	{
                    if (sonMatrix[parentMatrix[i][j]][jj]==i) ok=true;
                }
                if (ok==false) return(false);
            }
        }
        return(true);
    }
    
    private boolean existArc(int parent, int son)  {  // return true if the arc exist
        for (int i=1;i<parentMatrix[son][0];i++)  {
            if (parentMatrix[son][i]==parent) return(true);
        }
        return(false);
    }
    
    private boolean followMaxDegree(int maxD)  {  // return true if the degree of current node randP and randS do not exceed maxDegree
        if ( (parentMatrix[randP][0]+sonMatrix[randP][0]-2) <= maxD )
            if ((parentMatrix[randS][0]+sonMatrix[randS][0]-2) <= maxD)	return(true);
            else return(false);
        else return(false);
    }
    
    private boolean followMaxInDegree(int maxDIn)  {  // return true if the degree of current node randP and randS do not exceed maxInDegree
        if ((parentMatrix[randS][0]-1) <= maxDIn)	return(true);
        else return(false);
    }
    private boolean followMaxOutDegree(int maxDOut)  {  // return true if the degree of current node randP and randS do not exceed maxOutDegree
        if ((sonMatrix[randP][0]-1) <= maxDOut)	return(true);
        else return(false);
    }
    
    public boolean isConnected() {  // return true if the current graph is connected
        boolean visited[] = new boolean[getnNodes()];
        boolean resp=false;
        int list[] = new int[getnNodes()];
        int index = 0;
        int lastIndex = 1;
        list[0] = 0;
        visited[0] = true;
        while (index < lastIndex) {
            int currentNode =list[index];
            for (int i=1; i<parentMatrix[currentNode][0]; i++) {   // verify parents of current node
                if (visited[parentMatrix[currentNode][i]] == false) {
                    list[lastIndex] = parentMatrix[currentNode][i];
                    visited[parentMatrix[currentNode][i]] = true;
                    lastIndex++;
                }
            }
            for (int i=1; i<sonMatrix[currentNode][0]; i++) {   // verify sons of current node
                if (visited[sonMatrix[currentNode][i]] == false) {
                    list[lastIndex] = sonMatrix[currentNode][i];
                    visited[sonMatrix[currentNode][i]] = true;
                    lastIndex++;
                }
            }
            index++;
        }
        for (int k=0; (k<visited.length); k++)   // verify whether all nodes were visited
            if (visited[k] == false)	{
            //System.out.println("\tisConnected:false");
            return(false);
            }
        //System.out.println("\tisConnected:true");
        return(true);
    }
    
    public boolean isAcyclic() {  // return true if the graph is still acyclic after last arc added
        //ATTENTION! This method works after adding an arc, not after removing an arc
        boolean visited[] = new boolean[getnNodes()];
        boolean noCycle = true;
        int list[] = new int[getnNodes()+1];
        int index = 0;
        int lastIndex = 1;
        list[0] = randP;
        visited[randP] = true;
        while (index < lastIndex && noCycle==true) {
            int currentNode = list[index];
            int i=1;
            while ((i<parentMatrix[currentNode][0]) && noCycle==true ) {   // verify parents of current node
                if (visited[parentMatrix[currentNode][i]] == false) {
                    if (parentMatrix[currentNode][i] != randS ) {
                        list[lastIndex] = parentMatrix[currentNode][i];
                        lastIndex++;
                    } else noCycle=false;
                    visited[parentMatrix[currentNode][i]] = true;
                } // end of if(visited)
                i++;
            }
            index++;
        }
        //System.out.println("\tnoCycle:"+noCycle);
        return(noCycle);
    }
    
    void ckeckingCycle(int nGra,int nIterati,int maxVal,String te,String fo,String baseFileNam,int maxInducedWidt) throws Exception{
        int semente=1;
        String fileName = "SementeProcuradaIW3";
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        for (semente=1;semente<10000;semente++){
            iwDistribution= new int[100];
            rand=new MersenneTwister(semente);
            randSampleArc=new MersenneTwister(semente);
            inicializeGraph(); // Inicialize a simple ordered tree as a BN structure
            generateMixedEJ(nGra,nIterati,maxVal,te,fo,baseFileNam,maxInducedWidt);
            if  (iwDistribution[3]<900)  {
                System.out.println("A semente geradora de ciclo �:"+semente);
                outputFile.println("A semente procurada geradora de ciclo �:"+semente);
                throw(new Exception("Cycle at Markov chain was found !!!"));
            }
        }
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Methods that implement operations /////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    public void inicializeGraph(){		// initialize a simple ordered tree
        for (int i=0;i<getnNodes();i++ ) {
            for (int j=1;j<getMaxDegree()+1;j++ ) {
                parentMatrix[i][j]=-5; //set first node
                sonMatrix[i][j]=-5;
            }
        }
        parentMatrix[0][0]=1; //set first node
        sonMatrix[0][0]=2;    //set first node
        sonMatrix[0][1]=1;    //set first node
        parentMatrix[getnNodes()-1][0]=2;  //set last node
        parentMatrix[getnNodes()-1][1]=getnNodes()-2;  //set last node
        sonMatrix[getnNodes()-1][0]=1;     //set last node
        for (int i=1;i<(getnNodes()-1);i++)  {  // set the other nodes
            parentMatrix[i][0]=2;
            parentMatrix[i][1]=i-1;
            sonMatrix[i][0]=2;
            sonMatrix[i][1]=i+1;
        }
    }
    
    private void sampleArc()	{  //  sample a uniform pair of arcs
        int rand=randSampleArc.nextInt(getnNodes()*(getnNodes()-1));
        randP=rand/(getnNodes()-1);
        int rest = rand-randP*(getnNodes()-1);
        if (rest >= randP )	randS=rest+1;
        else randS=rest;
        
    }
    
    private void addArc()  {  // add randP and randS in the Graph
        sonMatrix[randP][sonMatrix[randP][0]]=randS;
        sonMatrix[randP][0]++;
        parentMatrix[randS][parentMatrix[randS][0]]=randP;
        parentMatrix[randS][0]++;
    }
    private void addArc(int rnP, int rnS)  {  // add rnP and rnS in the Graph
        sonMatrix[rnP][sonMatrix[rnP][0]]=rnS;
        sonMatrix[rnP][0]++;
        parentMatrix[rnS][parentMatrix[rnS][0]]=rnP;
        parentMatrix[rnS][0]++;
        randP=rnP;  // it is important to reset for the cycle test!
        randS=rnS;
    }
    private void removeArc()  throws Exception{  // remove randP and randS in the Graph
        boolean go=true;
        int lastNode=-1;
        int proxNode=-1;
        int atualNode=-1;
        if ( (parentMatrix[randS][0]!=1) && (sonMatrix[randP][0]!=1) ) {
            lastNode=parentMatrix[randS][parentMatrix[randS][0]-1];
            for (int i=(parentMatrix[randS][0]-1);(i>0 && go);i--)	{ // remove element from parentMatrix
                atualNode=parentMatrix[randS][i];
                if (atualNode!=randP)	{
                    proxNode=atualNode;
                    parentMatrix[randS][i]=lastNode;
                    lastNode=proxNode;
                } else {
                    parentMatrix[randS][i]=lastNode;
                    go=false;
                }
            }
            if ( (sonMatrix[randP][0]!=1) && (sonMatrix[randP][0]!=1) ) {
                lastNode=sonMatrix[randP][sonMatrix[randP][0]-1];
                go=true;
                for (int i=(sonMatrix[randP][0]-1);(i>0 && go);i--)	{ // remove element from sonMatrix
                    atualNode=sonMatrix[randP][i];
                    if (atualNode!=randS)	{
                        proxNode=atualNode;
                        sonMatrix[randP][i]=lastNode;
                        lastNode=proxNode;
                    } else {
                        sonMatrix[randP][i]=lastNode;
                        go=false;
                    }
                } // end of for
            }
            sonMatrix[randP][(sonMatrix[randP][0]-1)]=-4;
            sonMatrix[randP][0]--;
            parentMatrix[randS][(parentMatrix[randS][0]-1)]=-4;
            parentMatrix[randS][0]--;
        }
        /*else    {
            int sai=10;
            String fileName = "SementeProcurada_encontradoDuranteRemocao";
            PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
            printArcs();
            System.out.print("\trandP:"+randP);
            System.out.print("\trandS:"+randS);
            System.out.print("\tCan�t be moved!");
            outputFile.println("\trandP:"+randP);
            outputFile.println("\trandP:"+randP);
            outputFile.println("A semente procurada geradora de ciclo �:"+semente);
            throw(new Exception("Error removing!!!"));
         
            //pause();
         
         }
         */
    }
    private void removeArc(int rnP, int rnS)  {  // add rnP and rnS in the Graph
        boolean go=true;
        int lastNode=-1;
        int proxNode=-1;
        int atualNode=-1;
        if ( (parentMatrix[rnS][0]!=1) && (sonMatrix[rnP][0]!=1) ) {
            lastNode=parentMatrix[rnS][parentMatrix[rnS][0]-1];
            for (int i=(parentMatrix[rnS][0]-1);(i>0 && go);i--)	{ // remove element from parentMatrix
                atualNode=parentMatrix[rnS][i];
                if (atualNode!=rnP)	{
                    proxNode=atualNode;
                    parentMatrix[rnS][i]=lastNode;
                    lastNode=proxNode;
                } else {
                    parentMatrix[rnS][i]=lastNode;
                    go=false;
                }
            }
            if ( (sonMatrix[rnP][0]!=1) && (sonMatrix[rnP][0]!=1) ) {
                lastNode=sonMatrix[rnP][sonMatrix[rnP][0]-1];
                go=true;
                for (int i=(sonMatrix[rnP][0]-1);(i>0 && go);i--)	{ // remove element from sonMatrix
                    atualNode=sonMatrix[rnP][i];
                    if (atualNode!=rnS)	{
                        proxNode=atualNode;
                        sonMatrix[rnP][i]=lastNode;
                        lastNode=proxNode;
                    } else {
                        sonMatrix[rnP][i]=lastNode;
                        go=false;
                    }
                } // end of for
            }
            sonMatrix[rnP][(sonMatrix[rnP][0]-1)]=-4;
            sonMatrix[rnP][0]--;
            parentMatrix[rnS][(parentMatrix[rnS][0]-1)]=-4;
            parentMatrix[rnS][0]--;
        } else System.out.print("Can�t be moved!");
    }
    
    public int[] findArcToRemove(int queried, int son)	{
        boolean visited[] = new boolean[getnNodes()];
        boolean found=false;
        int list[] = new int[getnNodes()];
        int removeArc[] = new int[2];
        
        for (int j=1; j<parentMatrix[son][0] && found==false; j++)	{ // search queried node between parents nodes
            int index = 0;
            int lastIndex = 1;
            list[0] = parentMatrix[son][j];
            visited[list[0]] = true;
            visited[son] = true;
            while (index<lastIndex) {
                int currentNode =list[index];
                for (int i=1; i<parentMatrix[currentNode][0]; i++) {   // verify parents of current node
                    int currentParent=parentMatrix[currentNode][i];
                    if (visited[currentParent] == false) {
                        list[lastIndex] = currentParent;
                        visited[currentParent] = true;
                        lastIndex++;
                    }
                }
                for (int i=1; i<sonMatrix[currentNode][0]; i++) {   // verify sons of current node
                    if (visited[sonMatrix[currentNode][i]] == false) {
                        list[lastIndex] = sonMatrix[currentNode][i];
                        visited[sonMatrix[currentNode][i]] = true;
                        lastIndex++;
                    }
                }
                index++;
            } // end of while
            if (visited[queried] == true)	{
                found=true;  // verify if queried node was visited
                removeArc[0]=parentMatrix[son][j];
                removeArc[1]=son;
            }  // end of if
        } // end of for
        
        for (int j=1; j<sonMatrix[son][0] && found==false; j++)	{// search queried node between son nodes
            int index = 0;
            int lastIndex = 1;
            list[0] = sonMatrix[son][j];
            visited[list[0]] = true;
            visited[son] = true;
            while (index<lastIndex) {
                int currentNode =list[index];
                for (int i=1; i<parentMatrix[currentNode][0]; i++) {   // verify parents of current node
                    int currentParent=parentMatrix[currentNode][i];
                    if (visited[currentParent] == false) {
                        list[lastIndex] = currentParent;
                        visited[currentParent] = true;
                        lastIndex++;
                    }
                }
                for (int i=1; i<sonMatrix[currentNode][0]; i++) {   // verify sons of current node
                    if (visited[sonMatrix[currentNode][i]] == false) {
                        list[lastIndex] = sonMatrix[currentNode][i];
                        visited[sonMatrix[currentNode][i]] = true;
                        lastIndex++;
                    }
                }
                index++;
            } // end of while
            if (visited[queried] == true)	{
                found=true;  // verify if queried node was visited
                removeArc[1]=sonMatrix[son][j];
                removeArc[0]=son;
            } // end of if
        } // end of for
        return(removeArc);
    } // end of finArcToRemove
    
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Methods to save and input probabilities of Bayesian networks ///////
    /////////////////////////////////////////////////////////////////////////////////
    private void saveBNXMLJava(String name,String format,int n, int maxValues) throws Exception {
        // this method is a optional to save Bayesian network in XML and java format at the same time
        String fileName = name.concat("" + (n+1) + ".xml");
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        String fileName2 = name.concat("" + (n+1) + ".java");
        PrintStream outputFile2 = new PrintStream(new FileOutputStream(fileName2), true);
        
        outputFile.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFile.println("<!--");
        outputFile.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFile.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFile.println("\tNumber of nodes:" + getnNodes());
        outputFile.println("\tOutput created " + (new Date()));
        outputFile.println("-->\n\n\n");
        outputFile.println("<BIF VERSION=\"0.3\">");
        outputFile.println("<NETWORK>");
        String correctedName=name.concat("" + (n+1) ); 
		outputFile.println("<NAME>InternalNetwork</NAME>");
		//if (name != null)  outputFile.println("<NAME>" + correctedName + "</NAME>");
        
        outputFile2.println("");
        outputFile2.println("/*");
        outputFile2.println(" * "+correctedName+".java");
        outputFile2.println(" * Number of nodes:" + getnNodes());
        outputFile2.println(" * This network was created on " + (new Date()));
        outputFile2.println();
        outputFile2.println(" * @author : BNGenerator (Random and uniform DAG generator)");
        outputFile2.println(" *            and Jaime Ide (for function generation)");
        outputFile2.println(" */");
        outputFile2.println("import embayes.data.*;");
        outputFile2.println();
        outputFile2.println("class "+correctedName+" {");
        outputFile2.println();
        outputFile2.println("  private BayesNet network;");
        outputFile2.println("  public BayesNet getNetwork() { return(network); }");
        outputFile2.println();
        outputFile2.println("  public "+correctedName+"() {");
        outputFile2.println();
        outputFile2.println("\tDataFactory factory =");
        outputFile2.println("\t\t\t embayes.data.impl.DataBasicFactory.getInstance();");
        outputFile2.println("\tnetwork = factory.newBayesNet();");
        outputFile2.println("\tnetwork.setName(\""+correctedName+"\");");
        outputFile2.println();
        
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            outputFile.println("<!-- Variables -->");
            outputFile.println("<VARIABLE TYPE=\"nature\">");
            outputFile.println("    <NAME>node"+ j + "</NAME>");
            outputFile2.println("\tCategoricalVariable n"+j+" =");
            outputFile2.print("\t\tfactory.newCategoricalVariable(\"n"+j+"\", new String[] {");
            
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            for (int i=0;i<(states);i++) {
                outputFile.println("    <OUTCOME>state"+i+"</OUTCOME>");
                outputFile2.print("\"state"+i+"\"");
                if (i<(states-1)) outputFile2.print(",");
                countState++;
            }
            outputFile2.println("});");
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            outputFile.println("    <PROPERTY>position = ("+x+", "+y+")</PROPERTY>");
            outputFile.println("</VARIABLE>");
            contaEmX[nP]++;
        } //end of for
        
        outputFile.println("<!-- Probability Distributions -->");
        for (int i=0;i<getnNodes();i++ ) {
            int nParents=1;
            outputFile.println(" <DEFINITION>");
            outputFile.println("      <FOR>node"+i+"</FOR>");
            outputFile2.println("\tCategoricalProbability p"+i+" =");
            outputFile2.println("\t\tfactory.newCategoricalProbability(n"+i+",");
            
            if (parentMatrix[i][0]!=1)  {
                outputFile2.print("\t\tnew CategoricalVariable[] {");
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    outputFile.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    outputFile2.print("n"+parentMatrix[i][j]);
                    if (j<(aux-1))   outputFile2.print(",");
                    nParents=nParents*numberStates[parentMatrix[i][j]];
                }
                outputFile2.println("},");
            }  // end of if
            inputTableXMLJava(outputFile,outputFile2,numberStates[i],nParents);
            outputFile.println(" </DEFINITION>");
        } // end of for
        
        // End net description
        outputFile.println("</NETWORK>");
        outputFile.println("</BIF>");
        
        // create set of variables:
        outputFile2.println("network.setVariables( new CategoricalVariable[] {");
        for (int i=0;i<getnNodes();i++ ) {
            outputFile2.print("n"+i);
            if (i<(getnNodes()-1))  outputFile2.print(",");
        } // end of for
        outputFile2.println("});");
        // create set of probabilities:
        outputFile2.println("network.setProbabilities( new CategoricalProbability[] {");
        for (int i=0;i<getnNodes();i++ ) {
            outputFile2.print("p"+i);
            if (i<(getnNodes()-1))  outputFile2.print(",");
        } // end of for
        outputFile2.println("});");
        outputFile2.println("  } // end of public");
        outputFile2.println("}  // end of class");
        
    } // end of class saveBNa
    
    /*
    public void sampleNumberStates(int maxValues)  {
        //int nStates[] = new int[nNodes];
        int max=2;
        for (int j=0;j<nNodes;j++) {
            max = calculateNumberOfStates(maxValues,fixed_nVal);
            nStates[j]=max;
        }
    }
     */
    
    private void saveBNJava(String name,String format,int n, int maxValues) throws Exception { // Save BN in BIF format
        String fileName2 = name.concat("" + (n+1) + ".java");
        PrintStream outputFile2 = new PrintStream(new FileOutputStream(fileName2), true);
        String correctedName=name.concat("" + (n+1) );
        outputFile2.println("");
        outputFile2.println("/*");
        outputFile2.println(" * "+correctedName+".java");
        outputFile2.println(" * Number of nodes:" + getnNodes());
        outputFile2.println(" * This network was created on " + (new Date()));
        outputFile2.println();
        outputFile2.println(" * @author : BNGenerator (Random and uniform DAG generator)");
        outputFile2.println(" *            and Jaime Ide (for function generation)");
        outputFile2.println(" */");
        outputFile2.println("import embayes.data.*;");
        outputFile2.println();
        outputFile2.println("class "+correctedName+" {");
        outputFile2.println();
        outputFile2.println("  private BayesNet network;");
        outputFile2.println("  public BayesNet getNetwork() { return(network); }");
        outputFile2.println();
        outputFile2.println("  public "+correctedName+"() {");
        outputFile2.println();
        outputFile2.println("\tDataFactory factory =");
        outputFile2.println("\t\t\t embayes.data.impl.DataBasicFactory.getInstance();");
        outputFile2.println("\tnetwork = factory.newBayesNet();");
        outputFile2.println("\tnetwork.setName(\""+correctedName+"\");");
        outputFile2.println();
        
        int max=2;
        int numberStates[] = new int[getnNodes()];
        
        // set variables
        for (int j=0;j<getnNodes();j++) {
            outputFile2.println("\tCategoricalVariable n"+j+" =");
            outputFile2.print("\t\tfactory.newCategoricalVariable(\"n"+j+"\", new String[] {");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            for (int i=0;i<(states);i++) {
                outputFile2.print("\"state"+i+"\"");
                if (i<(states-1)) outputFile2.print(",");
                countState++;
            }
            outputFile2.println("});");
            numberStates[j]=countState;
        } //end of for
        
        // set probabilities
        for (int i=0;i<getnNodes();i++ ) {
            int nParents=1;
            outputFile2.println("\tCategoricalProbability p"+i+" =");
            outputFile2.println("\t\tfactory.newCategoricalProbability(n"+i+",");
            
            if (parentMatrix[i][0]!=1)  {
                outputFile2.print("\t\tnew CategoricalVariable[] {");
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    outputFile2.print("n"+parentMatrix[i][j]);
                    if (j<(aux-1))   outputFile2.print(",");
                    nParents=nParents*numberStates[parentMatrix[i][j]];
                }
                outputFile2.println("},");
            }  // end of if
            inputTableJava(outputFile2,numberStates[i],nParents);
        } // end of for
        
        
        // create set of variables:
        outputFile2.println("network.setVariables( new CategoricalVariable[] {");
        for (int i=0;i<getnNodes();i++ ) {
            outputFile2.print("n"+i);
            if (i<(getnNodes()-1))  outputFile2.print(",");
        } // end of for
        outputFile2.println("});");
        
        // create set of probabilities:
        outputFile2.println("network.setProbabilities( new CategoricalProbability[] {");
        for (int i=0;i<getnNodes();i++ ) {
            outputFile2.print("p"+i);
            if (i<(getnNodes()-1))  outputFile2.print(",");
        } // end of for
        outputFile2.println("});");
        outputFile2.println("  } // end of public");
        outputFile2.println("}  // end of class");
        
    } // end of class saveBNJava
    

    public String toDag() { 
        ArrayList<String> arcs = new ArrayList<>();
        for (int i=0;i<getnNodes();i++ ) {
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    arcs.add("("+ parentMatrix[i][j]+","+i+")");
                }
            }  // end of if
        }
        return String.join(",", arcs);
    }

    private void saveDAG(String name,String format,int n, int maxValues, int inducedWidth, int maxDegree, int maxInDegree, int maxOutDegree, int maxArcs) throws Exception { // Save BN in XML format
        String fileName = name.concat("" + (n+1) + "." + format);
        ArrayList<String> arcs = new ArrayList<>();
        for (int i=0;i<getnNodes();i++ ) {
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    arcs.add("("+ parentMatrix[i][j]+","+i+")");
                }
            }  // end of if
        }
        
      //  cacheDAG(String.join(",", arcs));
    } 
    

    private void saveBNXML(String name,String format,int n, int maxValues) throws Exception { // Save BN in XML format
        String fileName = name.concat("" + (n+1) + "." + format);
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        
        outputFile.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFile.println("<!--");
        outputFile.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFile.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFile.println("\tNumber of nodes:" + getnNodes());
        outputFile.println("\tOutput created " + (new Date()));
        outputFile.println("-->\n\n\n");
        outputFile.println("<BIF VERSION=\"0.3\">");
        outputFile.println("<NETWORK>");
        String correctedName=name.concat("" + (n+1) );
        outputFile.println("<NAME>InternalNetwork</NAME>");
		//if (name != null)  outputFile.println("<NAME>" + correctedName + "</NAME>");
        
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            outputFile.println("<!-- Variables -->");
            outputFile.println("<VARIABLE TYPE=\"nature\">");
            outputFile.println("    <NAME>node"+ j + "</NAME>");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            for (int i=0;i<(states);i++) {
                outputFile.println("    <OUTCOME>state"+i+"</OUTCOME>");
                countState++;
            }
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFile.println("</VARIABLE>");
            contaEmX[nP]++;
        } //end of for
        
        outputFile.println("<!-- Probability Distributions -->");
        for (int i=0;i<getnNodes();i++ ) {
            int nParents=1;
            outputFile.println(" <DEFINITION>");
            outputFile.println("      <FOR>node"+i+"</FOR>");
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    outputFile.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    nParents=nParents*numberStates[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
                }
            }  // end of if
            inputTableXML(outputFile,numberStates[i],nParents);
            //inputTableXML(outputFile,nStates[i],nParents);
            outputFile.println(" </DEFINITION>");
        } // end of for
        // End net description
        outputFile.println("</NETWORK>");
        outputFile.println("</BIF>");
        
    } // end of class saveBNXML
    
	private void saveBNXML_fromFile(String name,String format,int n, int[] nStates) throws Exception { // Save BN in XML format
        String fileName = name.concat("_" + (n+1) + "." + format);
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        
        outputFile.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFile.println("<!--");
        outputFile.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFile.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFile.println("\tNumber of nodes:" + getnNodes());
        outputFile.println("\tOutput created " + (new Date()));
        outputFile.println("-->\n\n\n");
        outputFile.println("<BIF VERSION=\"0.3\">");
        outputFile.println("<NETWORK>");
        String correctedName=name.concat("" + (n+1) );
        outputFile.println("<NAME>InternalNetwork</NAME>");
		//if (name != null)  outputFile.println("<NAME>" + correctedName + "</NAME>");
        
		int max=2;
        double x,y;
        setMaxDegree(nStates.length);
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        setnNodes(nStates.length);
        int numberStates[] = new int[getnNodes()];

        for (int j=0;j<getnNodes();j++) {
            outputFile.println("<!-- Variables -->");
            outputFile.println("<VARIABLE TYPE=\"nature\">");
            outputFile.println("    <NAME>node"+ j + "</NAME>");
            //max = calculateNumberOfStates(maxValues,fixed_nValue);
            int countState=0; // count the number of states
            //int states=Math.max(2,max);
            int states=nStates[j];
            for (int i=0;i<(states);i++) {
                outputFile.println("    <OUTCOME>state"+i+"</OUTCOME>");
                countState++;
            }
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFile.println("</VARIABLE>");
            contaEmX[nP]++;
        } //end of for
        
        outputFile.println("<!-- Probability Distributions -->");
        for (int i=0;i<getnNodes();i++ ) {
            int nParents=1;
            outputFile.println(" <DEFINITION>");
            outputFile.println("      <FOR>node"+i+"</FOR>");
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    outputFile.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    nParents=nParents*numberStates[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
                }
            }  // end of if
            inputTableXML(outputFile,numberStates[i],nParents);
            //inputTableXML(outputFile,nStates[i],nParents);
            outputFile.println(" </DEFINITION>");
        } // end of for
        // End net description
        outputFile.println("</NETWORK>");
        outputFile.println("</BIF>");
        
    } // end of class saveBNXML_fromFile
    

        /**
     * Save a credal network in the Credal InterchangeFormat (.cif). 
	 * This file contains just the linear "base" point probabilities of the local credal set
     */
    public void toArcList(String name,String format,int n, int maxValues) throws Exception {

        // to save in interval cif format
        String fileName1 = name.concat("_" + (n+1) + ".cif");

        // SET number of points;
        int nTables=getnPointProb();
        
                
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        String correctedName=name.concat("" + (n+1) );
        
        for (int j=0;j<getnNodes();j++) {
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
  
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            contaEmX[nP]++;
        } //end of for
        
        //****************** INPUTING TABLES**************
        List<String> sb = new ArrayList<>();

        for (int i=0; i<getnNodes(); i++)  {
            int nParents=1; // count number of parents
           
            for (int j=1; j<parentMatrix[i][0]; j++) {
                sb.add("(" + i + "," + parentMatrix[i][j] + ")");
            }
          
        } // end of for i(Node)
        PrintWriter out = new PrintWriter(fileName1);
        out.write(String.join(",", sb));
        out.close();
    }
    

    /**
     * Save a credal network in the Credal InterchangeFormat (.cif). 
	 * This file contains just the linear "base" point probabilities of the local credal set
     */
    public void saveCIF(String name,String format,int n, int maxValues) throws Exception {
        // SET number of points;
        int nTables=getnPointProb();
        
        // to save in interval cif format
        String fileName1 = name.concat("_" + (n+1) + ".cif");
        PrintStream outputFile1 = new PrintStream(new FileOutputStream(fileName1), true);
        
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        // Saving in Interval Cif Format
        outputFile1.println("// \tCredal network in CIF(CredalNet Interchange Format)");
        outputFile1.println("// \tProduced by BNGenerator (http://www.pmr.poli.usp.br/ltd/Software/BNGenerator");
        outputFile1.println("// \tNumber of nodes:" + getnNodes());
        outputFile1.println("// \tOutput created " + (new Date()));
        //outputFile1.println("-->\n\n\n");
        String correctedName=name.concat("" + (n+1) );
        if (name != null)  {
            outputFile1.println("network \"" + correctedName + "\"\t{");
            outputFile1.println("}");
        }
        
        for (int j=0;j<getnNodes();j++) {
            outputFile1.println("variable  \"node"+ j + "\"\t{");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            outputFile1.print("\ttype discrete["+states+"]  {");
            for (int i=0;i<(states);i++) {
                outputFile1.print(" \"state"+i+"\" ");
                countState++;
            }
            outputFile1.println("};");
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile1.println("\tproperty \"position = ("+xx+", "+yy+") \";");
            outputFile1.println("}");
            contaEmX[nP]++;
        } //end of for
        
        //****************** INPUTING TABLES**************
        
        for (int i=0; i<getnNodes(); i++)  {
            int nParents=1; // count number of parents
            ////////////////////////// inputing tables of Interval Cif
            outputFile1.print("probability ( \"node"+i+"\" ");
            for (int j=1; j<parentMatrix[i][0]; j++) {
                outputFile1.print( " \"node" + parentMatrix[i][j] + "\" ");
                nParents=nParents*numberStates[parentMatrix[i][j]];
            }
            outputFile1.print(") {");
            outputFile1.println(" //" +  (parentMatrix[i][0]) +
                    " variable(s) and " + nParents+ " values");
            for (int contV=0;contV<nTables;contV++)  {
                outputFile1.println("\ttable ");
                // create lower and upper tables
                //float table[]=df.generateDistributionFunction(numberStates[i],nParents,"emFloat");
                // added on 04 april 2006 (*)
				float table[]=df.generateDistributionFunctionInterval(numberStates[i],nParents,"emFloat",getLowerP(),getUpperP());
				//System.out.println("TABLE:"); //(*)
				//for (int xx=0;xx<table.length ;xx++ )	System.out.println(""+table[xx]); //(*)
				float transposed_table[]=transTable(table,numberStates[i],nParents);
                for (int k=0; k<table.length; k++)  outputFile1.print("\t" + transposed_table[k]);
                outputFile1.println(";");
            }
            outputFile1.println("}");
        } // end of for i(Node)
    }
    
    /**
     * Save a credal network in the Credal InterchangeFormat for Cassio�s format (_.cif).
     */
    public void saveCIF_fromFile(String name,int n, int nStates[],int nT) throws Exception {
        // SET number of vertexes;
        int nTables=nT;
        // to save in interval cif format
        String fileName1 = name.concat("_" + (n+1) + ".cif");
        PrintStream outputFile1 = new PrintStream(new FileOutputStream(fileName1), true);
        
        int max=2;
        double x,y;
        setMaxDegree(nStates.length);
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        setnNodes(nStates.length);
        int numberStates[] = new int[getnNodes()];
        
        // Saving in Interval Cif Format
        outputFile1.println("// \tCredal network in CIF(CredalNet Interchange Format)");
        outputFile1.println("// \tProduced by BNGenerator (http://www.pmr.poli.usp.br/ltd/Software/BNGenerator");
        outputFile1.println("// \tNumber of nodes:" + getnNodes());
        outputFile1.println("// \tOutput created " + (new Date()));
        //outputFile1.println("-->\n\n\n");
        String correctedName=name.concat("" + (n+1) );
        if (name != null)  {
            outputFile1.println("network \"" + correctedName + "\"\t{");
            outputFile1.println("}");
        }
        
        for (int j=0;j<getnNodes();j++) {
            outputFile1.println("variable  \"node"+ j + "\"\t{");
            int countState=0; // count the number of states
            int states=nStates[j];
            //int states=nStates[j];
            outputFile1.print("\ttype discrete["+states+"]  {");
            for (int i=0;i<(states);i++) {
                outputFile1.print(" \"state"+i+"\" ");
                countState++;
            }
            outputFile1.println("};");
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile1.println("\tproperty \"position = ("+xx+", "+yy+") \";");
            outputFile1.println("}");
            contaEmX[nP]++;
        } //end of for
        
        //****************** INPUTING TABLES**************
        
        for (int i=0; i<getnNodes(); i++)  {
            int nParents=1; // count number of parents
            ////////////////////////// inputing tables of Interval Cif
            outputFile1.print("probability ( \"node"+i+"\" ");
            for (int j=1; j<parentMatrix[i][0]; j++) {
                outputFile1.print( " \"node" + parentMatrix[i][j] + "\" ");
                nParents=nParents*numberStates[parentMatrix[i][j]];
            }
            outputFile1.print(") {");
            outputFile1.println(" //" +  (parentMatrix[i][0]) +
                    " variable(s) and " + nParents+ " values");
            for (int contV=0;contV<nTables;contV++)  {
                outputFile1.println("\ttable ");
                // create lower and upper tables
                //float table[]=df.generateDistributionFunction(numberStates[i],nParents,"emFloat");
				float table[]=df.generateDistributionFunction2dig(numberStates[i],nParents,"emFloat");
                float transposed_table[]=transTable(table,numberStates[i],nParents);
                boolean zero=false;
                boolean negative=false;
                for (int k=0; k<table.length; k++)  {
                    outputFile1.print("\t" + transposed_table[k]);
                    if (transposed_table[k]==0) zero=true;
                    if (transposed_table[k]<0) negative=true;
                }
                if (zero)   outputFile1.println(";  // Table has zero probability!");
                else outputFile1.println(";");
                if (negative) outputFile1.print("// Have negative probability!!!");
            }
            outputFile1.println("}");
            
            
        } // end of for i(Node)
        
    }
    
    
    
    /**
     * Save a binary credal network in the BIF InterchangeFormat for Cassio�s format (_.cif)
     * and Jaime�s format (Lower.xml and Upper.xml).
     */
    public void saveCIF_interval(String name,String format,int n, int maxValues) throws Exception {
        
        // to save in interval cif format
        String fileName1 = name.concat("_" + (n+1) + ".cif");
        PrintStream outputFile1 = new PrintStream(new FileOutputStream(fileName1), true);
        
        // to save in lower and upper networks
        String fileNameXML = name.concat("_" + (n+1) + "Lower.xml");
        PrintStream outputFileXML = new PrintStream(new FileOutputStream(fileNameXML), true);
        String fileNameXML2 = name.concat("_" + (n+1) + "Upper.xml");
        PrintStream outputFileXML2 = new PrintStream(new FileOutputStream(fileNameXML2), true);
        
        String correctedName=name.concat("" + (n+1) );
        
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        // Saving in Interval Cif Format
        outputFile1.println("// \tCredal network in CIF(CredalNet Interchange Format)");
        outputFile1.println("// \tProduced by BNGenerator (http://www.pmr.poli.usp.br/ltd/Software/BNGenerator");
        outputFile1.println("// \tNumber of nodes:" + getnNodes());
        outputFile1.println("// \tOutput created " + (new Date()));
        //outputFile1.println("-->\n\n\n");
        correctedName=name.concat("" + (n+1) );
        if (name != null)  {
            outputFile1.println("network \"" + correctedName + "\"\t{");
            outputFile1.println("}");
        }
        max=2;
        nDegree=getMaxDegree()+1;
        int contaEmX_1[]= new int[nDegree];
        int numberStates_1[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            outputFile1.println("variable  \"node"+ j + "\"\t{");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            outputFile1.print("\ttype discrete["+states+"]  {");
            for (int i=0;i<(states);i++) {
                outputFile1.print(" \"state"+i+"\" ");
                countState++;
            }
            outputFile1.println("};");
            numberStates_1[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX_1[nP]);
            x=30+120*contaEmX_1[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile1.println("\tproperty \"position = ("+xx+", "+yy+") \";");
            outputFile1.println("}");
            contaEmX_1[nP]++;
        } //end of for
        
        // Saving in Interval Cif Format
        outputFileXML.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML.println("<!--");
        outputFileXML.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML.println("\tNumber of nodes:" + getnNodes());
        outputFileXML.println("\tOutput created " + (new Date()));
        outputFileXML.println("-->\n\n\n");
        outputFileXML.println("<BIF VERSION=\"0.3\">");
        outputFileXML.println("<NETWORK>");
        //if (name != null)  outputFileXML.println("<NAME>" + correctedName + "</NAME>");
        outputFileXML.println("<NAME>InternalNetwork</NAME>");
		// high
        outputFileXML2.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML2.println("<!--");
        outputFileXML2.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML2.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML2.println("\tNumber of nodes:" + getnNodes());
        outputFileXML2.println("\tOutput created " + (new Date()));
        outputFileXML2.println("-->\n\n\n");
        outputFileXML2.println("<BIF VERSION=\"0.3\">");
        outputFileXML2.println("<NETWORK>");
        if (name != null)  outputFileXML2.println("<NAME>" + correctedName + "</NAME>");
        
        max=2;
        nDegree=getMaxDegree()+1;
        int contaEmXXML[]= new int[nDegree];
        int numberStatesXML[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            // low
            outputFileXML.println("<!-- Variables -->");
            outputFileXML.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML.println("    <NAME>node"+ j + "</NAME>");
            // high
            outputFileXML2.println("<!-- Variables -->");
            outputFileXML2.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML2.println("    <NAME>node"+ j + "</NAME>");
            
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            for (int i=0;i<(states);i++) {
                // low
                outputFileXML.println("    <OUTCOME>state"+i+"</OUTCOME>");
                // high
                outputFileXML2.println("    <OUTCOME>state"+i+"</OUTCOME>");
                countState++;
            }
            numberStatesXML[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmXXML[nP]);
            x=30+120*contaEmXXML[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            // low
            outputFileXML.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML.println("</VARIABLE>");
            // high
            outputFileXML2.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML2.println("</VARIABLE>");
            contaEmXXML[nP]++;
        } //end of for
        
        //****************** INPUTING TABLES**************
        
        for (int i=0; i<getnNodes(); i++)  {
            //CategoricalVariable var[]=probabilitiesLower[i].getVariables();
            int valuesLength=(int)Math.pow(2,parentMatrix[i][0]);
            int nTable=(int)Math.pow(2,(valuesLength/2));
            float values[][]= new float[nTable][valuesLength];
            
            // create lower and upper tables
            float table1[]=df.generateDistributionFunction(2,(valuesLength/2),"emFloat");
            float table2[]=df.generateDistributionFunction(2,(valuesLength/2),"emFloat");
            // modified to generate with interval (March 2006)
			//float table1[]=df.generateDistributionFunctionInterval(2,(valuesLength/2),"emFloat",lowerP,upperP);
			//System.out.println("TABLE:");
			//for (int xx=0;xx<table1.length ;xx++ )	System.out.println(""+table1[xx]);
			//float table2[]=df.generateDistributionFunctionInterval(2,(valuesLength/2),"emFloat",lowerP,upperP);
            
            float lowTable[]= new float[valuesLength];
            float highTable[]= new float[valuesLength];
            
            int auxZ=0;
            for (int z=0;z<(valuesLength)/2;z++)    {
                if (table1[2*z]<table2[2*z])	{
                    lowTable[auxZ]=table1[2*z];
                    lowTable[auxZ+(valuesLength/2)]=table1[2*z+1];
                    highTable[auxZ]=table2[2*z];
                    highTable[auxZ+(valuesLength/2)]=table2[2*z+1];
                    auxZ++;
                } else {
                    lowTable[auxZ]=table2[2*z];
                    lowTable[auxZ+(valuesLength/2)]=table2[2*z+1];
                    highTable[auxZ]=table1[2*z];
                    highTable[auxZ+(valuesLength/2)]=table1[2*z+1];
                    auxZ++;
                }
            }
            
            for (int j=0;j<(valuesLength)/2;j++)    {
                boolean isLow=true;
                int cont=0;
                int base=(int)Math.pow(2,j);
                for (int k=0;k<nTable;k++)  {
                    if (cont==base) {
                        cont=0;
                        if (isLow) isLow=false;
                        else isLow=true;
                    }
                    if (isLow) {
                        values[k][j]=lowTable[j];
                        values[k][j+(valuesLength)/2]=lowTable[j+(valuesLength)/2];
                    } else    {
                        values[k][j]=highTable[j];
                        values[k][j+(valuesLength)/2]=highTable[j+(valuesLength)/2];
                    }
                    cont++;
                    
                }
            }
            ////////////////////////// inputing tables of Interval Cif
            outputFile1.print("probability ( \"node"+i+"\" ");
            for (int j=1; j<parentMatrix[i][0]; j++) {
                outputFile1.print( " \"node" + parentMatrix[i][j] + "\" ");
            }
            outputFile1.print(") {");
            outputFile1.println(" //" +  (parentMatrix[i][0]+1) +
                    " variable(s) and " + valuesLength + " values");
            
            outputFile1.println("\ttable ");
            for (int k=0; k<valuesLength; k++)  outputFile1.print("\t" + lowTable[k]);
            outputFile1.println(";");
            outputFile1.println("\ttable ");
            for (int k=0; k<valuesLength; k++)  outputFile1.print("\t" + highTable[k]);
            outputFile1.println(";");
            
            outputFile1.println("}");
            
            ///////////// Inputing Tables Interval
            // low
            outputFileXML.println("<!-- Probability Distributions -->");
            // high
            outputFileXML2.println("<!-- Probability Distributions -->");
            //for (int i=0;i<nNodes;i++ ) {
            //int nParents=1;
            // low
            outputFileXML.println(" <DEFINITION>");
            outputFileXML.println("      <FOR>node"+i+"</FOR>");
            // high
            outputFileXML2.println(" <DEFINITION>");
            outputFileXML2.println("      <FOR>node"+i+"</FOR>");
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    // low
                    outputFileXML.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    // high
                    outputFileXML2.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    //nParents=nParents*numberStatesXML[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
                }
            }  // end of if
            // input low and high table
            outputFileXML.print("     <TABLE>");
            outputFileXML2.print("     <TABLE>");
            for (int a=0;a<table1.length;a++) {
                if (table1[a]<table2[a])	{
                    outputFileXML.print(table1[a]+" ");
                    outputFileXML.print(table1[a+1]+" ");
                    outputFileXML2.print(table2[a]+" ");
                    outputFileXML2.print(table2[a+1]+" ");
                } else	{
                    outputFileXML.print(table2[a]+" ");
                    outputFileXML.print(table2[a+1]+" ");
                    outputFileXML2.print(table1[a]+" ");
                    outputFileXML2.print(table1[a+1]+" ");
                }
                a=a+1;
            }
            // low
            outputFileXML.println("</TABLE>");
            outputFileXML.println(" </DEFINITION>");
            // high
            outputFileXML2.println("</TABLE>");
            outputFileXML2.println(" </DEFINITION>");
            
            ///////////// end of inputing Tables Interval
            
            
        } // end of for i(Node)
        
        // Finalizing low and high xml networks
        // low
        outputFileXML.println("</NETWORK>");
        outputFileXML.println("</BIF>");
        // high
        outputFileXML2.println("</NETWORK>");
        outputFileXML2.println("</BIF>");
        
    }

    
    /**
     * Save a binary credal network in the BIF InterchangeFormat for 
	 * - Java Bayes cif format(all vertex),
	 * - ".cif" (with linear base) and
	 * - Lower and Upper .xml format.
     */
    public void saveBinaryCIF(String name,String format,int n, int maxValues) throws Exception {
        
        // to save in JavaBayes bif format
        String fileName = name.concat("_" + (n+1) + "_jb.cif");
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        
        // to save in interval bif format
        String fileName1 = name.concat("_" + (n+1) + ".cif");
        PrintStream outputFile1 = new PrintStream(new FileOutputStream(fileName1), true);
        
        // to save in lower and upper network
        String fileNameXML = name.concat("_" + (n+1) + "Lower.xml");
        PrintStream outputFileXML = new PrintStream(new FileOutputStream(fileNameXML), true);
        String fileNameXML2 = name.concat("_" + (n+1) + "Upper.xml");
        PrintStream outputFileXML2 = new PrintStream(new FileOutputStream(fileNameXML2), true);
        
        // Saving in JavaBayes Format
        outputFile.println("// \tCredal network in CIF(CredalNet Interchange Format)");
        outputFile.println("// \tProduced by BNGenerator (http://www.pmr.poli.usp.br/ltd/Software/BNGenerator");
        outputFile.println("// \tNumber of nodes:" + getnNodes());
        outputFile.println("// \tOutput created " + (new Date()));
        //outputFile.println("-->\n\n\n");
        String correctedName=name.concat("" + (n+1) );
        if (name != null)  {
            outputFile.println("network \"" + correctedName + "\"\t{");
            outputFile.println("}");
        }
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            outputFile.println("variable  \"node"+ j + "\"\t{");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            outputFile.print("\ttype discrete["+states+"]  {");
            for (int i=0;i<(states);i++) {
                outputFile.print(" \"state"+i+"\" ");
                countState++;
            }
            outputFile.println("};");
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile.println("\tproperty \"position = ("+xx+", "+yy+") \";");
            outputFile.println("}");
            contaEmX[nP]++;
        } //end of for
        
        // Saving in Interval Cif Format
        outputFile1.println("// \tCredal network in CIF(CredalNet Interchange Format)");
        outputFile1.println("// \tProduced by BNGenerator (http://www.pmr.poli.usp.br/ltd/Software/BNGenerator");
        outputFile1.println("// \tNumber of nodes:" + getnNodes());
        outputFile1.println("// \tOutput created " + (new Date()));
        //outputFile1.println("-->\n\n\n");
        correctedName=name.concat("" + (n+1) );
        if (name != null)  {
            outputFile1.println("network \"" + correctedName + "\"\t{");
            outputFile1.println("}");
        }
        max=2;
        nDegree=getMaxDegree()+1;
        int contaEmX_1[]= new int[nDegree];
        int numberStates_1[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            outputFile1.println("variable  \"node"+ j + "\"\t{");
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            outputFile1.print("\ttype discrete["+states+"]  {");
            for (int i=0;i<(states);i++) {
                outputFile1.print(" \"state"+i+"\" ");
                countState++;
            }
            outputFile1.println("};");
            numberStates_1[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX_1[nP]);
            x=30+120*contaEmX_1[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            outputFile1.println("\tproperty \"position = ("+xx+", "+yy+") \";");
            outputFile1.println("}");
            contaEmX_1[nP]++;
        } //end of for
        
        // Saving in Interval Cif Format
        outputFileXML.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML.println("<!--");
        outputFileXML.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML.println("\tNumber of nodes:" + getnNodes());
        outputFileXML.println("\tOutput created " + (new Date()));
        outputFileXML.println("-->\n\n\n");
        outputFileXML.println("<BIF VERSION=\"0.3\">");
        outputFileXML.println("<NETWORK>");
        //if (name != null)  outputFileXML.println("<NAME>" + correctedName + "</NAME>");
        outputFileXML.println("<NAME>InternalNetwork</NAME>");
		// high
        outputFileXML2.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML2.println("<!--");
        outputFileXML2.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML2.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML2.println("\tNumber of nodes:" + getnNodes());
        outputFileXML2.println("\tOutput created " + (new Date()));
        outputFileXML2.println("-->\n\n\n");
        outputFileXML2.println("<BIF VERSION=\"0.3\">");
        outputFileXML2.println("<NETWORK>");
        if (name != null)  outputFileXML2.println("<NAME>" + correctedName + "</NAME>");
        
        max=2;
        nDegree=getMaxDegree()+1;
        int contaEmXXML[]= new int[nDegree];
        int numberStatesXML[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            // low
            outputFileXML.println("<!-- Variables -->");
            outputFileXML.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML.println("    <NAME>node"+ j + "</NAME>");
            // high
            outputFileXML2.println("<!-- Variables -->");
            outputFileXML2.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML2.println("    <NAME>node"+ j + "</NAME>");
            
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            for (int i=0;i<(states);i++) {
                // low
                outputFileXML.println("    <OUTCOME>state"+i+"</OUTCOME>");
                // high
                outputFileXML2.println("    <OUTCOME>state"+i+"</OUTCOME>");
                countState++;
            }
            numberStatesXML[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmXXML[nP]);
            x=30+120*contaEmXXML[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            // low
            outputFileXML.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML.println("</VARIABLE>");
            // high
            outputFileXML2.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML2.println("</VARIABLE>");
            contaEmXXML[nP]++;
        } //end of for
        
        //****************** INPUTING TABLES**************
        
        for (int i=0; i<getnNodes(); i++)  {
            //CategoricalVariable var[]=probabilitiesLower[i].getVariables();
            int valuesLength=(int)Math.pow(2,parentMatrix[i][0]);
            int nTable=(int)Math.pow(2,(valuesLength/2));
            float values[][]= new float[nTable][valuesLength];
            
            // create lower and upper tables
            float table1[]=df.generateDistributionFunction(2,(valuesLength/2),"emFloat");
            float table2[]=df.generateDistributionFunction(2,(valuesLength/2),"emFloat");
            
            float lowTable[]= new float[valuesLength];
            float highTable[]= new float[valuesLength];
            
            int auxZ=0;
            for (int z=0;z<(valuesLength)/2;z++)    {
                if (table1[2*z]<table2[2*z])	{
                    lowTable[auxZ]=table1[2*z];
                    lowTable[auxZ+(valuesLength/2)]=table1[2*z+1];
                    highTable[auxZ]=table2[2*z];
                    highTable[auxZ+(valuesLength/2)]=table2[2*z+1];
                    auxZ++;
                } else {
                    lowTable[auxZ]=table2[2*z];
                    lowTable[auxZ+(valuesLength/2)]=table2[2*z+1];
                    highTable[auxZ]=table1[2*z];
                    highTable[auxZ+(valuesLength/2)]=table1[2*z+1];
                    auxZ++;
                }
            }
            
            for (int j=0;j<(valuesLength)/2;j++)    {
                boolean isLow=true;
                int cont=0;
                int base=(int)Math.pow(2,j);
                for (int k=0;k<nTable;k++)  {
                    if (cont==base) {
                        cont=0;
                        if (isLow) isLow=false;
                        else isLow=true;
                    }
                    if (isLow) {
                        values[k][j]=lowTable[j];
                        values[k][j+(valuesLength)/2]=lowTable[j+(valuesLength)/2];
                    } else    {
                        values[k][j]=highTable[j];
                        values[k][j+(valuesLength)/2]=highTable[j+(valuesLength)/2];
                    }
                    cont++;
                    
                }
            }
            /////////////////////// inputing tables of JavaBayes
            outputFile.print("probability ( \"node"+i+"\" ");
            for (int j=1; j<parentMatrix[i][0]; j++) {
                outputFile.print( " \"node" + parentMatrix[i][j] + "\" ");
            }
            outputFile.print(") {");
            outputFile.println(" //" +  (parentMatrix[i][0]+1) +
                    " variable(s) and " + valuesLength + " values");
            
            for (int j=0;j<nTable;j++)  {
                outputFile.println("\ttable ");
                for (int k=0; k<valuesLength; k++)  outputFile.print("\t" + values[j][k]);
                outputFile.println(";");
            }
            outputFile.println("}");
            
            ////////////////////////// inputing tables of Interval Bif
            outputFile1.print("probability ( \"node"+i+"\" ");
            for (int j=1; j<parentMatrix[i][0]; j++) {
                outputFile1.print( " \"node" + parentMatrix[i][j] + "\" ");
            }
            outputFile1.print(") {");
            outputFile1.println(" //" +  (parentMatrix[i][0]+1) +
                    " variable(s) and " + valuesLength + " values");
            
            outputFile1.println("\ttable ");
            for (int k=0; k<valuesLength; k++)  outputFile1.print("\t" + lowTable[k]);
            outputFile1.println(";");
            outputFile1.println("\ttable ");
            for (int k=0; k<valuesLength; k++)  outputFile1.print("\t" + highTable[k]);
            outputFile1.println(";");
            
            outputFile1.println("}");
            
            ///////////// Inputing Tables Interval
            // low
            outputFileXML.println("<!-- Probability Distributions -->");
            // high
            outputFileXML2.println("<!-- Probability Distributions -->");
            //for (int i=0;i<nNodes;i++ ) {
            //int nParents=1;
            // low
            outputFileXML.println(" <DEFINITION>");
            outputFileXML.println("      <FOR>node"+i+"</FOR>");
            // high
            outputFileXML2.println(" <DEFINITION>");
            outputFileXML2.println("      <FOR>node"+i+"</FOR>");
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    // low
                    outputFileXML.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    // high
                    outputFileXML2.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    //nParents=nParents*numberStatesXML[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
                }
            }  // end of if
            // input low and high table
            outputFileXML.print("     <TABLE>");
            outputFileXML2.print("     <TABLE>");
            for (int a=0;a<table1.length;a++) {
                if (table1[a]<table2[a])	{
                    outputFileXML.print(table1[a]+" ");
                    outputFileXML.print(table1[a+1]+" ");
                    outputFileXML2.print(table2[a]+" ");
                    outputFileXML2.print(table2[a+1]+" ");
                } else	{
                    outputFileXML.print(table2[a]+" ");
                    outputFileXML.print(table2[a+1]+" ");
                    outputFileXML2.print(table1[a]+" ");
                    outputFileXML2.print(table1[a+1]+" ");
                }
                a=a+1;
            }
            // low
            outputFileXML.println("</TABLE>");
            outputFileXML.println(" </DEFINITION>");
            // high
            outputFileXML2.println("</TABLE>");
            outputFileXML2.println(" </DEFINITION>");
            
            ///////////// end of inputing Tables Interval
            
            
        } // end of for i(Node)
        
        // Finalizing low and high xml networks
        // low
        outputFileXML.println("</NETWORK>");
        outputFileXML.println("</BIF>");
        // high
        outputFileXML2.println("</NETWORK>");
        outputFileXML2.println("</BIF>");
        
    }
    
    
    private void saveBNXML_interval(String name,String format,int n, int maxValues) throws Exception { // Save BN in XML format
        String fileNameXML = name.concat("_" + (n+1) + "Lower.xml");
        PrintStream outputFileXML = new PrintStream(new FileOutputStream(fileNameXML), true);
        String fileNameXML2 = name.concat("_" + (n+1) + "Upper.xml");
        PrintStream outputFileXML2 = new PrintStream(new FileOutputStream(fileNameXML2), true);
        // low
        outputFileXML.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML.println("<!--");
        outputFileXML.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML.println("\tNumber of nodes:" + getnNodes());
        outputFileXML.println("\tOutput created " + (new Date()));
        outputFileXML.println("-->\n\n\n");
        outputFileXML.println("<BIF VERSION=\"0.3\">");
        outputFileXML.println("<NETWORK>");
        // high
        outputFileXML2.println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n\n");
        outputFileXML2.println("<!--");
        outputFileXML2.println("\tBayesian network in XMLBIF v0.3 (BayesNet Interchange Format)");
        outputFileXML2.println("\tProduced by JavaBayes (http://www.cs.cmu.edu/~javabayes/");
        outputFileXML2.println("\tNumber of nodes:" + getnNodes());
        outputFileXML2.println("\tOutput created " + (new Date()));
        outputFileXML2.println("-->\n\n\n");
        outputFileXML2.println("<BIF VERSION=\"0.3\">");
        outputFileXML2.println("<NETWORK>");
        
        String correctedName=name.concat("" + (n+1) );
        outputFileXML.println("<NAME>InternalNetwork</NAME>");
		//if (name != null)  outputFileXML.println("<NAME>" + correctedName + "</NAME>");
        
        int max=2;
        double x,y;
        int nDegree=getMaxDegree()+1;
        int contaEmXXML[]= new int[nDegree];
        int numberStatesXML[] = new int[getnNodes()];
        
        for (int j=0;j<getnNodes();j++) {
            // low
            outputFileXML.println("<!-- Variables -->");
            outputFileXML.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML.println("    <NAME>node"+ j + "</NAME>");
            // high
            outputFileXML2.println("<!-- Variables -->");
            outputFileXML2.println("<VARIABLE TYPE=\"nature\">");
            outputFileXML2.println("    <NAME>node"+ j + "</NAME>");
            
            max = calculateNumberOfStates(maxValues,isFixed_nValue());
            int countState=0; // count the number of states
            int states=Math.max(2,max);
            //int states=nStates[j];
            for (int i=0;i<(states);i++) {
                // low
                outputFileXML.println("    <OUTCOME>state"+i+"</OUTCOME>");
                // high
                outputFileXML2.println("    <OUTCOME>state"+i+"</OUTCOME>");
                countState++;
            }
            numberStatesXML[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmXXML[nP]);
            x=30+120*contaEmXXML[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            // low
            outputFileXML.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML.println("</VARIABLE>");
            // high
            outputFileXML2.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            outputFileXML2.println("</VARIABLE>");
            contaEmXXML[nP]++;
        } //end of for
        // low
        outputFileXML.println("<!-- Probability Distributions -->");
        // high
        outputFileXML2.println("<!-- Probability Distributions -->");
        for (int i=0;i<getnNodes();i++ ) {
            int nParents=1;
            // low
            outputFileXML.println(" <DEFINITION>");
            outputFileXML.println("      <FOR>node"+i+"</FOR>");
            // high
            outputFileXML2.println(" <DEFINITION>");
            outputFileXML2.println("      <FOR>node"+i+"</FOR>");
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    // low
                    outputFileXML.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    // high
                    outputFileXML2.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    nParents=nParents*numberStatesXML[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
                }
            }  // end of if
            
            float table1[]=df.generateDistributionFunction(numberStatesXML[i],nParents,"emFloat");
            float table2[]=df.generateDistributionFunction(numberStatesXML[i],nParents,"emFloat");
            
            // input low and high table
            outputFileXML.print("     <TABLE>");
            outputFileXML2.print("     <TABLE>");
            for (int a=0;a<table1.length;a++) {
                if (table1[a]<table2[a])	{
                    outputFileXML.print(table1[a]+" ");
                    outputFileXML.print(table1[a+1]+" ");
                    outputFileXML2.print(table2[a]+" ");
                    outputFileXML2.print(table2[a+1]+" ");
                } else	{
                    outputFileXML.print(table2[a]+" ");
                    outputFileXML.print(table2[a+1]+" ");
                    outputFileXML2.print(table1[a]+" ");
                    outputFileXML2.print(table1[a+1]+" ");
                }
                a=a+1;
            }
            // low
            outputFileXML.println("</TABLE>");
            outputFileXML.println(" </DEFINITION>");
            // high
            outputFileXML2.println("</TABLE>");
            outputFileXML2.println(" </DEFINITION>");
            
        } // end of for
        // End net description
        // low
        outputFileXML.println("</NETWORK>");
        outputFileXML.println("</BIF>");
        // high
        outputFileXML2.println("</NETWORK>");
        outputFileXML2.println("</BIF>");
        
    } // end of class saveBNXML_interval
    
    public void generateParentMatrix(BayesNet network,int parentsMatrix[][]){
        BayesNet bn=network;
        int lastPut;
        int index;
        CategoricalVariable present;
        CategoricalVariable children[];
        CategoricalVariable parents[];
        CategoricalVariable list[];
        int nVar = bn.numberVariables();
        list = bn.getVariables();
        index = 0;
        lastPut = 0;
        System.out.println("\n Network was loaded!");
        System.out.println("\n number of variables:"+nVar);
        while(index!=nVar){
            present = list[index];
            //System.out.println("\n Present variable:"+present.getName());
            parents = bn.getParentsAndSelf(present);
            for(int i=1;i<parents.length;i++){
                //System.out.println("\n This variable has parents!");
                int lastPoint=parentsMatrix[present.getIndex()][0];
                // add parent and count it
                parentsMatrix[present.getIndex()][lastPoint]= parents[i].getIndex();
                parentsMatrix[present.getIndex()][0]++;
                System.out.println("");
             
            }
               index++;
        }
    }
    
        /* Method that return a Graph object of the actual generated network
         */
    /*
    private Graph getGraph(String name,String format,int n, int maxn) throws Exception { // Save BN in XML format
     
        String graphName = name.concat("" + n); // n is just a second name
     
        // codigo para javabayes
        Graph graph = new Graph();
        graph.setName(graphName);
     
        int max=2;
        double x,y;
        int nDegree=maxDegree+1;
        int contaEmX[]= new int[nDegree];
        int numberStates[] = new int[nNodes];
     
        for (int j=0;j<nNodes;j++) {
            if (maxn!=2)  max=2+(int)(rand.nextFloat()*(maxn-1));	//expression to sort uniformily between 2 and maxn
            int countState=0; // count the number of states
            int states=Math.max(2,max);
     
            //int states=nStates[j];
     
     
            String categories[] = new String[states];
            for (int i=0;i<(states);i++) {
                //                outputFile.println("    <OUTCOME>state"+i+"</OUTCOME>");
                categories[i] = state + i;
     
     
                countState++;
            }
     
            ContentsChanceNode contents = new ContentsChanceNode(new DrawChanceNode());
     
            DataFactory df = DataBasicFactory.getInstance();
            CategoricalVariable cv = df.newCategoricalVariable(variableName, categories);
     
            double defaultProb = 1.0 / categories.length;
            double[] probabilities = new double[categories.length];
            //    		for (int i = 0; i < probabilities.length; i++) {
            //    			probabilities[i] = defaultProb;
            //    		}
            //
            CategoricalProbability cp = df.newCategoricalProbability(cv, probabilities);
            cv.setProbability(cp);
     
            contents.setVariable(cv);
     
            Node node = new Node();
     
            node.setContents(contents);
            // Add node to the graph.
            graph.add(node);
     
     
            numberStates[j]=countState;
            int nP=parentMatrix[j][0]-1;
            //if(nP<0) System.out.println("Erro! nP(numero de pais) assumiu valor menor do que zero!");
            y=30+120*nP+15*Math.sin(3.1415/2*contaEmX[nP]);
            x=30+120*contaEmX[nP]+15*Math.cos(3.1415*nP);
            int xx=(int)x;
            int yy=(int)y;
            //            outputFile.println("    <PROPERTY>position = ("+xx+", "+yy+")</PROPERTY>");
            //            outputFile.println("</VARIABLE>");
     
            node.setPosition(xx, yy);
            contaEmX[nP]++;
        } //end of for
     
        outputFile.println("<!-- Probability Distributions -->");
        for (int i=0;i<nNodes;i++ ) {
            int nParents=1;
            outputFile.println(" <DEFINITION>");
            outputFile.println("      <FOR>node"+i+"</FOR>");
     
            Node childNode = ????;
     
            if (parentMatrix[i][0]!=1)  {
                int aux=parentMatrix[i][0];
                for (int j=1;j<aux;j++) {
                    //                    outputFile.println("      <GIVEN>node"+parentMatrix[i][j]+"</GIVEN>");
                    nParents=nParents*numberStates[parentMatrix[i][j]];
                    //nParents=nParents*nStates[parentMatrix[i][j]];
     
                    Node parentNode = ????;
     
                    graph.getNode("dfsdf");
     
                    ContentsNode contents = childNode.getContents();
     
                    if (contents instanceof ContentsChanceNode) {
                        if (((ContentsChanceNode) contents).addParent(parentNode) == false)
                            return;
                    }
                    // Connect the nodes.
                    Edge edge = new Edge(parentNode, childNode);
                    edge.setDrawable(new DrawEdge());
     
                    graph.add(edge);
     
                }
     
     
     
            }  // end of if
            inputTableXML(outputFile,numberStates[i],nParents);
            //inputTableXML(outputFile,nStates[i],nParents);
            outputFile.println(" </DEFINITION>");
        } // end of for
        // End net description
        //        outputFile.println("</NETWORK>");
        //        outputFile.println("</BIF>");
     
        if(save) {
            WriteFormatter writeFormatter = null;
            try {
                writeFormatter = getWriteFormatter(format);
            } catch (NoClassDefFoundError e) {
     
                return;
            }
     
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(fileName));
                writeFormatter.write(writer, graph);
                writer.close();
                System.out.println("saved");
            } catch (IOException e) {
                System.out.println("error");
            }
     
        }
     
    } // end of class getGraph
     
     
     */
    
    private int calculateNumberOfStates(int maxValues,boolean fixed_nVal) {
        int max = 2;
        if (fixed_nVal) {
            max = maxValues;
        } else {
            if (maxValues!=2)  max=2+(int)(rand.nextFloat()*(maxValues-1));	//expression to sort uniformily between 2 and maxValues
        }
        return max;
    }
    
    public void inputTableXML(PrintStream outFile, int nvv, int npp){ // Method to input xml format probability distribution
        float table[]=df.generateDistributionFunction(nvv,npp,"emFloat");
        outFile.print("     <TABLE>");
        for (int i=0;i<table.length;i++) outFile.print(table[i]+" ");
        outFile.println("</TABLE>");
    }
    
    //.. This method create two probability function simultaneously for each file
    public void inputTableXMLJava(PrintStream outFile,PrintStream outFileEJB, int nvv, int npp){
        float table[]=df.generateDistributionFunction(nvv,npp,"emFloat");
        float tableEJB[]=transTable(table,nvv,npp);
        outFile.print("     <TABLE>");
        outFileEJB.print("\t\tnew double[] {");
        int tam=table.length;
        for (int i=0;i<tam;i++) {
            outFile.print(table[i]+" ");
            outFileEJB.print(tableEJB[i]+" ");
            if (i<(tam-1))  outFileEJB.print(",");
        }
        outFile.println("</TABLE>");
        outFileEJB.println("});");
    }
    
    public void inputTableJava(PrintStream outFileEJB, int nvv, int npp){	// Method to input bif format probability distribution
        float table[]=df.generateDistributionFunction(nvv,npp,"emFloat");
        float tableEJB[]=transTable(table,nvv,npp); // For some BIF notation in java, probabilty table is transposed
        outFileEJB.print("\t\tnew double[] {");
        int tam=table.length;
        for (int i=0;i<tam;i++) {
            outFileEJB.print(tableEJB[i]+" ");
            if (i<(tam-1))  outFileEJB.print(",");
        }
        outFileEJB.println("});");
    }
    
    public float[] transTable( float t[], int nv, int np) { // This method is used to transpose the probability distribution to xml format
        float trans[]=new float[t.length];
        int ii=0;
        for (int i=0;i<trans.length;i++)  {
            int q =i/nv;
            ii=(np*(i-nv*q))+q;
            trans[ii]=t[i];
        }
        return(trans);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////// What is essential for generating networks stops here!! ///////////////
    ///////////////////////////////////////////////////////////////////////////////////////
    ///////////////  Remainder methods are to:
    ///////////////  1) TestUniformity
    ///////////////  2) Other experimental generation methods
    ///////////////  3) Graph layout
    //////////////////////////////////////////////////////////////////////////////////////
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Methods for testing uniformity /////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    // If you want to use the following methods, contact the author.
    public int[][] orderMatrix(int[][] matrix)	{
        int orderedMatrix[][]= matrix;
        for (int i=0;i<getnNodes();i++)	{
            boolean visited[] = new boolean[getnNodes()];
            for (int j=1;j<orderedMatrix[i][0];j++)	{
                visited[orderedMatrix[i][j]]=true;
            }
            int cont=1;
            for (int j=0;j<getnNodes();j++)	{
                if (visited[j]==true)	{
                    orderedMatrix[i][cont]=j;
                    cont++;
                }
            }
            
        }
        return(orderedMatrix);
    }
    
    public void testUniformity(int iw)	{   // this method is a test suite for verifying
        //uniformity of generated graphs
        int[][] orderedM = orderMatrix(parentMatrix);
        boolean halt=false;
        boolean halt2=false;
        for (int i=0;i<numberStates && (halt==false) && (halt2==false);i++)	{
            if (repository[i].table!=null)	{
                if ( compareMatrix(orderedM,repository[i].table) )	{
                    distribution[i]++;
                    halt=true;
                }
            } else halt2=true;
        }
        if (halt==false)	{  // first time of actual type of graph
            repository[lastPosition].table=receiveMatrix(orderedM,getnNodes(),getMaxDegree()+2);
            distribution[lastPosition]=1;
            iwOfEachGraphType[lastPosition]=iw;  //IW
            lastPosition++;
        }
        //if (compareMatrix(parentMatrix,parentMatrix))	System.out.println("equivalent matrixs");
        ///System.out.println("equivalent matrix :"+resp);
    }
    
    public int[][] receiveMatrix(int[][] matrix, int ii, int jj)	{
        int m[][] = new int[ii][jj];
        for (int i=0;i<ii ;i++ )	{
            for (int j=0;j<jj ;j++ )	m[i][j]=matrix[i][j];
        }
        return(m);
    }
    
    public boolean compareMatrix(int[][] matrix1,int[][] matrix2)	{ // return true if matrixs is the same
        for (int i=0;(i<getnNodes());i++ )	{
            int last;
            //if (matrix1[i][0]==0) last = maxDegree+2;
            //else last = matrix1[i][0];
            for (int j=0;j<matrix1[i][0] ;j++ )	{
                if (matrix1[i][j]!=matrix2[i][j])	return(false);
            }
        }
        return(true);
    }
    
    public void printDistribution()	{
        System.out.println("Distribution:");
        System.out.println();
        for (int i=0;i<2000 ;i++)	System.out.print(" "+distribution[i]);
    }
    
    private void saveDistribution(String name,String format,int nG,int nI, String structure,int IW)	throws Exception {
        String fileName = name.concat(structure).concat("_n"+getnNodes()).concat("_d"+getMaxDegree()).concat("_iw"+IW).concat("_g"+nG).concat("_i"+nI).concat("." + format);
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        outputFile.println("Distribution:");
        outputFile.println("Characteristics: ");
        outputFile.println("\t"+getnNodes()+" nodes");
        outputFile.println("\t"+getMaxDegree()+" maxDegree");
        outputFile.println("\t"+getMaxInDegree()+" maxInDegree");
        outputFile.println("\t"+getMaxOutDegree()+" maxOutDegree");
        outputFile.println("\t"+getMaxArcs()+" maxArcs");
        outputFile.println("\t"+nG+" graphs generated");
        outputFile.println("\tStructure type:"+structure);
        outputFile.println("\t"+nI+" iteration between samples or until first sample");
        outputFile.println("\tMaximum allowed induced-width:"+IW);
        outputFile.println("\t(Counting)Number of different graphs: "+lastPosition);
        outputFile.println("\t qui-square: "+quiSquare);
        for (int i=0;i<20000 && distribution[i]!=0;i++)	outputFile.println(" "+distribution[i]);
    }
    
    void averageQuiSquare(int nGra,int nIterati,int maxVal,String te,String fo,String baseFileNam,int maxInducedW) throws Exception{
        int nSet=10;
        double averageQuiSquare=0;
        String fileName = "ComputingQuiSquare".concat("_n" +getnNodes()+"d"+getMaxDegree()+"iw"+maxInducedW);;
        PrintStream outputFile = new PrintStream(new FileOutputStream(fileName), true);
        outputFile.println(" * Number of nodes:" + getnNodes());
        outputFile.println(" * Number of maxDegree:" + getMaxDegree());
        outputFile.println(" * Number of maxIW:" + maxInducedW);
        outputFile.println(" **** Networks were created on " + (new Date()));
        outputFile.println(" **** Number of iteration between samples:" + nIterati);
        outputFile.println(" **** Number of graph of each sample set:" + nGra);
        outputFile.println(" **** Number of sets:" + nSet);
        outputFile.println(" **** Number of different types of structure:" + lastPosition);
        for (int i=0;i<nSet;i++)    {
            lastPosition=0;
            distribution = new int[numberStates];
            for (int j=0;j<numberStates;j++ )  repository[j] = new Matrix();	// for testing Uniformity
            repository[0].inicializeTable(getnNodes(),getMaxDegree()+2);				// for testing Uniformity
            inicializeGraph(); // Inicialize a simple ordered tree as a BN structure
            generateMixedEJ(nGra,nIterati,maxVal,te,fo,baseFileNam,maxInducedW);
            computeQuiSquare(nGra);
            System.out.println("\tqui-Square set"+i+": "+quiSquare);
            outputFile.println("\tqui-Square set"+i+": "+quiSquare);
            averageQuiSquare=averageQuiSquare+quiSquare;
        }
        outputFile.println("\t Average qui-Square between "+nSet+" sets is: "+(averageQuiSquare/nSet));
    }
    
    private void computeQuiSquare(int nG)   {
        int nDifG=lastPosition;        // lastPosition keep the value of number of different graphs
        expectedFrequency=(double)nG/nDifG;
        quiSquare=0;
        for (int i=0;i<nDifG;i++)   {
            quiSquare=quiSquare+Math.pow((expectedFrequency-distribution[i]),2);
        }
        quiSquare=quiSquare/expectedFrequency;
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////
    ///////////  Auxiliar optional methods for developers ////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    public void pause()	{
        DataInputStream dis = null;
        dis = new DataInputStream(System.in);
        try {
            System.out.println("Type some button to continue.");
            String line = dis.readLine();
        } catch (Exception e) { System.out.println(e); }
    }
    
    // Routine for tests
    public void test(){
        boolean resp;
        inicializeGraph();
        removeArc(2,3);
        resp=isConnected();
        System.out.println("Resposta:"+resp);
        printArcs();
        printMatrix(parentMatrix,getnNodes(),getMaxDegree()+2);
        //printMatrix (sonMatrix,nNodes,maxDegree+2);
        //removeArc();
        //printMatrix (parentMatrix,nNodes,maxDegree+2);
        //printMatrix (sonMatrix,nNodes,maxDegree+2);
    }
    
    private void printArcs()  {  // just print the arc�s
        System.out.println("Arcs:");
        for (int i=0;i<getnNodes();i++)  {
            for (int j=1;j<sonMatrix[i][0];j++)
                System.out.println("\t"+i+" ---> "+sonMatrix[i][j]);
        }
    }
    
    public void printMatrix(int[][] matrix,int ii, int jj){
        for (int j=0;j<jj;j++) {
            for (int i=0;i<ii;i++) System.out.print("\t"+matrix[i][j]);
            System.out.println();
        }
        System.out.println();
    }

    public void generate(String structure, int nGraphs, int nIterations, int maxValues, String format, String baseFileName, int maxInducedWidth) throws Exception {
        if ( (structure.compareTo("singly") == 0))	{	//  polytree
            generatePolytree(nGraphs,nIterations,maxValues,"no",format,baseFileName,maxInducedWidth);
        } // end of if(structure=poly)
        else if	((structure.compareTo("multi") == 0)&&(maxInducedWidth==-1)) {	// multi-connected graph is the default structure
            generateMultiConnected(nGraphs,nIterations,maxValues,"no",format,baseFileName);
        } // end of if(structure=multi)
        else if ((structure.compareTo("multi") == 0)&&(maxInducedWidth!=-1))	{	// generate multi-connected with maxIW constraint
            generateMixedEJ(nGraphs,nIterations,maxValues,"no",format,baseFileName,maxInducedWidth);
        } // end of if(structure=pmmixed)
    }
    
    
    /** I am trying to find some simple way to find a good layout of the graph **************
     ** if someone has any suggestion, please contact me!                      **************
     * public int[][][] findGoodLayout()   {
     * int nRoot=0; // count number of roots
     * int position[][][] = new int[nNodes][nNodes][nNodes];
     * int distante=20;    // distance between nodes
     * int xPosition=1,level=1;
     * int countCoveredNode=0;
     * boolean coveredNode[] = new boolean[nNodes];
     * for (int i=0;i<nNodes;i++)   {
     * coveredNode=false;  // initialize vetor
     * if (parentMatrix[i][0]==1)  nRoot++;    // count number of roots
     * }
     * while (countCoveredNode<nNodes)    {
     * for
     * xPosition=1;
     * countCouveredNode++;
     * }
     * return();
     * }
     * public int[][][] findBestLayout()   {
     * int nRoot=0; // count number of roots
     * int position[][][] = new int[nNodes][nNodes][nNodes];
     * int distante=20;    // distance between nodes
     * int xPosition=1,yPosition=1;
     * boolean coveredNode[] = new boolean[nNodes];
     * boolean firstSon==true;
     * for (int i=0;i<nNodes;i++)   {
     * if (parentMatrix[i][0]==1)  {   //when node is a root
     * int currentNode=i;
     * findNextNode(currentNode);
     * }
     * }
     * return();
     * }
     * public void findNextNode(int cNode, int xPosition, int yPosition)   {
     * int currentNode=cNode;
     * int j=1;
     * if (firstSon==false)    xPosition++;
     * yPosition++;
     * while (sonMatrix[currentNode][0]!=0 && coveredNode[currentNode]==false)   {
     * fintNextNode(sonMatrix[currentNode][j], xPosition, yPosition);
     * coveredNode[currentNode]=true;
     * yPosition--;
     * xPosition++;
     * j++;
     * }
     * return();
     * }
     */
    
    
    
} // end of class BNGenerator



/*
 * Auxiliary class Matrix.
 */
class Matrix {
    public	int[][] table;
    public Matrix() {}
    public Matrix(int i, int j) {
        table = new int[i][j];
    }
    public void inicializeTable(int i, int j)	{
        table = new int[i][j];
    }
} // End of the class Matrix.