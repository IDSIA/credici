import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.LabelInfo;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class LearnModel {
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


        CausalEMVE inf = new CausalEMVE(model, data, numRuns, maxIter);
        // Save the models
        for(int i=0; i<inf.getInputModels().size(); i++) {
            String filepath = String.valueOf(outputFolder.resolve(modelname+"/model_"+i+".uai"));
            System.out.println(filepath);
            IO.write(inf.getInputModels().get(i), filepath);
        }
    }
}
