import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.nio.file.Path;


import gnu.trove.map.TIntIntMap;

import java.nio.file.Path;

public class Qfactorisation {
    public static void main(String[] args) throws IOException, CsvException {

        int T = 0;  //  Treatment
        int S = 1;  // Survival
        int G = 2;  // Gender

        int Ut = 3;

        // states for G, T and S
        int female=1, drug=1, survived=1;
        int male=0, no_drug=0, dead=0;

        String wdir = ".";  // NOTE: The working directory should be project folder
        String dataPath = Path.of(wdir, "./papers/journalPGM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        TIntIntMap[] data = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());
        StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());


        // Empirical model from a SCM and a dataset
        BayesianNetwork empNet1 = model.getEmpiricalNet(data);

        // Empirical model from a FS-SCM
        BayesianNetwork empNet2 = model.getEmpiricalNet();


        // From a FS-SCM, get the c-factors associated to each endogenous variables (as a hashmap)
        model.getCFactorsSplittedMap();

        // From a dataset and the causal structure in a model, get the c-factors associated to each endogenous variables (as a hashmap)
        DataUtil.getCFactorsSplittedMap(model, data);

        // Get a list with all the c-factors (one per endogenous variable) associated to the c-component of Ut in a FS-SCM
        model.getCFactorsSplitted(Ut);

        // The log-likelihood of a SCM wrt to a dataset
        double llk = model.logLikelihood(data);

        // The maximum log-likelihood of a SCM wrt to a dataset
        Probability.maxLogLikelihood(model, data);


    }
}
