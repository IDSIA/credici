package ch.idsia.credici.model.io.net;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.IndexIterator;


public class NetWriter {

    private static final String VARIABLE_PREFIX = "node";


    public static void write(BayesianNetwork network, String fileName) throws IOException {
        
        try(PrintWriter out = new PrintWriter(new FileWriter(fileName))) {

            out.println("network temp {\n}\n");

            for (int i : network.getVariables()) {
                int size = network.getSize(i);

                out.println("variable " + VARIABLE_PREFIX + i + " {");
                out.print("\ttype discrete[" + size + "] { ");

                out.print(IntStream.range(0, size).mapToObj(String::valueOf).reduce((a,b)->a.concat(", ").concat(b)).get());
                out.println(" };");
                out.println("}\n");
            }

            for (int i : network.getVariables()) {
                
                BayesianFactor bf = network.getFactor(i);
                Strides s = bf.getDomain();
                
                int[] conditioning = ArrayUtils.removeElement(s.getVariables().clone(), i);
                int[] target = ArrayUtils.addAll(new int[] {i}, conditioning);
                ArrayUtils.reverse(target);

                IndexIterator ii = getIterator(s, network.getDomain(target));
                
                List<String> cond = Arrays.stream(conditioning).mapToObj(a -> VARIABLE_PREFIX + a).collect(Collectors.toList());

                out.print("probability (" + VARIABLE_PREFIX + i );
                if (!cond.isEmpty()) {
                    out.print(" | " + String.join(", ", cond));
                }
                out.println(") {");
                out.print("\ttable");
                while (ii.hasNext()) {
                    int index = ii.next();
                    out.print(" " + bf.getValueAt(index));
                }

                out.println(";\n}\n");
            }
        }
    }


    private static IndexIterator getIterator(Strides source, Strides target) {
        int[] sizes = target.getSizes();
        int[] strides = new int[sizes.length]; // initialized to zero!

        for (int i = 0; i < strides.length; ++i) {
            int idx = source.indexOf(target.getVariables()[i]);
            if (idx >= 0) {
                strides[i] = source.getStrideAt(idx);
            }
        }
        return new IndexIterator(strides, sizes, target.getCombinations());

    }

}
