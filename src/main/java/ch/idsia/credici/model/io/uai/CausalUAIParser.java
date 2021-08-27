package ch.idsia.credici.model.io.uai;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.io.TypesIO;
import ch.idsia.crema.model.io.uai.*;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.DoubleStream;

public class CausalUAIParser extends NetUAIParser<StructuralCausalModel> {


    private double[][] probs;

    public CausalUAIParser(List<String> lines) throws IOException {
        super(lines);
        TYPE = UAITypes.CAUSAL;
    }

    public CausalUAIParser(String file) throws IOException {
        super(file);
        TYPE = UAITypes.CAUSAL;
    }




    public static StructuralCausalModel read(String filename) throws IOException {

        List<String> lines = readLines(filename);

        TypesIO type = null;

        if(filename.endsWith(".uai")) {
            // Extract the type to know the required parser
            String str = lines.get(0);
            int i = str.indexOf(" ");
            if (i > 0) type = UAITypes.valueOf(str.substring(0, i));
            else type = UAITypes.valueOfLabel(str);
        }else{
            throw new IllegalArgumentException("Unknown file extension");
        }
        StructuralCausalModel parsedObject = null;

        // Parse the file
        if (type == UAITypes.CAUSAL) {
            parsedObject = new CausalUAIParser(lines).parse();
        }else {
            throw new IllegalArgumentException("Unknown type to be parsed");
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

                BayesianFactor f = BayesianFactorFactory.factory()
                        .domain(model.getDomain(u))
                        .data(probs[u])
                        .get();
                model.setFactor(u, f);
            }
        }
        return model;

    }


    private boolean isExogenous(int v){
        return this.parents[v].length == 0;
    }



    public static void main(String[] args) throws IOException {
        StructuralCausalModel model = (StructuralCausalModel) IO.read("./models/simple-scm.uai");

        for(int v:model.getVariables()){
            System.out.println(model.getFactor(v));
        }

        System.out.println(model.getProb(2));

    }


}
