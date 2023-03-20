package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ace.AceExt;
import ch.idsia.credici.model.StructuralCausalModel;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.procedure.TIntIntProcedure;

public class AceInference {

    private File networkFile;
    private StructuralCausalModel model; 
    private AceExt ace; 
    private String acePath; 

    public AceInference(String acepath) {
        this.acePath = acepath;
    }


    public File setNetwork(StructuralCausalModel network) throws IOException {
        networkFile = File.createTempFile("CrediciAceModel", ".net");

        //networkFile = new File("./abc.net");
        try(NetworkWriter writer = new NetworkWriter(networkFile, "n", "s")) {
            writer.write(network.toBnet());
        }
        this.model = network;
        return networkFile;
    }

    public void compile() {
        String[] names = IntStream.of(model.getExogenousVars()).mapToObj((a)-> "n" + a).toArray(String[]::new);
        ace = new AceExt(networkFile.getAbsolutePath(), names, acePath);
    }
   
    

    public void update(StructuralCausalModel network) {
        Map<String, List<Double>> data = new HashMap<>();
        
        for (int ex : network.getExogenousVars()) {
            BayesianFactor f = network.getFactor(ex);
            List<Double> dta = Arrays.stream(f.getData()).<Double>mapToObj(Double::valueOf).collect(Collectors.toList());
            data.put(nodeName(ex), dta);
        }
        ace.update_CPTs(data);
    }

    public double[] query(int node, TIntIntMap evidence) {
        Map<String, String> evidenceMap = new HashMap<>();
        if (evidence != null) {
            evidence.forEachEntry(new TIntIntProcedure() {    
                @Override
                public boolean execute(int n, int s) {
                    evidenceMap.put(nodeName(n), stateName(n, s));
                    return true;
                }
            });
        }
        String name = nodeName(node);
        Map<String, List<Double>> ret = ace.evaluate(Arrays.asList(name), evidenceMap);
        return ret.get(name).stream().mapToDouble(Double::doubleValue).toArray();
    }

    String stateName(int node, int state){
        return "s" + state;
    }
    String nodeName(int node) {
        return "n" + node;
    }
}
