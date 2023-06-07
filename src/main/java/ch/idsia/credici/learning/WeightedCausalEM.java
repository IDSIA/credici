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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;

import ch.idsia.credici.collections.FIntObjectHashMap;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIWriter;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;



public class WeightedCausalEM extends FrequentistCausalEM {
    static AtomicInteger counter = new AtomicInteger();

    public WeightedCausalEM(GraphicalModel<BayesianFactor> model) {
        super(model);
        logger = Logger.getGlobal();
    }


    @Override
    public void step(Collection stepArgs) throws InterruptedException {
        stepPrivate(stepArgs);
        performedIterations++;
        if(recordIntermediate)
            addIntermediateModels(posteriorModel);

    }

    double maxll;
    @Override
    public void run(Collection stepArgs, int iterations) throws InterruptedException {
        ll = Double.NEGATIVE_INFINITY;

        setData((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));
        List<Pair> pairs = Arrays.asList(DataUtil.getCounts(data));       
        super.run(pairs, iterations);
    }
    
    Logger logger;

    @Override
    protected void stepPrivate(Collection stepArgs) throws InterruptedException {
        try {
            // E-stage
            //logger.logp(Level.INFO, "WeightedCausalEM", "stepPrivate", "Expectation");
            TIntObjectMap<BayesianFactor> counts = expectation(stepArgs);

            // M-stage
            //logger.logp(Level.INFO, "WeightedCausalEM", "stepPrivate", "Maximization");
            maximization(counts);
        } catch (Exception ex) {
            ex.printStackTrace();
            int v = counter.incrementAndGet();
        
            CausalUAIWriter writer;
            try {
                writer = new CausalUAIWriter((StructuralCausalModel) posteriorModel, "ErrorModel"+v+".uai");
                writer.writeToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new InterruptedException(ex.getMessage());
        }
    }

    NumberFormat nf = NumberFormat.getNumberInstance();
    protected TIntObjectMap<BayesianFactor> expectation(Collection<Pair> dataWeighted) throws InterruptedException, IOException {

        TIntObjectMap<BayesianFactor> counts = new FIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            counts.put(variable, new BayesianFactor(posteriorModel.getFactor(variable).getDomain(), false));
        }

        
        if (inferenceVariation == 5 && this.method != null){
            this.method.set((StructuralCausalModel) posteriorModel);
		} else {
            clearPosteriorCache();
        }

        ll = 0;

        for(Pair p : dataWeighted){
            TIntIntMap observation = (TIntIntMap) p.getLeft();
            long w = ((Long) p.getRight()).longValue();

            if (inferenceVariation == 5 && this.method != null) {
                try {
                    this.method.update(observation);
                } catch(Exception ex) {
                    
                    ex.printStackTrace();
                    // StructuralCausalModel scm = (StructuralCausalModel) posteriorModel;
                    // for (int e : scm.getExogenousVars()) {
                    //     BayesianFactor bf = scm.getFactor(e);

                    //     System.out.print("Var " + e + "\t");
                    //     System.out.println(Arrays.stream(bf.getData()).<String>mapToObj(nf::format).collect(Collectors.joining(",")));
                    // }
                    // var ace = new AceInference("src/resources/ace");
                    // File model = ace.init(scm, true);
                    // System.out.println(model);

                    // String buffer = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<instantiation date=\"Jun 4, 2005 7:07:21 AM\">\n";
                    // for (var k : observation.keySet().toArray()) {

                    //     String var_name = "n"+k;
                    //     String val = "s" + observation.get(k);
                    //     buffer += "<inst id=\"";
                    //     buffer += var_name;
                    //     buffer += "\" value=\"";
                    //     buffer += val;
                    //     buffer += "\"/>\n";
                    // }
                    // buffer += "</instantiation>\n";
                    // Files.writeString(Path.of("n1.inst"), buffer);
                    System.exit(-1);
                }
                ll += FastMath.log(this.method.pevidence()) * w;
            }
            
            for (int var : trainableVars) {

                int[] relevantVars = ArraysUtil.addToSortedArray(posteriorModel.getParents(var), var);
                int[] hidden =  IntStream.of(relevantVars).filter(x -> !observation.containsKey(x)).toArray();

                if(hidden.length==1) {
                    // Case with missing data
                    BayesianFactor phidden_obs = posteriorInference(hidden, observation);
                    
                    
                    // multiply by the number of rows in the data
                    // we are multiplying here as we are using the P for the counts
                    phidden_obs = phidden_obs.scalarMultiply(w);

                    BayesianFactor bf = counts.get(var);
                    counts.put(var, bf.addition(phidden_obs));
                } else if (hidden.length == 0) {
                    //fully-observable case
                    for(int index : counts.get(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getValueAt(index) + w;
                        counts.get(var).setValueAt(x, index);
                    }
                } else {
                    throw new RuntimeException("Unsupported");
                }
            }
        }

        return counts;
    }

}

