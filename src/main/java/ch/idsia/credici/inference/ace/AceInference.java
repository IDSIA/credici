package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ace.Ace_Ext;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.net.NetWriter;

public class AceInference {

    private File networkFile;
    private StructuralCausalModel model; 
    private Ace_Ext ace; 

    public AceInference() {
    }


    public File setNetwork(StructuralCausalModel network) throws IOException {
        networkFile = File.createTempFile("CrediciAceModel", ".uai");
        NetWriter.write(network.toBnet(), networkFile.getAbsolutePath());
        this.model = network;
        return networkFile;
    }

    public void compile() {
        String[] names = IntStream.of(model.getExogenousVars()).mapToObj((a)-> "node" + a).toArray(String[]::new);
        ace = new Ace_Ext(networkFile.getAbsolutePath(), names);
    }
   
    
}
