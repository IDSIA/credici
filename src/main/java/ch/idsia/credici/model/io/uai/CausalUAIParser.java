package ch.idsia.credici.model.io.uai;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.io.TypesIO;
import ch.idsia.crema.model.io.uai.*;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.DoubleStream;

public class CausalUAIParser extends NetUAIParser<StructuralCausalModel> {


    private double[][] probs;

    public static boolean ignoreChecks = false;

    public CausalUAIParser(String file) throws FileNotFoundException {
        TYPE = UAITypes.CAUSAL;
        this.bufferedReader = initReader(file);
    }

    public CausalUAIParser(BufferedReader reader) {
        TYPE = UAITypes.CAUSAL;
        this.bufferedReader = reader;
    }

    public static StructuralCausalModel read(String fileName) throws IOException {
        BufferedReader buff = initReader(fileName);
        TypesIO type = null;

        if(fileName.endsWith(".uai")) {
            // Extract the type to know the required parser
            String str = buff.readLine().replaceAll("[ \\t\\n]+","");
            int i = str.indexOf(" ");
            if (i > 0) type = UAITypes.valueOf(str.substring(0, i));
            else type = UAITypes.valueOfLabel(str);
            // rest the buffer
            buff = initReader(fileName);
        }else{
            throw new IllegalArgumentException("Unknown file extension");
        }
        StructuralCausalModel parsedObject = null;

        try{
            // Parse the file
            if (type == UAITypes.CAUSAL) {
                parsedObject = new CausalUAIParser(buff).parse();
            }else {
                throw new IllegalArgumentException("Unknown type to be parsed");
            }
        }catch (Exception e){
            buff.close();
            throw e;
        }

        return parsedObject;

    }



    @Override
    protected void processFile() {
        parseType();
        parseVariablesInfo();
        parseDomainsLastIsHead();
        parseFactors();
    }

    @Override
    protected void sanityChecks() {
        if(!ignoreChecks)
            super.sanityChecks();
    }

    private void parseFactors(){
        // Parse the probability values and store them in a 1D array
        // for each factor
        probs = new double[numberOfVariables][];
        for(int i=0; i<numberOfVariables;i++){
            int numValues = Integer.max(0, popInteger());
            probs[i] = new double[numValues];
            for(int j=0;j<numValues;j++){
                probs[i][j] = popDouble();
            }
        }
    }

    @Override
    protected StructuralCausalModel build() {
        StructuralCausalModel model = new StructuralCausalModel();

        for(int v=0; v<numberOfVariables; v++) {
            model.addVariable(cardinalities[v], isExogenous(v));
        }

        for(int v=0; v<numberOfVariables; v++) {
            model.addParents(v, ArraysUtil.reverse(parents[v]));
        }

        // Add deterministic CPTs to Xs
        for(int x : model.getEndogenousVars()){
            if(probs[x].length>0) {
                model.setFactor(x, EquationBuilder.of(model)
                                    .fromVector(x,
                                            DoubleStream.of(probs[x])
                                                    .mapToInt(i -> (int) i).toArray()));
            }
        }

        // Add marginal CPTs to Us
        for(int u: model.getExogenousVars()){
            if(probs[u].length>0) {
                model.setFactor(u, new BayesianFactor(model.getDomain(u), probs[u]));
            }
        }
        return model;

    }


    private boolean isExogenous(int v){
        return this.parents[v].length == 0;
    }


//
//    public static void main(String[] args) throws IOException {
//        StructuralCausalModel model = (StructuralCausalModel) IO.read("./models/simple-scm.uai");
//
//        for(int v:model.getVariables()){
//            System.out.println(model.getFactor(v));
//        }
//
//        System.out.println(model.getProb(2));
//
//    }


}
