package br.usp.poli.generator;

//================================================================
//     Copyright (c) 2002, Escola Polit�cnica-USP
//                     All Rights Reserved
//================================================================
//
// NAME : DFGenerator
// @DATE        : 31/01/2002
// @AUTHOR      : Jaime Shinsuke Ide
//				  jaime.ide@poli.usp.br
//===============================================================

/* The DFGenerator distribution is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation (either
 * version 2 of the License or, at your option, any later version), 
 * provided that this notice and the name of the author appear in all 
 * copies. 
 * If you're using the software, please notify jaime.ide@poli.usp.br so
 * that you can receive updates and patches. DFGenerator is distributed
 * "as is", in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with the DFGenerator distribution. If not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
 
// This class generates random distribution functions.
import java.io.*;
import java.util.StringTokenizer;
import java.util.*;
import java.lang.*;
import cern.jet.stat.Probability;
import cern.jet.random.*;
//import MersenneTwister;
import cern.jet.random.engine.MersenneTwister;

public class DFGenerator {

private Random random = new Random();
int seed= (int)(100000*random.nextFloat());  
protected cern.jet.random.engine.RandomEngine engine = new cern.jet.random.engine.MersenneTwister(seed+5);
protected Gamma seedGamma= new Gamma(0.5,0.5,engine); // (alpha,lambda,random_engine)
protected MersenneTwister rn= new MersenneTwister(seed);

/*
* Default Constructor
*/
public DFGenerator () {
    
}

//////////// The class DFGenerator can run independly ///////////////////////////
public static void main(String args[]) {
    DFGenerator gbn = new DFGenerator(); 
	double vAlpha[]={1,1,1};  
    int dim = vAlpha.length;
	double distr[];
	float distrUni[];
	double soma=0.0,somaUni=0.0;
	int n=1000;  // number of generated distributions
	for (int j=0;j<n;j++) {
	    distr = gbn.generateDistribution(dim,vAlpha);
		distrUni = gbn.generateUniformDistribution(dim,"emFloat");
		System.out.print("Dirichlet: ");
		for (int i=0; i<dim; i++)  System.out.print(" " + distr[i]);
		System.out.println();
		
		/*
		float p=gbn.rn.nextFloat();
		float lp=-(float)Math.log(p);
		int ip=(int)(lp*1000);
		lp=(float)ip;
		lp=lp/1000;
		System.out.println("Float: "+p);
		System.out.println("-log(Float): "+lp);
		*/

		System.out.print("Uniform: ");
		for (int i=0; i<dim; i++)  System.out.print(" " + distrUni[i]);
		soma=soma+distr[1];
		somaUni=somaUni+distrUni[1];
		System.out.println();
	}
	System.out.println("Mean value at position one (Dirichlet): " + soma/n);
	System.out.println("Mean value at position one (Uniforme): " + somaUni/n);
} // end of the main. 

public double[] generateUniformDistribution(int n) {
	double distribution[] = new double[n];
    double normalization = 0.0;
    for (int i=0; i<n; i++) {
		distribution[i] = -Math.log(rn.nextDouble());
		normalization += distribution[i];
        }
    for (int i=0; i<n; i++)
		distribution[i] /= normalization;
    return(distribution);
} // End of generateDistribution method.

// This method that intends to produce a vector summing exact "1"
// is biased and do not produce a exact uniform distribuiton. (with intervals! - created on 2006 March)
public float[] generateUniformDistributionInterval(int n, String emFloat, float lowerP, float upperP) {
	float distribution[] = new float[n];
    float normalization = 0;
    for (int i=0; i<n; i++) {
		float randFloat=rn.nextFloat();
		randFloat=lowerP+randFloat*(upperP-lowerP);//resize randFloat to interval [lowerP,upperP]
		distribution[i] = -(float)Math.log(randFloat);
		normalization += distribution[i];
        }
    // Arredondamento para 4 casas decimais
	float auxSum1=0;
	for (int i=1; i<n; i++){
		distribution[i] /= normalization;
		int auxInt=Math.round((distribution[i]*10000));
		distribution[i]=((float)auxInt)/10000;
		auxSum1=auxSum1+distribution[i];
	}
	// Truque para que sempre some 1 exatamente
	distribution[0]=1-auxSum1;
	int auxInt=Math.round((distribution[0]*10000));
	distribution[0]=((float)auxInt)/10000;

	return(distribution);
} // End of generateDistribution method.

