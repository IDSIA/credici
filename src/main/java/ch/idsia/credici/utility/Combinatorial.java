package ch.idsia.credici.utility;

import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Combinatorial {

    private Combinatorial() {}

    public static <T> List<List<T>> cartesianProduct(List<List<T>> a){
        return cartesianProduct(0, a);
    }

    private static <T> List<List<T>> cartesianProduct(int i, List<List<T>> a) {
        if(i == a.size() ) {
            List<List<T>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }
        List<List<T>> next = cartesianProduct(i+1, a);
        List<List<T>> result = new ArrayList<>();
        for(int j=0; j < a.get(i).size(); j++) {
            for(int k=0; k < next.size(); k++) {
                List<T> concat = new ArrayList<>();
                concat.add(a.get(i).get(j));
                concat.addAll(next.get(k));
                result.add(concat);
            }
        }
        return result;
    }

    public static int[][] getCombinations(int n, int[] elements){
        List<List<Integer>> lists = new ArrayList<>(n);
        for(int i=0; i<n; i++)
            lists.add(Ints.asList(elements));

        List<List<Integer>> cartProd = Combinatorial.cartesianProduct(lists);

        int[][] C = new int[cartProd.size()][];
        int i = 0;
        for(List<Integer> c : cartProd){
            C[i] = Ints.toArray(c);
            i++;
        }

        return C;
    }

    public static double[][] getCombinations(int n, double[] elements){

        List<List<Double>> lists = new ArrayList<>(n);
        for(int i=0; i<n; i++)
            lists.add(Doubles.asList(elements));

        List<List<Double>> cartProd = Combinatorial.cartesianProduct(lists);

        double[][] C = new double[cartProd.size()][];
        int i = 0;
        for(List<Double> c : cartProd){
            C[i] = Doubles.toArray(c);
            i++;
        }

        return C;
    }

    public static double[][] mapProbabilitySpace(int card, boolean zeros, double step){
        double init = 0.0;
        if(!zeros)
            init = step;

        List<Double> space = new ArrayList<>();
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


    public static int[][] randomPairs(int[] elements, int maxDist) {

        List<int[]> out = new ArrayList<>();
        List<Integer> aux = CollectionTools.asList(elements);
        Collections.shuffle(aux, RandomUtil.getRandom());

        while(!aux.isEmpty()){
            int xi = aux.get(0);
            aux.remove(0);

            for(int j=0; j<aux.size(); j++) {
                int xj = aux.get(j);
                if (Math.abs(xi - xj) <= maxDist) {
    
                    aux.remove(j);
                    out.add(new int[]{xi, xj});
                    break;
                }
            }
        }

        return out.stream().toArray(int[][]::new);
    }



    public static int[][] randomPairs(int[] elements, int maxDist, int[][] matrixDistances) {

        List out = new ArrayList();
        List<Integer> aux = CollectionTools.asList(elements);
        Collections.shuffle(aux, RandomUtil.getRandom());

        while(aux.size()>0){
            int xi = aux.get(0);
            aux.remove(0);

            for(int j=0; j<aux.size(); j++) {
                int xj = (int) aux.get(j);
                if (matrixDistances[xi][xj] <= maxDist) {
                    //System.out.println(xi+" "+xj);
                    aux.remove(j);
                    out.add(new int[]{xi, xj});
                    break;
                }
            }
        }

        return (int[][]) out.stream().toArray(int[][]::new);
    }


}
