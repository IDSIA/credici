package ch.idsia.credici.model.tools;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Logger;
import gnu.trove.map.TIntIntMap;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsModel {
    StructuralCausalModel model = null;
    TIntIntMap[] data = null;
    HashMap<String, String> info = null;
    public StatisticsModel(StructuralCausalModel model, TIntIntMap[] data){
        this.model = model;
        this.data = data;
    }

    public StatisticsModel(StructuralCausalModel model){
        this.model = model;
    }


    public static StatisticsModel of(StructuralCausalModel model) {
        return new StatisticsModel(model);
    }
    public static StatisticsModel of(StructuralCausalModel model, TIntIntMap[] data) {
        return new StatisticsModel(model, data);
    }

    public double avgExoCardinality(){
        return Arrays.stream(model.getExogenousVars())
                .map(u-> this.model.getDomain(u).getCardinality(u))
                .average().getAsDouble();
    }

    public int maxExoCardinality(){
        return Arrays.stream(model.getExogenousVars())
                .map(u-> this.model.getDomain(u).getCardinality(u))
                .max().getAsInt();
    }


    public void logStatistics(Logger logger){
        if(info == null)
            getInfo();
        for(String k : info.keySet())
            logger.info(" - "+k+" : "+info.get(k));
    }

    public HashMap<String, String> getInfo(){
        if(info != null)
            return info;

        info = new HashMap<>();
        info.put("avg_exo_card", String.valueOf(this.avgExoCardinality()));
        info.put("max_exo_card", String.valueOf(this.maxExoCardinality()));
        info.put("endo_dag", model.getEndogenousDAG().toString().replace(","," "));
        info.put("exo_dag", model.getExogenousDAG().toString().replace(","," "));
        info.put("dag", model.getNetwork().toString().replace(","," "));
        info.put("num_exo_vars", String.valueOf(model.getExogenousVars().length));
        info.put("markovianity", String.valueOf(CausalGraphTools.getMarkovianity(model.getNetwork())));
        info.put("avg_indegree", String.valueOf(DAGUtil.avgIndegree(model.getNetwork())));
        info.put("avg_endo_indegree", String.valueOf(DAGUtil.avgIndegree(model.getEndogenousDAG())));

        String exoCC = model.exoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("")).replace(",","");
        String endoCC = model.endoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining("")).replace(",","");
        info.put("exo_cc", exoCC);
        info.put("endo_cc", endoCC);

        if(data!=null) {
            info.put("ratio", String.valueOf(model.ratioLogLikelihood(data)));
            info.put("datasize", String.valueOf(data.length));
        }

        addTreeWidthInfo();


        return info;
    }

    private void addTreeWidthInfo() {
        try {
            info.put("exo_tw", String.valueOf(model.getExogenousTreewidth()));
        }catch (Exception e){}
        try {
            info.put("endo_tw", String.valueOf(model.getEndogenousTreewidth()));
        }catch (Exception e){}
        try {

            info.put("tw", String.valueOf(model.getTreewidth()));
        }catch (Exception e){}
    }


}
