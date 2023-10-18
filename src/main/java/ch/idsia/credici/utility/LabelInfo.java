package ch.idsia.credici.utility;

import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class LabelInfo {
    HashMap<Integer,String> varnames = null;
    HashMap<Integer,String[]> domainnames = null;

    public HashMap<Integer, String> getVarNames() {
        return varnames;
    }

    public HashMap<Integer, String[]> getDomainNames() {
        return domainnames;
    }

    public LabelInfo(String filename) throws IOException, CsvException {
        List info = DataUtil.fromCSVtoStrMap(filename);
        varnames = new HashMap();
        domainnames = new HashMap();

        for(int i=0; i<info.size(); i++) {
            varnames.put(i, (String) ((HashMap) info.get(i)).get("var"));
            domainnames.put(i,     ((String)((HashMap) info.get(i)).get("states")).split("[|]"));
        }
    }

    public static LabelInfo from(String filename) throws IOException, CsvException {
        return new LabelInfo(filename);
    }

    public static LabelInfo from(Path path) throws IOException, CsvException {
        return new LabelInfo(path.toAbsolutePath().toString());
    }

    public static HashMap<Integer,String> readVarNames(String filename) throws IOException, CsvException {
        HashMap<String,String> data = DataUtil.fromCSVtoStrMap(filename).get(0);
        HashMap<Integer, String> out = new HashMap<>();
        for(String v : data.keySet())
            out.put(Integer.parseInt(v), data.get(v));

        return out;
    }

    public static HashMap<Integer,String> readVarNames(Path path) throws IOException, CsvException {
        return readVarNames(path.toAbsolutePath().toString());
    }



}
