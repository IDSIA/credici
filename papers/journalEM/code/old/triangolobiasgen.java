package code.old;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.utility.ArraysUtil;

import java.io.IOException;
import java.nio.file.Path;

public class triangolobiasgen {
    public static void main(String[] args) throws IOException {


        Logger logger = new Logger();

        String wdir = ".";
        String modelfolder = "papers/journalEM/models/";
        String modelname = "triangolo_causal.uai";

        Path modelpath = Path.of(wdir, modelfolder, modelname);

        StructuralCausalModel model = (StructuralCausalModel) IO.read(modelpath.toString());
        logger.info("Read model from :"+modelpath.toString());


        //						0		1			2				3		4			5				6			7		   8		  9				10				11
        //String[] labels = {"Death", "Symptoms", "PPreference", "FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"


        int Karnofsky = 10;
        int Symptoms = 1;

        int[] Sparents = new int[]{Karnofsky, Symptoms};


        //// SOFT BIAS ////

        // Non available configurations
        int[][] hidden_conf = new int[][]{{1,1}};

        // Model  with Selection Bias structure
        StructuralCausalModel modelBiased = SelectionBias.addSelector(model, Sparents, hidden_conf);
        int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];

        FactorUtil.print(modelBiased.getFactor(selectVar).filter(24,1).filter(25,0));

        String output = Path.of(wdir, modelfolder, modelname.replace(".uai", "_biassoft.uai")).toAbsolutePath().toString();
        IO.write(modelBiased, output);

        logger.info("Saved model to :"+output);

        //// HARD BIAS ////

        // Non available configurations
        hidden_conf = new int[][]{{1,1}, {1,0}, {0,1}};

        // Model  with Selection Bias structure
        modelBiased = SelectionBias.addSelector(model, Sparents, hidden_conf);
        selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];

        FactorUtil.print(modelBiased.getFactor(selectVar).filter(24,1).filter(25,0));

        output = Path.of(wdir, modelfolder, modelname.replace(".uai", "_biashard.uai")).toAbsolutePath().toString();
        IO.write(modelBiased, output);

        logger.info("Saved model to :"+output);

    }
}
