package ch.idsia.credici.utility;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.SparseModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.stream.IntStream;

public class InferenceTools {
    public static TIntIntMap modifyModelForJointQuery(GraphicalModel model, int[] target, int [] values){

        if(target.length!=2 || values.length!=2)
            throw new IllegalArgumentException("Operation only implemented for 2 parents.");

        // Add common child
        int s = model.addVariable(model.getDomain(target).getCombinations());
        BayesianFactor fs = BayesianFactor.deterministic(
                model.getDomain(s), model.getDomain(target),
                IntStream.range(0,model.getDomain(s).getCombinations()).toArray());
        model.setFactor(s, fs);

        // Identify target value in child
        double[] fvalues = fs.filter(target[0], values[0]).filter(target[1],values[1]).getData();
        int svalue = 0;
        for(;svalue< fvalues.length; svalue++)
            if(fvalues[svalue]==1.0)
                break;

        TIntIntMap obs = new TIntIntHashMap();
        obs.put(s, svalue);
        return obs;

    }

    public static TIntIntMap modifyModelForJointQueryBin(GraphicalModel model, int[] target, int [] values){

        if(target.length!=2 || values.length!=2)
            throw new IllegalArgumentException("Operation only implemented for 2 parents.");

        // Add common child
        int s = model.addVariable(2);
        int offset = model.getDomain(target).getOffset(values);
        int[] assign = new int[model.getDomain(target).getCombinations()];
        assign[offset] = 1;


        BayesianFactor fs = BayesianFactor.deterministic(
                model.getDomain(s), model.getDomain(target),
                assign);
        model.setFactor(s, fs);

        TIntIntMap obs = new TIntIntHashMap();
        obs.put(s, 1);
        return obs;

    }
}
