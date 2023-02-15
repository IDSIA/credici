package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;

/** Example with model from:
 *  Mueller, S., Li, A., Pearl, J., 2021. Causes of effects: Learning individual
 *  responses from population data. arXiv:2104.13730 .
 *
 */

public class MuellerExampleBias {
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

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
        TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());


        // Set the hidden configurations
        int[][] hidden = {
                {no_drug, survived, male},
                {no_drug, dead, male},
                {drug, survived, female},
                {drug, dead, female},
        };
        int[] Sassig = SelectionBias.getAssignmentWithHidden(model, new int[]{T,S,G}, hidden);


        // Get the extended biased model and the biased data
        StructuralCausalModel modelBiased = SelectionBias.addSelector(model, new int[]{T,S,G}, Sassig);
        int Svar = SelectionBias.findSelector(modelBiased);
        TIntIntMap[] biasedData = SelectionBias.applySelector(dataObs, modelBiased, Svar);

        EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, biasedData)
                .setNumTrajectories(20)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(200)
                .build();

        CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor resBiased = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);
        System.out.println(resBiased);

    }
}
