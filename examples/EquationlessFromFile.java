import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;

public class EquationlessFromFile {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Load the empirical model
        String fileName = "./models/simple-bayes.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        //Get the Credal Netoworks
        SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal =  causalModel.toHCredal(bnet.getFactors());


        // Set query
        TIntIntMap intervention = new TIntIntHashMap();
        intervention.put(0,1);
        int target = 1;


        //Exact inference
        CredalCausalVE inf2 = new CredalCausalVE(vcredal);
        VertexFactor res2 = inf2.doQuery(target, intervention);
        System.out.println(res2);

        // Approx inference
        CredalCausalApproxLP inf = new CredalCausalApproxLP(hcredal);
        IntervalFactor res = inf.doQuery(target, intervention);
        System.out.println(res);



    }
}
