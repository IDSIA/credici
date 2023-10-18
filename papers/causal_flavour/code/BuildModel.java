import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.LabelInfo;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BuildModel {


    public static void main(String[] args) throws IOException, CsvException {
        // If needed, update
        Path wdir = Path.of(".");
        Path modelsFolder = wdir.resolve("./models/");
        String modelname = "simple_learner";

        ////////////////

        // Build the model
        StructuralCausalModel model = loadModelFromFolder(modelsFolder.resolve(modelname + "/").toString());
        // Save the model
        IO.write(model, modelsFolder.resolve(modelname + ".uai").toString());

    }

    @NotNull
    private static StructuralCausalModel loadModelFromFolder(String modelfolderpath) throws IOException, CsvException {
        // Get label info for variables and states
        String filename = Path.of(modelfolderpath).resolve("domains.csv").toString();
        LabelInfo info = LabelInfo.from(filename);
        HashMap<Integer, String> varnames = info.getVarNames();
        HashMap<Integer, String[]> domainnames = info.getDomainNames();
        StructuralCausalModel model = parseModel(varnames, domainnames, modelfolderpath);
        return model;
    }

    @NotNull
    private static StructuralCausalModel parseModel(HashMap<Integer, String> varnames, HashMap<Integer, String[]> domainnames, String modelfolderpath) throws IOException, CsvException {
        List<String> eqFiles =
                Files.walk(Path.of(modelfolderpath, "./eqs/"))
                        .map(s -> s.toAbsolutePath().toString())
                        .filter(f -> f.endsWith(".csv"))
                        .collect(Collectors.toList());


        StructuralCausalModel model = new StructuralCausalModel();

        TIntObjectMap eqs = new TIntObjectHashMap();
        for (String filepath : eqFiles) {
            List var_Eq = getEquation(varnames, domainnames, filepath);
            eqs.put((Integer) var_Eq.get(0), var_Eq.get(1));
        }

        for (int x : eqs.keys()) {
            BayesianFactor f = (BayesianFactor) eqs.get(x);
            model.addVariable(x, f.getDomain().getCardinality(x));
            for (int y : f.getDomain().getVariables()) {
                if (x != y && !ArraysUtil.contains(y, model.getVariables())) {
                    model.addVariable(y, f.getDomain().getCardinality(y), !ArraysUtil.contains(y, eqs.keys()));
                    model.addParent(x, y);
                }
            }
            model.setFactor(x, f);
        }


        model.fillExogenousWithRandomFactors(2);
        return model;
    }

    @NotNull
    private static List getEquation(HashMap<Integer, String> varnames, HashMap<Integer, String[]> domainnames, String filepath) throws IOException, CsvException {
        String s[] = filepath.replace(".csv", "").split("/");


        String leftName = s[s.length - 1];
        List<HashMap<String, String>> data = DataUtil.fromCSVtoStrMap(filepath);

        String[] varNames = (String[]) ((HashMap) data.get(0)).keySet().toArray(String[]::new);


        int[] vars = Stream.of(varNames).mapToInt(v -> (int) CollectionTools.getKey(varnames, v)).toArray();
        int[] card = IntStream.of(vars).map(v -> ((String[]) domainnames.get(v)).length).toArray();

        int leftVar = (int) CollectionTools.getKey(varnames, leftName);
        Strides dom = new Strides(vars, card);


        BayesianFactor f = new BayesianFactor(dom);


        for (HashMap<String, String> d : data) {

            TIntIntMap paValues = new TIntIntHashMap();
            int leftValue = -1;
            for (int v : vars) {
                String valName = (String) d.get(varnames.get(v));
                int state = List.of((String[]) domainnames.get(v)).indexOf(valName);
                if (v != leftVar)
                    paValues.put(v, state);
                else
                    leftValue = state;
            }

            EquationOps.setValue(f, (TIntIntHashMap) paValues, leftVar, leftValue);

        }
        return List.of(leftVar, f);
    }
}
