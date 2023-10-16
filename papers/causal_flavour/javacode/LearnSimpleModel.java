import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class LearnSimpleModel {
    public static void main(String[] args) throws IOException, InterruptedException, CsvException {

        // If needed, update
        Path wdir = Path.of("./papers/causal_flavour/");

        // Learning parameters
        int numRuns = 100;
        int maxIter = 500;


        // Folders
        Path dataFolder = wdir.resolve("./data/");
        Path modelFolder = wdir.resolve("./models/");
        Path outputFolder = wdir.resolve("./learntmodels/");

        // Model and data
        String modelname = "simple_learner.uai";
        String dataname = "Simple_learner_data.csv";



        int VH1=0, VQ1=1,	VH2=2,	VQ2=3; // endogenous
        int UL1=4, USI=5, USII=6, UL2=7, UV2=8, UH=9, UV1=10; // exogenous

        HashMap varnames = new HashMap();
        varnames.put("VH1", VH1);
        varnames.put("VH2", VH2);
        varnames.put("VQ1", VQ1);
        varnames.put("VQ2", VQ2);

        varnames.put("UL1", UL1);
        varnames.put("USI", USI);
        varnames.put("USII", USII);
        varnames.put("UL2", UL2);
        varnames.put("UV2", UV2);
        varnames.put("UH", UH);
        varnames.put("UV1", UV1);

        String datapath = dataFolder.resolve(dataname).toString();
        TIntIntMap[] data = DataUtil.fromCSV(datapath);

        String modelpath = modelFolder.resolve(modelname).toString();
        StructuralCausalModel model  = (StructuralCausalModel) IO.read(modelpath);

        CausalEMVE inf = new CausalEMVE(model, data, numRuns, maxIter);


        // Save the models
        for(int i=0; i<inf.getInputModels().size(); i++) {
            String filepath = String.valueOf(outputFolder.resolve(modelname.replace(".uai","_"+i+".uai")));
            IO.write(inf.getInputModels().get(i), filepath);
        }
    }
}
