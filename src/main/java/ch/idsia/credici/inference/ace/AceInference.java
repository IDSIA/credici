package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ace.AceExt;
import ch.idsia.credici.model.StructuralCausalModel;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntIntProcedure;

public class AceInference {
    private boolean table = false;
    private File networkFile;
    private StructuralCausalModel model; 
    private AceExt ace; 
    private String acePath; 

    public AceInference(String acepath) {
        this.acePath = acepath;
    }


    public File setNetwork(StructuralCausalModel network) throws IOException {
        networkFile = File.createTempFile("CrediciAceModel", ".net");
        networkFile.deleteOnExit();
        
        try(NetworkWriter writer = new NetworkWriter(networkFile, "n", "s")) {
            writer.write(network.toBnet());
        }
        this.model = network;
        return networkFile;
    }

    public void compile() {
        String[] names = IntStream.of(model.getExogenousVars()).mapToObj((a)-> "n" + a).toArray(String[]::new);
        ace = new AceExt(networkFile.getAbsolutePath(), names, acePath, table);
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

    public void update(BayesianFactor f, int U) {
        Map<String, List<Double>> data = new HashMap<>();
        List<Double> dta = Arrays.stream(f.getData()).<Double>mapToObj(Double::valueOf).collect(Collectors.toList());
        data.put(nodeName(U), dta);
        ace.update_CPTs(data);
    }

    public double[] query(int node, TIntIntMap evidence) {
        Map<String, String> evidenceMap = new HashMap<>();
        if (evidence != null) {
            evidence.forEachEntry((n,s) -> {
                evidenceMap.put(nodeName(n), stateName(n, s));
                return true;
            });
        }
        String name = nodeName(node);
        Map<String, List<Double>> ret = ace.evaluate(Arrays.asList(name), evidenceMap);
        return ret.get(name).stream().mapToDouble(Double::doubleValue).toArray();
    }

    public double pevidence( TIntIntMap evidence) {
        Map<String, String> evidenceMap = new HashMap<>();
        if (evidence != null) {
            evidence.forEachEntry((n, s) -> {
                evidenceMap.put(nodeName(n), stateName(n, s));
                return true;
            });
        }
        Map<String, List<Double>> ret = ace.evaluate(Arrays.asList(), evidenceMap);
        return ret.get("e").stream().mapToDouble(Double::doubleValue).toArray()[0];
    }

    public TIntObjectMap<double[]> getPosteriors(TIntIntMap evidence) {
        Map<String, String> evidenceMap = new HashMap<>();
        if (evidence != null) {
            evidence.forEachEntry((n, s) -> {
                evidenceMap.put(nodeName(n), stateName(n, s));
                return true;
            });
        }
        Map<String, List<Double>> ret = ace.evaluate(Arrays.asList(), evidenceMap);
        TIntObjectMap<double[]> result = new TIntObjectHashMap<>(ret.size());
        ret.entrySet().stream().forEach(x->result.put(
            nodeId(x.getKey()), 
            toDoubleArray(x.getValue())
        ));
        return result;
    }
    
    private int nodeId(String node) {
        if (node.equals("e")) {
            return -1;
        }
        node = node.substring(1);
        return Integer.parseInt(node);
    }

    private double[] toDoubleArray(Collection<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    String stateName(int node, int state){
        return "s" + state;
    }
    String nodeName(int node) {
        return "n" + node;
    }

    public double getQueryTime() { 
        return ace.getLastQueryTime();
    }
    public double getReadTime() { 
        return ace.getLastReadTime();
    }
    public double getSetupTime() { 
        return ace.getLastSetupTime();
    }

}
