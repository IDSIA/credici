import ch.idsia.credici.inference.CredalCausalAproxLP;
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

public class Triangolo {
    public static void main(String[] args) throws IOException, InterruptedException {

        // print heap memory (change with -Xmx8g)
        System.out.println(Runtime.getRuntime().maxMemory()/Math.pow(2,20)+" MB.");

        // Load the empirical model
        String fileName = "./models/empirical_triangolo.uai";
        BayesianNetwork bnet = (BayesianNetwork) IO.read(fileName);

        System.out.println("Reading");

        // Get the markovian equationless SCM
        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);

        System.out.println("Building (Causal)");

        //Get the Credal Netoworks
        //SparseModel vcredal = causalModel.toVCredal(bnet.getFactors());
        SparseModel hcredal =  causalModel.toHCredal(bnet.getFactors());

        System.out.println("Building (Credal)");


        // Set query
        TIntIntMap intervention = new TIntIntHashMap();
        intervention.put(0,1);
        int target = 1;


        //Exact inference
        //CredalCausalVE inf2 = new CredalCausalVE(vcredal);
        //VertexFactor res2 = inf2.doQuery(target, intervention);
        //System.out.println(res2);

        System.out.println("Computing (Credal)");

        // Approx inference
        CredalCausalAproxLP inf = new CredalCausalAproxLP(hcredal);
        IntervalFactor res = inf.doQuery(target, intervention);
        System.out.println(res);

        System.out.println("Computing (Credal2)");


    }
}
