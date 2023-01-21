package ch.idsia.credici.utility.reconciliation;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataIntegrator {

    /** Map of datasets (with the initial variable indexes): intervention -> data **/
    private HashMap<TIntIntMap, TIntIntMap[]> datasets;

    /** Map of datasets (with the variable indexes in de extended model): intervention -> data **/
    private HashMap<TIntIntMap, TIntIntMap[]> mappedDatasets;

    /** Order in which interventions are considered */
    private List<TIntIntMap> interventionOrder = new ArrayList<>();

    /** Observational model */
    private StructuralCausalModel obsModel;

    /** Final extended model */
    private StructuralCausalModel extendedModel;

    private StructuralCausalModel extendedMultiStudy = null;

    /** Indicates if the extended model is built */
    boolean compiled = false;

    String description = "";

    int[] studySpecificExoVars = null;

    Set<Integer> studies;

    private int studyVar = -1;

    /**
     * Constructor
     * @param observationalModel
     */
    public DataIntegrator(StructuralCausalModel observationalModel, int... studySpecificExoVars) {
        this.obsModel = observationalModel;
        datasets = new HashMap<>();
        mappedDatasets = new HashMap<>();
        studies = new HashSet<>();

        this.studySpecificExoVars = studySpecificExoVars;

    }

    /**
     * Builder from the observational model
     * @param observationalModel
     * @return
     */
    public static DataIntegrator of(StructuralCausalModel observationalModel, int... studySpecificExoVars){
        return new DataIntegrator(observationalModel, studySpecificExoVars);
    }

    /**
     * Builder from the observational data and different interventional data
     * @param model
     * @param dataObs
     * @param interventions
     * @param datasets
     * @return
     */
    public static DataIntegrator of(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets)  {

        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(datasets[i], interventions[i]);

        return integrator;


    }


    /**
     * Get the extended model for doing inference
     * @return
     */
    public StructuralCausalModel getExtendedModel(boolean removeStudyVar) {
        requireCompiled();
        if(!removeStudyVar)
            return extendedModel;
        return extendedMultiStudy;
    }

    /**
     * Get the extended model for doing inference
     * @return
     */
    public StructuralCausalModel getExtendedModel() {
        return getExtendedModel(false);
    }

    /** Get the datasets with the original index variables */
    public HashMap<TIntIntMap, TIntIntMap[]> getOriginalDatasets() {
        return datasets;
    }

    /** Set the observational data */
    public DataIntegrator setObservationalData(TIntIntMap[] data, int... study){
        // Observational data is stored together with the rest of datasets and its key is an empty intervention: {} -> obsData
        setData(data, new TIntIntHashMap(), new int[]{}, study);
        return this;
    }

    /**
     * Set the data associated to an intervention
     *
     * @param data
     * @param intervention
     * @return
     */
    public DataIntegrator setData(TIntIntMap[] data, TIntIntMap intervention, int... study){
        return setData(data, intervention, new int[]{}, study);
    }

    /**
     * Set the data associated to an intervention
     *
     * @param data
     * @param intervention
     * @return
     */
    public DataIntegrator setData(TIntIntMap[] data, TIntIntMap intervention , int [] intervenedVars, int... study){

        //if(//(!hasStudySpecificExoVars() && study.length>0) || (hasStudySpecificExoVars() && study.length !=1))
        //    throw new IllegalArgumentException("Wrong value for study");

        data = DataUtil.deepCopy(data);

        // record the study
        if(study.length>0) {
            studies.add(study[0]);
            data = Arrays.stream(data).map(t -> {
                if(t.containsKey(-1)) throw new IllegalArgumentException("Input data cannot contain key -1");
                t.put(-1, study[0]);
                return t;
            }).toArray(TIntIntMap[]::new);
        }

        // Transform the unknown variables
        for (int v: intervenedVars) {
            if (intervention.containsKey(v)) throw new IllegalArgumentException("Wrong intervention");
            intervention.put(v, -1);
        }

        if(!isApplicableIntervention(intervention)) throw new IllegalArgumentException("Wrong intervention");

        for(int v : intervention.keys())
            if (!ArraysUtil.contains(v, this.obsModel.getEndogenousVars()))
                throw new IllegalArgumentException("Wrong intervention due to variable: "+v);


        putDatasets(intervention, data);
        compiled = false;

        return this;

    }

    private void putDatasets(TIntIntMap intervention, TIntIntMap[] data) {
        if(datasets.containsKey(intervention))
            data = DataUtil.vconcat(datasets.get(intervention), data);
        datasets.put(intervention, data);
    }

    /**
     *  Set the data associated to an unknown intervention
     * @param intervenedVars
     * @return
     */
    public DataIntegrator setData(TIntIntMap[] data, int[] intervenedVars, int... study) {
        if(intervenedVars.length==0) throw new IllegalArgumentException("Wrong number of interventions");
        setData(data, new TIntIntHashMap(), intervenedVars, study);
        return this;
    }

    private boolean isApplicableIntervention(TIntIntMap intervention){
        // todo: an intervention is not applicable if match on the states except for those that are -1
        for(TIntIntMap i : this.interventionOrder){
            if(ArraysUtil.equals(i.keys(), intervention.keys(), true, false)){
                boolean notApplicable_i = IntStream.of(i.keys()).allMatch(v -> {
                    int val1 = i.get(v);
                    int val2 = intervention.get(v);
                    return val1==val2 || val1==-1 || val2==-1;
                });

                if(notApplicable_i)
                    return false;

            }
        }
        return true;

    }


    /** Get the concatenated dataset from a set of interventions (with the indexes in the extended model)
     *
     * @param intervention
     * @return
     */
    public TIntIntMap[] getMappedData(TIntIntMap... intervention){
        requireCompiled();
        return DataUtil.vconcat(Arrays.stream(intervention).map(inter -> mappedDatasets.get(inter)).toArray(TIntIntMap[][]::new));
    }

    /**
     * Get the sub-model associated to an intervention
     * @param intervention
     * @return
     */
    public StructuralCausalModel subModel(TIntIntMap intervention){

        StructuralCausalModel subModel = this.getExtendedModel().copy();
        int[] variables = getVariablesAt(intervention);
        for(int v : subModel.getVariables())
            if(!ArraysUtil.contains(v,variables)) {
                subModel.removeVariable(v);
            }

        return subModel;
    }


    /**
     * Get the dataset with the exten
     * @return
     */

    public TIntIntMap[] getExtendedData() {
        return getExtendedData(false);
    }

    /**
     * Get the dataset with the exten
     * @return
     */

    public TIntIntMap[] getExtendedData(boolean removeStudyVar){
        requireCompiled();
        TIntIntMap[] extData = getMappedData(datasets.keySet().toArray(TIntIntMap[]::new));
        if(!removeStudyVar)
            return extData;

        WorldMapping map = extendedMultiStudy.getMap();
        int S = this.getStudyVar();
        TIntIntMap[] extDataMultiStudy =
                Arrays.stream(extData).map(t -> {
                    if(!t.containsKey(S))
                        return t;
                    TIntIntMap new_t = new TIntIntHashMap();
                    int s = t.get(S);
                    for(int v:t.keys()){
                        if(v != S)
                            new_t.put(map.getEquivalentVars(s, v), t.get(v));
                    }
                    return new_t;
                }).toArray(TIntIntMap[]::new);

        return extDataMultiStudy;


    }
    public TIntIntMap[] getObservationalData(){
        return datasets.get(new TIntIntHashMap());
    }

    private TIntIntMap[] transformData(TIntIntMap intervention, TIntIntMap[] data){

        // transform study variable
        if(getNumStudies()>0){
            data = DataUtil.renameVar(data, -1, studyVar);
        }

        if(intervention.size()==0)
            return data;

        // Rename the variables in the interventional data
        int index = getInterventionIndex(intervention);
        if(!hasObservational()) index++;
        int[] newVars = extendedModel.getMap().getEquivalentVars(index, obsModel.getEndogenousVars());
        return DataUtil.renameVars(data, obsModel.getEndogenousVars(), newVars);

    }
    private int getInterventionIndex(TIntIntMap inter){
       String key = String.valueOf(inter);
       for(int i=0; i<interventionOrder.size(); i++)
           if(key.equals(String.valueOf(interventionOrder.get(i))))
               return i;
       throw new IllegalArgumentException("Wrong intervention");
    }

    public int getInterventionPosition(TIntIntMap inter){
        int index = getInterventionIndex(inter);
        if(!hasObservational()) index++;
        return index;
    }

    public int getInterventionPosition(int var){
        int index = getInterventionIndex(ObservationBuilder.observe(var, -1));
        if(!hasObservational()) index++;
        return index;
    }



    public boolean hasObservational(){
        try{
            getInterventionIndex(new TIntIntHashMap());
        }catch (Exception e){
            return false;
        }
        return true;
    }

    public boolean hasNonObservational(){
        int n = this.interventionOrder.size();
        if(hasObservational()) n--;
        return n>0;
    }

    public boolean hasInterventions(TIntIntMap ... interventions){
        requireCompiled();
        try{
            for(TIntIntMap i : interventions)
                getInterventionIndex(i);
        }catch (Exception e){
            return false;
        }
        return true;
    }

    private void transformAllData(){
        mappedDatasets  = new HashMap<>();
        for(TIntIntMap inter : datasets.keySet()){
            mappedDatasets.put(inter, transformData(inter, datasets.get(inter)));
        }

    }
    private void buildExtendedModel(){

        interventionOrder = datasets.keySet().stream().collect(Collectors.toList());

        StructuralCausalModel[] interModels = interventionOrder.stream()
                .filter(inter -> inter.size()>0)
                .map(inter -> obsModel.intervention(inter, false))
                .toArray(StructuralCausalModel[]::new);

        extendedModel = obsModel.merge(interModels);

        if(getNumStudies()>0) {
            int numStudies = Collections.max(studies) + 1;
            if(numStudies<2)
                throw new IllegalArgumentException("Number of studies must be greater than 1");
            studyVar = extendedModel.addVariable(numStudies, false);
            double[] values = IntStream.range(0,numStudies).mapToDouble(i->1.0/numStudies).toArray();
            BayesianFactor pS = new BayesianFactor(extendedModel.getDomain(studyVar), values);
            extendedModel.setFactor(studyVar, pS);
            for(int u: obsModel.getExogenousVars()) {
                extendedModel.addParent(u, studyVar);
            }

            // Extended model without StudyVar

            StructuralCausalModel m = extendedModel.copy();
            int S = this.getStudyVar();
            int N = m.getDomain(S).getCombinations();
            m.removeVariable(S);
            m.fillExogenousWithRandomFactors(2);
            extendedMultiStudy = CausalOps.plateau(m, N, this.getNonStudySpecificExoVars());

        }

    }

    public DataIntegrator compile(){
        buildExtendedModel();
        transformAllData();
        compiled = true;
        return this;
    }

    private void requireCompiled(){
        if(!compiled) compile();
    }

    private int[] getInterventionalEndoVars(){
        requireCompiled();
        return ArraysUtil.difference(extendedModel.getEndogenousVars(), getObservationalEndoVars());
    }

    public int[] getObservationalEndoVars(){
        return obsModel.getEndogenousVars();
    }

    public int[] getVariablesAt(TIntIntMap intervention){
        requireCompiled();
        int index = getInterventionIndex(intervention);
        if(!hasObservational()) index++;
        return this.getExtendedModel().getMap().getVariablesIn(index);
    }

    public StructuralCausalModel removeInterventional(StructuralCausalModel model){
        StructuralCausalModel out = model.copy();
        for(int v : getInterventionalEndoVars())
            out.removeVariable(v);
        return out;
    }

    public StructuralCausalModel removeInterventionalFromMultiStudy(StructuralCausalModel extModel, int study) {
        StructuralCausalModel m = obsModel.copy();
        WorldMapping map = extendedMultiStudy.getMap();

        for(int u: m.getExogenousVars()){
            int u_ext = map.getEquivalentVars(study, u);
            BayesianFactor f = new BayesianFactor(m.getDomain(u), extModel.getFactor(u_ext).getData());
            m.setFactor(u, f);
        }

        return m;
    }


    public double logLikelihoodAt(TIntIntMap inter, StructuralCausalModel fullModel){
        TIntIntMap[] data = this.getMappedData(inter);
        StructuralCausalModel subModel = fullModel.subModel(this.getVariablesAt(inter)).getWithFixedIntervened();
        return Probability.logLikelihood(subModel.getCFactorsSplittedMap(), data);
    }

    public double[] logLikelihood(StructuralCausalModel fullModel){
        return this.interventionOrder.stream().mapToDouble(inter -> this.logLikelihoodAt(inter, fullModel)).toArray();
    }

    public List<TIntIntMap> getInterventionOrder() {
        return interventionOrder;
    }

    public DataIntegrator setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public int[] getStudySpecificExoVars() {
        return studySpecificExoVars;
    }

    public int[] getNonStudySpecificExoVars() {
        return ArraysUtil.difference(this.obsModel.getExogenousVars(), getStudySpecificExoVars());
    }

    public boolean hasStudySpecificExoVars() {
        return studySpecificExoVars.length>0;
    }

    public int getStudyVar() {
        return studyVar;
    }

    public Set<Integer> getStudies() {
        return studies;
    }

    public int getNumStudies(){
        return studies.size();
    }

    public StructuralCausalModel getObsModel() {
        return obsModel;
    }

    public WorldMapping getMap(boolean removeStudyVar){
        requireCompiled();
        if(!removeStudyVar)
            return this.extendedModel.getMap();
        return this.extendedMultiStudy.getMap();
    }



    @Override
    public String toString() {
        return "DataIntegrator{"+
                description+
                " datasets=" + datasets.size() +
                ", interventionOrder=" + interventionOrder +
                ", obsModel=" + hasObservational()+
                ", studySpecific=" + hasStudySpecificExoVars() +
                ", compiled=" + compiled +
                '}';
    }

    public void summary(){


        boolean multiStudy = getNumStudies()>0;

        if(multiStudy){
            System.out.println("Data integrator summary\n======================");
            WorldMapping map = getMap(true);
            StructuralCausalModel extModel = getExtendedModel(true);
            int[] dataVars = DataUtil.variables(getExtendedData(true));
            System.out.println("dataVars: "+Arrays.toString(dataVars));
            for(int s : studies){
                int[] vars = map.getVariablesIn(s);
                System.out.println("Study "+s);
                System.out.println(" - endo: "+Arrays.toString(ArraysUtil.intersection(vars, extModel.getEndogenousVars())));
                System.out.println(" - exo: "+Arrays.toString(ArraysUtil.intersection(vars, extModel.getExogenousVars())));
                System.out.println(" - dag: "+extModel.subModel(vars).getNetwork());
            }
        }else {
            System.out.println(getExtendedModel(multiStudy));
            System.out.println(DataUtil.variables(getExtendedData(multiStudy)));
            //System.out.println(getMap(multiStudy));
        }



    }

    public static void main(String[] args) {

        int datasize = 10;
        RandomUtil.setRandomSeed(1);

        SparseDirectedAcyclicGraph endoDAG = DAGUtil.build("(0,1),(1,2)");
        SparseDirectedAcyclicGraph causalDAG = DAGUtil.build("(0,1),(1,2),(3,0),(4,1),(5,2)");
        StructuralCausalModel model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();
        model = Cofounding.mergeExoVars(model, new int[][]{new int[]{3,5}});
        model.fillExogenousWithRandomFactors(3);

        int x = 0, y = 1, z=2; // Variables in the model;
        int cause = x, effect = z;

        /* Only with observational data */
        TIntIntMap[] obsData = model.samples(datasize, model.getEndogenousVars());

        TIntIntMap inter0 = ObservationBuilder.observe(x,0);
        TIntIntMap[] interData0 = model.intervention(inter0).samples(datasize, model.getEndogenousVars());

        TIntIntMap inter1 = ObservationBuilder.observe(x,1);
        TIntIntMap[] interData1 = model.intervention(inter1).samples(datasize, model.getEndogenousVars());



        DataIntegrator integrator = DataIntegrator.of(model)
                //.setObservationalData(obsData)
                .setData(interData0, inter0)
                .setData(interData1, inter1)
                .compile();

        System.out.println(integrator.getExtendedModel());
        TIntIntMap[] fulldata = integrator.getExtendedData();
        System.out.println();
        StructuralCausalModel m = integrator.getExtendedModel();
        System.out.println(m);
        StructuralCausalModel m2 = integrator.removeInterventional(m);
        System.out.println(m);

        System.out.println(m2);

    }

}
