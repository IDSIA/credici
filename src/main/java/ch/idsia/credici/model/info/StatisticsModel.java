package ch.idsia.credici.model.info;

import ch.idsia.credici.model.StructuralCausalModel;

import java.util.Arrays;

public class StatisticsModel {
    StructuralCausalModel model = null;
    public StatisticsModel(StructuralCausalModel model){
        this.model = model;
    }

    public static StatisticsModel of(StructuralCausalModel model) {
        return new StatisticsModel(model);
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


}
