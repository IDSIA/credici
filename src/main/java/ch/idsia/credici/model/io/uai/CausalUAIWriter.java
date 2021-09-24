package ch.idsia.credici.model.io.uai;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.Operations;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianDefaultFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.io.uai.HCredalUAIWriter;
import ch.idsia.crema.model.io.uai.NetUAIWriter;
import ch.idsia.crema.model.io.uai.UAIWriter;
import ch.idsia.crema.model.io.uai.VCredalUAIWriter;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import org.apache.commons.math3.optim.linear.LinearConstraint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static ch.idsia.credici.model.io.uai.UAITypes.CAUSAL;

public class CausalUAIWriter extends NetUAIWriter<StructuralCausalModel> {

/*
    public static void write(Object target, String fileName) throws IOException {

        if(CausalUAIWriter.isCompatible(target))
            new CausalUAIWriter((StructuralCausalModel) target, fileName).write();
        else
            throw new IllegalArgumentException("Unknown type to write");

    }
 */

    public CausalUAIWriter(StructuralCausalModel target, String filename) {
        super(target, filename);
        this.target = target;
        TYPE = CAUSAL;

    }


    @Override
    protected void sanityChecks() {
        return;
    }

    @Override
    protected void writeFactors() {


        append("");
        for(int v : target.getVariables()) {

            BayesianFactor f =  target.getFactor(v);

            if(f != null){
                if(target.isEndogenous(v)) {
                   int[] assig = Operations.getAssignments(target.getFactor(v), target.getParents(v));
                   append(assig.length+"\t");
                   append(assig);
                }else{
                    double[] probs = target.getFactor(v).getData();
                    append(probs.length+"\t");
                    append(probs);

                }
            }else{
                append(0);
            }
        }

    }

    @Override
    protected void writeTarget(){
        writeType();
        writeVariablesInfo();
        writeDomains();
        writeFactors();
    }


    @Override
    protected void writeDomains() {
        // Write the number of factors
        append(target.getVariables().length);
        // Add the factor domains with children at the end
        for(int v: target.getVariables()){
            int[] parents = ArraysUtil.reverse(target.getParents(v));
            append(parents.length+1+"\t");
            append(parents);
            append(v);
        }
    }


    public static boolean isCompatible(Object target){
        return target instanceof StructuralCausalModel;
    }



    public static void main(String[] args) throws IOException {
        String fileName = "./models/simple-scm";

        StructuralCausalModel model;

        model = (StructuralCausalModel) IO.read(fileName+".uai");
        IO.write(model, fileName+"_2.uai");
        model = (StructuralCausalModel) IO.read(fileName+"_2.uai");
        IO.write(model,fileName+"_3.uai");

    }



}
