package ch.idsia.credici.learning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.inference.ace.AceInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


public class WeightedCausalEM extends FrequentistCausalEM {


    public WeightedCausalEM(GraphicalModel<BayesianFactor> model) {
        super(model);
    }


    @Override
    public void step(Collection stepArgs) throws InterruptedException {
        stepPrivate(stepArgs);

        performedIterations++;
        if(recordIntermediate)
            addIntermediateModels(posteriorModel);

    }

    @Override
    public void run(Collection stepArgs, int iterations) throws InterruptedException {
        setData((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));

        List<Pair> pairs = Arrays.asList(DataUtil.getCounts(data));
        super.run(pairs, iterations);
    }



    @Override
    protected void stepPrivate(Collection stepArgs) throws InterruptedException {
        try {
            // E-stage
            TIntObjectMap<BayesianFactor> counts = expectation(stepArgs);
            // M-stage
            maximization(counts);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    NumberFormat nf = NumberFormat.getNumberInstance();
    protected TIntObjectMap<BayesianFactor> expectation(Collection<Pair> dataWeighted) throws InterruptedException, IOException {

        TIntObjectMap<BayesianFactor> counts = new TIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            counts.put(variable, new BayesianFactor(posteriorModel.getFactor(variable).getDomain(), false));
        }

        clearPosteriorCache();
        if (inferenceVariation == 5 && this.method != null){
            this.method.set((StructuralCausalModel) posteriorModel);
        }


        for(Pair p : dataWeighted){
            TIntIntMap observation = (TIntIntMap) p.getLeft();
            long w = ((Long) p.getRight()).longValue();

            if (inferenceVariation == 5 && this.method != null) {
                try{
                    this.method.update(observation);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    StructuralCausalModel scm = (StructuralCausalModel) posteriorModel;
                    for (int e : scm.getExogenousVars()) {
                        BayesianFactor bf = scm.getFactor(e);

                        System.out.print("Var " + e + "\t");
                        System.out.println(Arrays.stream(bf.getData()).<String>mapToObj(nf::format).collect(Collectors.joining(",")));
                    }
                    var ace = new AceInference("src/resources/ace");
                    File model = ace.init(scm, true);
                    System.out.println(model);

                    String buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<instantiation date=\"Jun 4, 2005 7:07:21 AM\">\n";
                    for (var k : observation.keySet().toArray()) {

                        String var_name = "n"+k;
                        String val = "s" + observation.get(k);
                        buffer += "<inst id=\"";
                        buffer += var_name;
                        buffer += "\" value=\"";
                        buffer += val;
                        buffer += "\"/>\n";
                    }
                    buffer += "</instantiation>\n";
                    Files.writeString(Path.of("n1.inst"), buffer);
                    System.exit(-1);
                }
            }

            for (int var : trainableVars) {

                int[] relevantVars = ArraysUtil.addToSortedArray(posteriorModel.getParents(var), var);
                int[] hidden =  IntStream.of(relevantVars).filter(x -> !observation.containsKey(x)).toArray();

                if(hidden.length>0){
                    // Case with missing data
                    BayesianFactor phidden_obs = posteriorInference(hidden, observation);
                    phidden_obs = phidden_obs.scalarMultiply(w);
                    //System.out.println(phidden_obs);
                    counts.put(var, counts.get(var).addition(phidden_obs));
                }else{
                    //fully-observable case
                    for(int index : counts.get(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getValueAt(index) + w;
                        counts.get(var).setValueAt(x, index);
                    }
                }
            }
        }

        return counts;
    }

}

