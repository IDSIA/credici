package ch.idsia.credici.model.io.uai;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.model.io.uai.NetUAIParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;

public class CausalUAIParser extends NetUAIParser<StructuralCausalModel> {



    public CausalUAIParser(String file) throws FileNotFoundException {
        TYPE = UAITypes.CAUSAL;
        this.bufferedReader = initReader(file);
    }

    public CausalUAIParser(BufferedReader reader) {
        TYPE = UAITypes.CAUSAL;
        this.bufferedReader = reader;
    }


    // TODO: to fill these methods, check V-CREDAL parser in crema
    @Override
    protected void processFile() {

    }

    @Override
    protected StructuralCausalModel build() {
        return null;
    }
}
