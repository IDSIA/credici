package ch.idsia.credici.utility.experiments;

import ch.idsia.credici.utility.DataUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ResultsManager {

    public boolean includeLabel = true;

    public ResultsManager(){
        results.put("", new HashMap<String, String>());
    }

    public ResultsManager setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public ResultsManager setIncludeLabel(boolean includeLabel) {
        this.includeLabel = includeLabel;
        return this;
    }

    Logger logger = null;

    HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();



    public void addExperiment(String label){
        if(label.equals(""))
            throw new IllegalArgumentException("Wrong label");
        results.put(label, new HashMap<String, String>());
        if(includeLabel) add(label,"label",label);
    }


    public void add(String label, String field, double[] value) {
        for(int i=0; i<value.length; i++)
            add(label, field+"_"+i, value[i]);
    }

    public void add(String label, String field, int[] value) {
        for(int i=0; i<value.length; i++)
            add(label, field+"_"+i, value[i]);
    }
    public void add(String label, String field, double value) { results.get(label).put(field, String.valueOf(value));}
    public void add(String label, String field, int value) { results.get(label).put(field, String.valueOf(value));}
    public void add(String label, String field, long value) { results.get(label).put(field, String.valueOf(value));};
    public void add(String label, String field, boolean value) { results.get(label).put(field, String.valueOf(value));};
    public void add(String label, String field, String value) { results.get(label).put(field, value);};


    public void add(String field, double[] value){
        add("", field, value);
    }
    public void add(String field, int[] value){
        add("", field, value);
    }
    public void add(String field, double value) { add("", field, value);}
    public void add(String field, int value) { add("", field, value);}
    public void add(String field, long value) { add("", field, value);};
    public void add(String field, boolean value) { add("", field, value);};
    public void add(String field, String value) { add("", field, value);};




    public void addToAll(String field, double value) {
        for(String label : results.keySet())
            add(label,field,value);
    }
    public void addToAll(String field, int value) {
        for(String label : results.keySet())
            add(label,field,value);
    }
    public void addToAll(String field, long value) {
        for(String label : results.keySet())
            add(label,field,value);
    };
    public void addToAll(String field, boolean value) {
        for(String label : results.keySet())
            add(label,field,value);
    };

    public void addToAll(String field, String value) {
        for(String label : results.keySet())
            add(label,field,value);
    };


    public void addToAll(String field, double[] value){
        for(String label : results.keySet())
            add(label,field,value);
    }

    public void save(String fullpath) throws IOException {
        if(logger != null)
            logger.info("Saving info at:" +fullpath);
        DataUtil.toCSV(fullpath, results.values().stream().filter(m -> m.size()>0).collect(Collectors.toList()));

    }


    public List<String> getSummary(){
        List<String> out = new ArrayList<>();
        for(String label: this.results.keySet()){
            HashMap<String,String> r = results.get(label);
            for(String field : r.keySet()){
                String str = field+" =\t"+r.get(field);
                if(!label.equals(""))
                    str = label+"."+str;
                str = "-"+str;
                out.add(str);
            }
        }
        return out;
    }

    public void logSummary(){
        if(logger!=null)
            for(String l : this.getSummary())
                logger.debug(l);
    }


    public static void main(String[] args) throws IOException {
        ResultsManager res = new ResultsManager().setIncludeLabel(false);
        //res.addExperiment("exp1");
       // res.addExperiment("exp2");

        //res.add("exp1", "time", 1332);
        //res.add("exp2", "time", 43);
        res.add("time",32);
        res.add("v",32432);
        res.addToAll("pns", new double[]{0.1, 0.2});

        res.save("./res.csv");

    }



}
