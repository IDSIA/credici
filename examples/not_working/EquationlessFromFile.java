package not_working;

import ch.idsia.credici.factor.HalfSpaceFactorBuilder;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;

// todo: fix approxLP

public class EquationlessFromFile {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Load the empirical model
        String fileName = "./models/simple-bayes.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        //Get the Credal Netoworks
        DAGModel<VertexFactor> vcredal = causalModel.toVCredal(bnet.getFactors());
        DAGModel<SeparateHalfspaceFactor> hcredal =  causalModel.toHCredal(bnet.getFactors());


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
