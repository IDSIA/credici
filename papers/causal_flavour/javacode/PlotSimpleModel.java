import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class PlotSimpleModel {
    public static void main(String[] args) throws IOException, InterruptedException, CsvException {


        // If needed, update
        Path wdir = Path.of("./papers/causal_flavour/");


        int numRuns = 100;
        int maxIter = 500;


        // Folders
        Path dataFolder = wdir.resolve("./data/");
        Path modelFolder = wdir.resolve("./models/");

        // Model and data
        String modelname = "simple_learner.uai";
        String dataname = "Simple_learner_data.csv";


        ////  Variables and states labels: this is just for displaying purposes //////

        int VH1=0, VQ1=1,	VH2=2,	VQ2=3; // endogenous
        int UL1=4, USI=5, USII=6, UL2=7, UV2=8, UH=9, UV1=10; // exogenous

        HashMap varnames = new HashMap();
        varnames.put(VH1, "VH1");
        varnames.put(VH2, "VH2");
        varnames.put(VQ1, "VQ1");
        varnames.put(VQ2, "VQ2");

        varnames.put(UL1, "UL1");
        varnames.put(USI, "USI");
        varnames.put(USII, "USII");
        varnames.put(UL2, "UL2");
        varnames.put(UV2, "UV2");
        varnames.put(UH, "UH");
        varnames.put(UV1, "UV1");

        // no=0, yes=1  ||  bad=0, none=1, good=2   || weak=0, good=1, strong=2  ||  wrong=0, right=1

        String[] no_yes = {"no","yes"};
        String[] bad_none_good = {"bad","none","good"};
        String[] weak_good_strong = {"weak","good","strong"};
        String[] wrong_right = {"wrong", "right"};

        HashMap domainnames = new HashMap();
        domainnames.put(VH1, no_yes);
        domainnames.put(VH2, no_yes);
        domainnames.put(VQ1, wrong_right);
        domainnames.put(VQ2, wrong_right);

        domainnames.put(UL1, bad_none_good);
        domainnames.put(USI, weak_good_strong);
        domainnames.put(USII, weak_good_strong);
        domainnames.put(UL2, bad_none_good);
        domainnames.put(UV2, no_yes);
        domainnames.put(UH, no_yes);
        domainnames.put(UV1, no_yes);

        /////////////// End of variables and states labels ////////////


        String datapath = dataFolder.resolve(dataname).toString();
        TIntIntMap[] data = DataUtil.fromCSV(datapath);


        String modelpath = modelFolder.resolve(modelname).toString();
        StructuralCausalModel model  = (StructuralCausalModel) IO.read(modelpath);


        // Plot the data counts
        System.out.println("Data counts\n=============");
        BayesianFactor counts = DataUtil.getCounts(data, model.getDomain(model.getEndogenousVars())).reorderDomain(VQ2,VH2,VQ1,VH1);
        FactorUtil.print(counts, varnames, domainnames);





    }
}
