package ch.idsia.credici.utility;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.List;

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
}
