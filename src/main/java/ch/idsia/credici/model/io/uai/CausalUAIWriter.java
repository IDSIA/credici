package ch.idsia.credici.model.io.uai;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
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


    public static void write(Object target, String fileName) throws IOException {

        UAIWriter writer = null;
        try{
            if(CausalUAIWriter.isCompatible(target))
                writer =  new CausalUAIWriter((StructuralCausalModel) target, fileName);
            else
                throw new IllegalArgumentException("Unknown type to write");
            writer.writeToFile();
        }catch (Exception e){
            if(writer!=null) writer.getWriter().close();
            throw e;
        }
        if(writer!=null) writer.getWriter().close();
    }




    public CausalUAIWriter(StructuralCausalModel target, String file) throws IOException {
        this.target = target;
        TYPE = CAUSAL;
        this.writer = initWriter(file);

    }
    public CausalUAIWriter(StructuralCausalModel target, BufferedWriter writer){
        this.target = target;
        TYPE = CAUSAL;
        this.writer = writer;
    }


    @Override
    protected void sanityChecks() {
        return;
    }

    @Override
    protected void writeFactors() throws IOException {


        tofileln("");
        for(int v : target.getVariables()) {

            BayesianFactor f =  target.getFactor(v);

            if(f != null){
                if(target.isEndogenous(v)) {
                   int[] assig = target.getFactor(v).getAssignments(target.getParents(v));
                   tofile(assig.length+"\t");
                   tofileln(assig);
                }else{
                    double[] probs = target.getFactor(v).getData();
                    tofile(probs.length+"\t");
                    tofileln(probs);

                }
            }else{
                tofileln(0);
            }
        }

    }

    @Override
    protected void writeTarget() throws IOException {
        writeType();
        writeVariablesInfo();
        writeDomains();
        writeFactors();
    }


    @Override
    protected void writeDomains() throws IOException {
        // Write the number of factors
        tofileln(target.getVariables().length);
        // Add the factor domains with children at the end
        for(int v: target.getVariables()){
            int[] parents = ArraysUtil.reverse(target.getParents(v));
            tofile(parents.length+1+"\t");
            tofile(parents);
            tofileln(v);
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
