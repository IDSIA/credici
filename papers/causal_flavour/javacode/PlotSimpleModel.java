import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PlotSimpleModel {
    public static void main(String[] args) throws IOException, InterruptedException, CsvException {

        // If needed, update
        Path wdir = Path.of("./papers/causal_flavour/");
        Path dataFolder = wdir.resolve("./data/");
        Path modelsFolder = wdir.resolve("./models/");
        String modelname = "simple_learner";

        /////////////

        // Get the labels from the model
        LabelInfo info = LabelInfo.from(modelsFolder.resolve(modelname+"/domains.csv"));
        HashMap<Integer,String> varnames = info.getVarNames();
        HashMap<Integer,String[]> domainnames = info.getDomainNames();

        // Read data and model
        String modelpath = modelsFolder.resolve(modelname+".uai").toString();
        StructuralCausalModel model = (StructuralCausalModel) IO.read(modelpath);
        String datapath = dataFolder.resolve(modelname+"_data.csv").toString();
        TIntIntMap[] data = DataUtil.fromCSV(datapath);


        IO.write(model, modelpath.replace(".uai","_2.uai"));

        // Plot the data counts
        System.out.println("Data counts\n=============");
        BayesianFactor counts = DataUtil.getCounts(data, model.getDomain(model.getEndogenousVars()));
        FactorUtil.print(counts, varnames, domainnames);



        for(int x : model.getEndogenousVars()){
            System.out.println("\nSE of "+varnames.get(x));
            FactorUtil.printEquation(model.getFactor(x), x, varnames, domainnames);
        }

        SparseDirectedAcyclicGraph dag = model.getNetwork();
        System.out.println(DAGUtil.getLabelledEdges(dag, varnames));

    }

}

