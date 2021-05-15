package ch.idsia.credici.utility;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class Combinatorial {

    public static <T> List<List<T>> cartesianProduct(List<T>... a){
        return cartesianProduct(0, a);
    }

    private static <T> List<List<T>> cartesianProduct(int i, List<T>... a) {
        if(i == a.length ) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList());
            return result;
        }
        List<List<T>> next = cartesianProduct(i+1, a);
        List<List<T>> result = new ArrayList<>();
        for(int j=0; j < a[i].size(); j++) {
            for(int k=0; k < next.size(); k++) {
                List<T> concat = new ArrayList();
                concat.add(a[i].get(j));
                concat.addAll(next.get(k));
                result.add(concat);
            }
        }
        return result;
    }
    public static int[][] getCombinations(int n, int[] elements){

        List[] lists = new List[n];
        for(int i=0; i<n; i++)
            lists[i] = Ints.asList(elements);

        List cartProd = Combinatorial.cartesianProduct(lists);

        int[][] C = new int[cartProd.size()][];
        int i = 0;
        for(Object c : cartProd){
            C[i] = Ints.toArray((List)c);
            i++;
        }

        return C;
    }

    public static double[][] getCombinations(int n, double[] elements){

        List[] lists = new List[n];
        for(int i=0; i<n; i++)
            lists[i] = Doubles.asList(elements);

        List cartProd = Combinatorial.cartesianProduct(lists);

        double[][] C = new double[cartProd.size()][];
        int i = 0;
        for(Object c : cartProd){
            C[i] = Doubles.toArray((List)c);
            i++;
        }

        return C;
    }

    public static double[][] mapProbabilitySpace(int card, boolean zeros, double step){
        double init = 0.0;
        if(!zeros)
            init = step;
        List space = new ArrayList();
        for(double p=init; p<=1.0; p+=step)
            space.add(p);

        double finalInit = init;
        return Stream.of(Combinatorial
                .getCombinations(card-1, Doubles.toArray(space)))
                .filter(v -> DoubleStream.of(v).sum() < 1.0- finalInit)
                .map(v -> Doubles.concat(v, new double[]{1.0-DoubleStream.of(v).sum()}))
                .toArray(double[][]::new);
    }

    public static int[][] randomPairs(int[] elements){
        int[] aux = CollectionTools.shuffle(elements);
        int N = aux.length/2;
        int[][] pairs = new int[N][2];

        for(int i=0; i<N; i++){
            pairs[i][0] = aux[i*2];
            pairs[i][1] = aux[i*2 + 1];
        }

        return pairs;
    }
}
