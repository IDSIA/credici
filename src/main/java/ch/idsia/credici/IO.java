package ch.idsia.credici;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.io.uai.CausalUAIWriter;
import ch.idsia.credici.model.io.uai.UAITypes;
import ch.idsia.crema.model.io.uai.UAIParser;
import ch.idsia.crema.model.io.uai.UAIWriter;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * Access point to all the implemented parsers
 * @author Rafael Cabañas
 */
public class IO extends ch.idsia.crema.IO {

    public static final String[] UAIextensions = {".uai", ".uai.do", "uai.evid"};

    public static Object readUAI(String filename) throws IOException {
        if(UAIParser.getIOTypeStr(filename).equals(UAITypes.CAUSAL.label)) {
            return CausalUAIParser.read(filename);
        }
        return UAIParser.read(filename);
    }

    public static void writeUAI(Object target, String filename) throws IOException {
        if(target instanceof StructuralCausalModel)
            CausalUAIWriter.write(target, filename);
        else
            UAIWriter.write(target, filename);

    }

    public static Object read(String filename) throws IOException {

        if(Stream.of(UAIextensions).anyMatch(s -> filename.endsWith(s))){
            return readUAI(filename);
        }else{
            throw new IllegalArgumentException("Unknown file extension");
        }

    }

    public static void write(Object target, String filename) throws IOException {

        if(Stream.of(UAIextensions).anyMatch(s -> filename.endsWith(s))){
            writeUAI(target, filename);
        }else{
            throw new IllegalArgumentException("Unknown file extension");
        }

    }

}