// This method that intends to produce a vector summing exact "1"
// is biased and do not produce a exact uniform distribuiton.
public float[] generateUniformDistribution2dig(int n, String emFloat) {
	float distribution[] = new float[n];
    float normalization = 0;
    for (int i=0; i<n; i++) {
		distribution[i] = -(float)Math.log(rn.nextFloat());
		normalization += distribution[i];
        }
    // Arredondamento para 4 casas decimais
	float auxSum1=0;
	for (int i=1; i<n; i++){
		distribution[i] /= normalization;
		int auxInt=Math.round((distribution[i]*100));
		distribution[i]=((float)auxInt)/100;
		auxSum1=auxSum1+distribution[i];
	}
	// Truque para que sempre some 1 exatamente
	distribution[0]=1-auxSum1;
	int auxInt=Math.round((distribution[0]*100));
	distribution[0]=((float)auxInt)/100;

	return(distribution);
} // End of generateDistribution method.

// This method that intends to produce a vector summing exact "1"
// is biased and do not produce a exact uniform distribuiton.
public float[] generateUniformDistribution(int n, String emFloat) {
	float distribution[] = new float[n];
    float normalization = 0;
    for (int i=0; i<n; i++) {
		distribution[i] = -(float)Math.log(rn.nextFloat());
		normalization += distribution[i];
        }
    // Arredondamento para 4 casas decimais
	float auxSum1=0;
	for (int i=1; i<n; i++){
		distribution[i] /= normalization;
		int auxInt=Math.round((distribution[i]*10000));
		distribution[i]=((float)auxInt)/10000;
		auxSum1=auxSum1+distribution[i];
	}
	// Truque para que sempre some 1 exatamente
	distribution[0]=1-auxSum1;
	int auxInt=Math.round((distribution[0]*10000));
	distribution[0]=((float)auxInt)/10000;

	return(distribution);
} // End of generateDistribution method.


public double[] generateDistributionFunction(int nv, int np) {
	double distribution[] = new double[nv*np];
        int cont=0;
	double alphas[]={0.1,1};
        for (int i=0;i<np;i++) {
	  //double distr[]=generateUniformDistribution(nv);
	  double distr[]=generateDistribution(nv,alphas);
          for (int j=0;j<nv;j++) {
	    distribution[cont]=distr[j];
		cont++;
      }
	}
    return(distribution);
} // End of generateDistribution method.

public float[] generateDistributionFunction(int nv, int np, String emFloat) {
	float distribution[] = new float[nv*np];
    int cont=0;
	for (int i=0;i<np;i++) {
	  float distr[]=generateUniformDistribution(nv,"emFloat");
	  for (int j=0;j<nv;j++) {
	    distribution[cont]=distr[j];
		cont++;
      }
	}
    return(distribution);
} // End of generateDistribution method.

public float[] generateDistributionFunction2dig(int nv, int np, String emFloat) {
	float distribution[] = new float[nv*np];
    int cont=0;
	for (int i=0;i<np;i++) {
	  float distr[]=generateUniformDistribution2dig(nv,"emFloat");
	  for (int j=0;j<nv;j++) {
	    distribution[cont]=distr[j];
		cont++;
      }
	}
    return(distribution);
} // End of generateDistribution method.

// method that generate distributions with probabilities uniformily distributed between lowerP and upperP
// created on 2006 March
public float[] generateDistributionFunctionInterval(int nv, int np, String emFloat,float lowerP,float upperP) {
	float distribution[] = new float[nv*np];
    int cont=0;
	for (int i=0;i<np;i++) {
	  float distr[]=generateUniformDistributionInterval(nv,"emFloat",lowerP,upperP);
	  for (int j=0;j<nv;j++) {
	    distribution[cont]=distr[j];
		cont++;
      }
	}
    return(distribution);
} // End of generateDistribution method.

// This method generate distribution functions with Dirichlet priors (alphas)
public double[] generateDistribution(int n,double alphas[]) {
	double distribution[] = new double[n];
    double normalization = 0.0;
    for (int i=0; i<n; i++) {
		distribution[i] =  generateGamma(alphas[i]);
		normalization += distribution[i];
        }
    for (int i=0; i<n; i++)
		distribution[i] /= normalization;
    return(distribution);
} // End of generateDistribution method.

public double generateGamma(double alpha) {
	double gamma;
	gamma=seedGamma.nextDouble(alpha,1);
    return(gamma);
} // End of generateGamma method.

} // End of the class DFGenerator.
