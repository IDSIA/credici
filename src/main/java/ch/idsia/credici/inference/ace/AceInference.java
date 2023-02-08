package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ace.AceExt;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.net.NetWriter;

public class AceInference {

    private File networkFile;
    private StructuralCausalModel model; 
    private AceExt ace; 
    private String acePath; 

    public AceInference(String acepath) {
        this.acePath = acepath;
    }


    public File setNetwork(StructuralCausalModel network) throws IOException {
        //networkFile = File.createTempFile("CrediciAceModel", ".uai");
        networkFile = new File("./abc.uai");
        try(NetworkWriter writer = new NetworkWriter(networkFile)) {
            writer.write(network.toBnet());
        }
        this.model = network;
        return networkFile;
    }

    public void compile() {
        String[] names = IntStream.of(model.getExogenousVars()).mapToObj((a)-> "n" + a).toArray(String[]::new);

        ace = new AceExt(networkFile.getAbsolutePath(), names, acePath);
    }
   
    
}
