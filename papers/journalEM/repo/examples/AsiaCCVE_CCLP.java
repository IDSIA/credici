package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;

public class AsiaCCVE_CCLP {

    public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException, IOException {

        String[] varnames = {"smoke","bronc","dysp","either","lung","tub","asia","xray"};

        int smoke = 0;
        int bronc = 1;
        int dysp = 2;
        int either = 3;
        int lung = 4;
        int tub = 5;
        int asia = 6;
        int xray = 7;

        int yes = 0;
        int no = 1;

        // Relevant paths (update)
        String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
        String dataPath = Path.of(wdir, "/papers/journalEM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        BayesianNetwork bnet = (BayesianNetwork) IO.readUAI(Path.of(dataPath, "asia.uai").toString());
        TIntIntMap[] data = bnet.samples(1000, bnet.getVariables());



        StructuralCausalModel model = CausalBuilder.of(bnet.getNetwork(), 2).build();

        for(int u: model.getExogenousVars())
            System.out.println(model.getDomain(u).getCardinality(u));

        int[] Xvars = {
                asia, tub,
                smoke, bronc, lung};


        System.out.println("Building inference methods");
        CausalInference[] methods = {
                new CredalCausalVE(model, data).setConvexHull(true),
                new CredalCausalApproxLP(model, data)
        };

        String methodNames[] = {"CCVE","CCLP"};

        int y = dysp;





        for(int x : Xvars) {
            for(int i=0; i<methods.length; i++) {
                CausalInference inf = methods[i];
                double lb = Double.NaN;
                double ub = Double.NaN;
                GenericFactor res = inf.probNecessityAndSufficiency(x, y, yes, no);
                if (!(res instanceof IntervalFactor)) {
                    res = new VertexToInterval().apply((VertexFactor) res, y);
                }

                lb = ((IntervalFactor) res).getDataLower()[0][0];
                ub = ((IntervalFactor) res).getDataUpper()[0][0];
                System.out.println("PNS(" + varnames[x] + ",dysp) = [" + lb + "," + ub + "]\t ("+methodNames[i]+")");
            }

            System.out.println("");
        }

    }

}


/**

 PNS(asia,dysp) = [0.019408420462965207,0.031461678555035434]	 (CCVE)
 PNS(asia,dysp) = [0.007355162370895174,0.03146167855503542]	 (CCLP)

 PNS(tub,dysp) = [0.37081430001844107,0.4848493234685217]	 (CCVE)
 PNS(tub,dysp) = [0.2567792765683605,0.4848493234685217]	 (CCLP)


 * */