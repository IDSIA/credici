import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.change.CardinalityChange;
import ch.idsia.crema.model.change.NullChange;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Example of a non-canonical SCM where the bounds are
 * tight wrt. the Tian & Pearl bounds.
 */

public class MuellerExampleNonCannonical {

    //// Numeric values identifying the variables and states in the model
    //
    static int T = 0;  //  Treatment
    static int S = 1;  // Survival
    static int G = 2;  // Gender
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {



        // states for G, T and S
        int female=1, drug=1, survived=1;
        int male=0, no_drug=0, dead=0;

        // Relevant paths (update)
        String wdir = ".";
        String dataPath = Path.of(wdir, "./papers/journalPGM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());
        StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());

        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});



        // Make the model non-canonical
        model = getReduced(model);


        BayesianFactor pXY = (BayesianFactor) DataUtil.getJointProb(dataObs, model.getDomain(T, S));
        double TPubound = pXY.filter(T,0).filter(S,0).getData()[0] + pXY.filter(T,1).filter(S,1).getData()[0];
        System.out.println("TianAndPearl result: [0,"+TPubound+"]");


        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                .setNumTrajectories(500)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(200)
                .build();

        CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor resBiased = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);
        System.out.println("EMCC:"+ resBiased);


//        CredalCausalApproxLP inf_approx =  new CredalCausalApproxLP(model,dataObs);
//        System.out.println(inf_approx.probNecessityAndSufficiency(T, S, drug, no_drug));

    }



    @NotNull
    private static StructuralCausalModel getReduced(StructuralCausalModel model) {
        //Start from the non-conservative model
        StructuralCausalModel m_reduced = model.copy();

        //int U = model.getExogenousParents(T)[0];
        int V = model.getExogenousParents(S)[0];

        // Modify the SEs and exogenous domains
        //m_reduced.removeVariable(U);
        m_reduced.removeVariable(V);
        //m_reduced.addVariable(U,3,true);
        m_reduced.addVariable(V,3,true);
        //m_reduced.addParents(T,U);
        m_reduced.addParents(S,V);

        //BayesianFactor fx = EquationBuilder.of(m_reduced).fromVector(T, 1,0, 0,0, 0,1);
        //m_reduced.setFactor(T, fx);

        BayesianFactor fy = EquationBuilder.of(m_reduced).fromVector(S, 1,0, 0,0, 0,1);


        HashMap varnames = new HashMap();
        varnames.put(T,"T");
        varnames.put(S,"S");
        varnames.put(V,"Us");

        FactorUtil.printNonZeros(fy, varnames);

        m_reduced.setFactor(S, fy);
        return m_reduced;
    }

}
