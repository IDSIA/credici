package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/** Example with model from:
 *  Mueller, S., Li, A., Pearl, J., 2021. Causes of effects: Learning individual
 *  responses from population data. arXiv:2104.13730 .
 *
 */

public class MuellerExampleHybrid {

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

        int numEMruns = 100;
        int maxIter = 200;

        //// Numeric values identifying the variables and states in the model

        int T = 0;  //  Treatment
        int S = 1;  // Survival
        int G = 2;  // Gender

        // states for G, T and S
        int female=1, drug=1, survived=1;
        int male=0, no_drug=0, dead=0;

        // Relevant paths (update)
        String wdir = ".";
        String dataPath = Path.of(wdir, "./papers/journalPGM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());
        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});

        TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());

        // Load the intervened data
        TIntIntMap[] dataDoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoDrug.csv").toString());
        TIntIntMap[] dataDoNoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoNoDrug.csv").toString());
        TIntIntMap[] dataInt = DataUtil.vconcat(dataDoDrug, dataDoNoDrug);


        // Set integrator object to build the extended model and data
        DataIntegrator integrator = DataIntegrator.of(model);
        integrator.setObservationalData(dataObs);
        integrator.setData(dataInt, new int[] {T});

        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();


        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                .setNumTrajectories(numEMruns)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxIter)
                .build();

        // Rebuild a simple model with the learned parameters
        StructuralCausalModel finalModel = model;
        List selectedPoints = builder.getSelectedPoints().stream().map(m -> m.subModel(finalModel.getVariables())).collect(Collectors.toList());

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor resHybrid = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);
        System.out.println(resHybrid);



    }
}
