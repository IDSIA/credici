import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.Arrays;
import java.util.HashMap;

public class pid19_cofounded {
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException {




        // Define the SCM structure
        StructuralCausalModel m = new StructuralCausalModel();

        //Nodes and arcs
        int X = m.addVariable(2, false);
        int Y = m.addVariable(2, false);
        int V = m.addVariable(2, true);
        int U = m.addVariable(4, true);
        m.addParents(Y, U, X);
        m.addParents(X, V);





        // Define the equation P(X|U)
        m.setFactor(X, BayesianFactor.deterministic(
                        m.getDomain(X), 		// children domain
                        m.getDomain(V),			// parents domain
                        0, 1));     // assignments

        // Define the equation P(Y|X,U)
        m.setFactor(Y, BayesianFactor.deterministic(
                        m.getDomain(Y),
                        m.getDomain(m.getParents(Y)),
                        0,1, 0,1, 1,0, 0,0));

        m = Cofounding.mergeExoParents(m, X,Y);


        EquationOps.print(m,Y);


        BayesianFactor counts1 = new BayesianFactor(m.getDomain(Y,X), new double[]{4,6,6,4});
        TIntIntMap[] data1 = DataUtil.dataFromCounts(counts1, true);
        HashMap emp1 = DataUtil.getEmpiricalMap(m, data1);

        BayesianFactor counts2 = new BayesianFactor(m.getDomain(Y,X), new double[]{40, 60, 58, 42});
        TIntIntMap[] data2 = DataUtil.dataFromCounts(counts2, true);
        HashMap emp2 = DataUtil.getEmpiricalMap(m, data2);




        // Obtain the equivalent credal model with vertices
        m.isCompatible(data1);   // true
        m.isCompatible(data2) ;  // false


        // Exact inference
        CredalCausalVE infExact = new CredalCausalVE(m, emp1.values());
        VertexFactor resExact = (VertexFactor) infExact.counterfactualQuery()
                .setTarget(Y)
                .setIntervention(X,0)
                .setEvidence(X,1)
                .run();

        System.out.println("Result exact: "+resExact);
        VertexFactor pnsExact = (VertexFactor) infExact.probNecessityAndSufficiency(X,Y);
        System.out.println("pns exact: "+pnsExact);

        EMCredalBuilder builder = EMCredalBuilder.of(m, data2)
                .setWeightedEM(true)
                .setMaxEMIter(200)
                .setNumTrajectories(10);

        builder.build();
        CausalMultiVE infApprox = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor resApporx = (VertexFactor) infApprox
                .counterfactualQuery()
                .setTarget(Y)
                .setIntervention(X,0)
                .setEvidence(X,1)
                .run();     // K(vars[5]|[]) [0.41000000000825193, 0.5899999999917482]

        System.out.println(resApporx);


        VertexFactor pnsApprox = (VertexFactor) infApprox.probNecessityAndSufficiency(X,Y);
        System.out.println("pns approx: "+pnsApprox);

        System.out.println(resApporx.sampleVertex());

        builder.getSelectedPoints().stream().map(p -> ArraysUtil.remove(p.getFactor(U).getData(), 3)).forEach(p -> System.out.println(Arrays.toString(p)));
    /*

    [0.11417010960685407, 0.29582989038495655, 0.5899999999917482]
    [0.015136515992427659, 0.3948634839993429, 0.5899999999917078]
    [0.009812622837741945, 0.4001873771541247, 0.5899999999918046]
    [0.40490754848682137, 0.005092451508638452, 0.5899999999954252]
    [0.1303067863831306, 0.2796932136086044, 0.589999999991672]
    [0.39558146193991206, 0.014418538051803603, 0.5899999999916524]
    [0.2546091298078591, 0.15539087018545764, 0.5899999999932657]
    [0.3594649030079599, 0.05053509698388274, 0.5899999999917804]
    [0.29751046002641307, 0.1124895399658563, 0.5899999999922104]
    [0.21804107707804815, 0.19195892291387198, 0.5899999999918585]

    */



    /* todo for talk:
        include EM with compatible data
        include initial points and paths
     */





    }
}
