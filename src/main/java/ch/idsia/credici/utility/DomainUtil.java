package ch.idsia.credici.utility;

import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DomainUtil {

    public static TIntIntMap[] getObservationSpace(Strides dom){
        return (TIntIntMap[]) DomainUtil.getEventSpace(dom).stream().map(s -> ObservationBuilder.observe(dom.getVariables(), s)).toArray(TIntIntMap[]::new);
    }
    public static List<int[]> getEventSpace(Strides... domains) {
        Strides d = Arrays.stream(domains).reduce((d1 , d2) -> d1.concat(d2)).get();
        return IntStream.range(0, d.getCombinations()).mapToObj(i -> d.statesOf(i)).collect(Collectors.toList());
    }

    public static Strides subDomain(Strides domf, int... vars) {
        List varsizes = new ArrayList();
        for(int v : vars){
            varsizes.add(v);
            varsizes.add(domf.getCardinality(v));
        }
        return Strides.as(CollectionTools.toIntArray(varsizes));
    }

    public static Strides remove(Strides domf, int...toRemove){
        int[] vars = ArraysUtil.difference(domf.getVariables(), toRemove);
        return subDomain(domf,vars);
    }
    
    
}
