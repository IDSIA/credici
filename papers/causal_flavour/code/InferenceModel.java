import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.LabelInfo;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InferenceModel {
    public static void main(String[] args) throws IOException, InterruptedException, CsvException {
        // Learning parameters
        int numRuns = 100;
        int maxIter = 500;

        // If needed, update
        Path wdir = Path.of(".");
        Path dataFolder = wdir.resolve("./data/");
        Path modelsFolder = wdir.resolve("./models/");
        Path outputFolder = wdir.resolve("./learntmodels/");

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


        /// Load models
        String folderpath = outputFolder.resolve("./"+modelname+"/").toString();

        // Load learnt models
        List<StructuralCausalModel> models = getModels(folderpath);

        // Inference engine
        CausalMultiVE inf = new CausalMultiVE(models);


        ///// Determine the cause and effect variables together with the positive and negative states
        // (VH1,VQ1) no_yes wrong_right
        int X = CollectionTools.getKey(varnames, "VH1");    // Cause
        int Y = CollectionTools.getKey(varnames, "VQ1");    // Effect

        int xtrue = List.of(domainnames.get(X)).indexOf("yes");
        int xfalse = List.of(domainnames.get(X)).indexOf("no");

        int ytrue = List.of(domainnames.get(Y)).indexOf("right");
        int yfalse = List.of(domainnames.get(Y)).indexOf("wrong");


        //// Examples of queries

        VertexFactor res = null;

        // Interventional query P(Y|do(xtrue))
        res = (VertexFactor) inf.causalQuery().setTarget(Y).setIntervention(X,xtrue).run();
        System.out.println(res);


        // Counterfactual query P(Y_xtrue|xfalse)
        res = (VertexFactor) inf.counterfactualQuery().setTarget(Y).setIntervention(X,xtrue).setEvidence(X,xfalse).run();
        System.out.println(res);

        // Probability of necessity
        res = (VertexFactor) inf.probNecessity(X,Y,xtrue,xfalse,ytrue,yfalse);
        System.out.println(res);

        // Probability of sufficiency
        res = (VertexFactor) inf.probSufficiency(X,Y,xtrue,xfalse,ytrue,yfalse);
        System.out.println(res);

    }

    @NotNull
    private static List getModels(String folderpath) throws IOException {
        List models = Files.walk(Path.of(folderpath))
                .map(s->s.toAbsolutePath().toString())
                .filter(f -> f.endsWith(".uai"))
                .map(p -> {
                    try {
                        return IO.read(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return models;
    }

}
