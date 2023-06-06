package ch.idsia.credici.utility.experiments;

import ch.idsia.credici.utility.DataUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Results {

    Logger logger = null;

    HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();


    private HashMap<String, String> addExperiment(String key){
        return results.put(key, new HashMap<String, String>());
    }

    private void addResults(String name, double value, String key) { results.get(key).put(name, String.valueOf(value));}
    private void addResults(String name, int value, String key) { results.get(key).put(name, String.valueOf(value));}
    private void addResults(String name, long value, String key) { results.get(key).put(name, String.valueOf(value));};
    private void addResults(String name, boolean value, String key) { results.get(key).put(name, String.valueOf(value));};
    private void addResults(String name, String value, String key) { results.get(key).put(name, value);};


    private void addToAllResults(String name, double value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    }
    private void addToAllResults(String name, int value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    }
    private void addToAllResults(String name, long value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    };
    private void addToAllResults(String name, boolean value) {
        for(String key : results.keySet())
            results.get(key).put(name, String.valueOf(value));
    };

    private void addToAllResults(String name, String value) {
        for(String key : results.keySet())
            results.get(key).put(name, value);
    };

    private void save(String fullpath) throws IOException {
        if(logger != null)
            logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, List.of(results));

    }
}
