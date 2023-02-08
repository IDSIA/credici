package ch.idsia.credici.inference.ace;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import ch.javasoft.metabolic.fa.FaConstants.Net;

/*
 * potential ( D | C B )
{
	data = (
					(
						(	d0 | c0 b0,	   d1 | c0 b0 )
		        		(	d0 | c0 b1,	   d1 | c0 b1 )
					)
		        	(
						(	d0 | c1 b0,	   d1 | c1 b0 )
		        		(	d0 | c1 b1,	   d1 | c1 b1 )
					)
				);
}


node n1 {
	states = (s0 s1);
}
 */
public class NetworkWriter implements Closeable {
    private PrintWriter stream; 

    public NetworkWriter(File target) throws IOException, FileNotFoundException {
        this.stream = new PrintWriter(target);
    }

    public void write(BayesianNetwork network) throws IOException {
        for (int node : network.getVariables()){
            stream.printf("node n%d {%n", node);
            // states string
            String states = IntStream.range(0, network.getSize(node)).
                mapToObj(i->"s"+i).
                collect(Collectors.joining("\" \""));
            stream.printf("  states ( \"%s\" );%n", states);
            stream.println("}");
            stream.println();
        }

        // potential
        for (int node : network.getVariables()){
            BayesianFactor factor = network.getFactor(node);
            int[] parents = factor.getDomain().remove(node).getVariables();
            stream.printf("potential ( n%d", node);
            if (parents.length == 0) stream.println(" ) {");
            else {
                String parents_str = Arrays.stream(parents).mapToObj(i->"n"+i).collect(Collectors.joining(" "));
                stream.printf(" | %s ) {%n", parents_str);
            }

            // actual data ordering (we're not using the stride object this array is part of!)
            ArrayUtils.reverse(parents);
            int[] order = ArraysUtil.append(new int[]{node}, parents);
            stream.print("   data = ( ");
            IndexIterator iter = factor.getDomain().getReorderedIterator(order);
            while (iter.hasNext()) {
                int item = iter.next();
               //int[] states = iter.getPositions();
                stream.print(factor.getValueAt(item));
                stream.print(" ");
            }
            stream.println(" );");
            stream.println("}");
        }
        
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
