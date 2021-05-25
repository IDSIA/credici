package neurips21.triangolo;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class generate {
    public static void main(String[] args) throws IOException, InterruptedException {

        int seed = 233234;
        if(args.length>0)
            seed = Integer.valueOf(args[0]);


        System.out.println("seed="+seed);

        //String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";

        BayesianNetwork bnet = (BayesianNetwork) IO.read("empirical_triangolo.uai");

        int effect = 0;
        int cause = bnet.getParents(effect)[0];

        bnet.getNetwork();
        String scmFile = "triangolo_causal.uai";

        StructuralCausalModel scm = (StructuralCausalModel) IO.read(scmFile);

        RandomUtil.setRandomSeed(0);
        TIntIntMap[] data = bnet.samples(1000);
        System.out.println("Sampled");
        HashMap<Set<Integer>, BayesianFactor> empMap = DataUtil.getEmpiricalMap(scm, data);
        System.out.println(empMap);


        for(int i=0; i<5; i++) {
            Watch.start();

            int hash = (seed + "_" + i).hashCode();
            RandomUtil.setRandomSeed(hash);
            EMCredalBuilder builder = new EMCredalBuilder(scm, data, empMap)
                    .setMaxEMIter(100)
                    .setNumTrajectories(1)
                    .setVerbose(true)
                    .build();

            Watch.stopAndPrint();

            StructuralCausalModel m = builder.getSelectedPoints().get(0);
            String filename = "tr" + seed + "_" + i + ".uai";
            System.out.println(filename);
            IO.writeUAI(m, filename);
        }

    }

}
