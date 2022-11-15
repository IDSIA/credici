package ch.idsia.credici.utility.reconciliation;

import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IntegrationChecker {

    List<DataIntegrator> integrators = new ArrayList<>();

    List<TIntIntMap> interventions = null;

    boolean compiled = false;

    List<StructuralCausalModel> solutions;

    // Inference parameters
    int maxIter = 200;
    double threshold = 0.00000000001;

    public IntegrationChecker(StructuralCausalModel obsModel, TIntIntMap[] obsData, TIntIntMap[] interventions, TIntIntMap[][] intDatasets) {


        this.interventions = new ArrayList<>(List.of(interventions));
        this.interventions.add(0,new TIntIntHashMap());


        // Global integrator
        DataIntegrator Ig = DataIntegrator.of(obsModel);
        Ig.setObservationalData(obsData);
        for(int i = 0; i< interventions.length; i++)
            Ig.setData(intDatasets[i], interventions[i]);

        integrators.add(Ig.compile());

        // Local integrators
        for(int i = 0; i< this.interventions.size(); i++){

            DataIntegrator Il = DataIntegrator.of(obsModel);
            TIntIntMap[] data = null;
            if(this.interventions.get(i).size()==0)
                data = obsData;
            else
                data = intDatasets[i-1];
            Il.setData(data, this.interventions.get(i));
            integrators.add(Il.compile());
        }

    }


    private StructuralCausalModel generateSolution(DataIntegrator integrator) throws InterruptedException {
        EMCredalBuilder builder = EMCredalBuilder.of(integrator.getExtendedModel(), integrator.getExtendedData())
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(threshold)
                .setNumTrajectories(1)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxIter)
                .build();
        return builder.getSelectedPoints().get(0);
    }
    private void generateSolutions() throws InterruptedException {
        solutions = new ArrayList<>();
        for(DataIntegrator I : integrators)
            solutions.add(generateSolution(I));
    }

    public IntegrationChecker compile() throws InterruptedException {
        generateSolutions();
        compiled = true;
        return this;
    }
    private void requireCompiled() throws InterruptedException {
        if(!compiled) compile();
    }

    public DataIntegrator getGlobalIntegrator(){
        int index = getGlobalIntegratorIndex();
        if(index>=0)
            return integrators.get(index);
        return null;
    }

    public DataIntegrator getLocalIntegrator(TIntIntMap inter){
        int index = getLocalIntegratorIndex(inter);
        if(index>=0)
            return integrators.get(index);
        return null;
    }

    public StructuralCausalModel getGlobalSolution() throws InterruptedException {
        requireCompiled();
        int index = getGlobalIntegratorIndex();
        if(index>=0)
            return solutions.get(index);
        return null;
    }

    public StructuralCausalModel getLocalSolution(TIntIntMap inter) throws InterruptedException {
        requireCompiled();
        int index = getLocalIntegratorIndex(inter);
        if(index>=0)
            return solutions.get(index);
        return null;
    }
    public int getGlobalIntegratorIndex(){

        for(int i=0; i<integrators.size(); i++) {
            DataIntegrator I = integrators.get(i);
            if (I.hasObservational() && I.hasInterventions(this.interventions.toArray(TIntIntMap[]::new)))
                return i;
        }
        return -1;
    }

    public int getLocalIntegratorIndex(TIntIntMap inter){
        for(int i=0; i<integrators.size(); i++) {
            DataIntegrator I = integrators.get(i);
            if(I.getInterventionOrder().size()==1) {
                if((inter.size()==0 && I.hasObservational()) || I.hasInterventions(inter))
                    return i;
            }
        }
        return -1;
    }



    /**
     * This method returns a pair for each possible dataset (ll, lg) where ll is the log-likelihood in
     * the local model while lg corresponds to the global one.
     * */
    public List<double[]> getLogLikelihoodPairs() throws InterruptedException {
        requireCompiled();
        List<double[]> pairs = new ArrayList<>();

        DataIntegrator Ig = getGlobalIntegrator();
        StructuralCausalModel Mg = getGlobalSolution();

        //  local models
        for(int i=0; i<interventions.size(); i++){
            DataIntegrator Il = getLocalIntegrator(interventions.get(i));
            StructuralCausalModel Ml = getLocalSolution(interventions.get(i));
            double ll = Il.logLikelihood(Ml)[0];
            double lg = Ig.logLikelihoodAt(interventions.get(i), Mg);
            pairs.add(new double[]{ll, lg});
        }

        return pairs;
    }

    /**
     * Aggregates and standardizes the differences in the log-likelihoods
     * */
    public double getMetric() throws InterruptedException {
        requireCompiled();
        List<double[]> pairs = getLogLikelihoodPairs();

        double sumDiff = 0;
        double sumGlobals = 0;
        for(double[] p : getLogLikelihoodPairs()){
            sumDiff += Math.abs(p[0] - p[1]);
            sumGlobals += Math.abs(p[1]);
        }

        return sumDiff / sumGlobals;
    }

    public IntegrationChecker setMaxIter(int maxIter) {
        this.maxIter = maxIter;
        return this;
    }

    public String valuesStr() throws InterruptedException {
        requireCompiled();
        List<double[]> pairs = getLogLikelihoodPairs();
        String out = "";
        return pairs.stream().map(p -> "("+p[0]+","+p[1]+")").collect(Collectors.joining(","));

    }

    public IntegrationChecker setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
    }
}
