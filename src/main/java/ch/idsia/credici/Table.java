package ch.idsia.credici;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.impl.hash.TIntIntHash;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;


/**
 * A data table counting duplicates.
 */
public class Table implements Iterable<Map.Entry<int[], Integer>> {
    Double x;
    private int[] columns;
    private TreeMap<int[], Integer> dataTable;

    public Table(TIntIntMap[] data) {
        Number x; 
    
        if (data.length == 0)
            throw new IllegalArgumentException("No data provided");

        dataTable = new TreeMap<>(Arrays::compare);

        this.columns = data[0].keys().clone();
        for (TIntIntMap inst : data) {
            add(inst);
        }
    }

    public Table(int[] columns) {
        this.columns = columns;
        dataTable = new TreeMap<>(Arrays::compare);
    }


    public TIntIntMap[] convert() {
        ArrayList<TIntIntMap> res = new ArrayList<>();

        for (Map.Entry<int[], Integer> entry : dataTable.entrySet()) {
            FIntIntHashMap map = new FIntIntHashMap(columns, entry.getKey());
            for (int i = 0; i < entry.getValue(); i++) {
                res.add(map);
            }
        }
        return res.toArray(new TIntIntMap[res.size()]);
    }

    /**
     * Add to dataTable assuming correctly ordered row items
     * @param row the item to be added
     * @param count the number of rows to be added
     */
    protected void add(int[] row, int count) {
        dataTable.compute(row, (k,v)-> (v == null) ? count : v + count);
    }   

    /**
     * Add a new row using a different column order. 
     * 
     * @param cols int[] the new columns order
     * @param inst int[] the row to be added in cols order
     * @param count the number of rows being added.
     */
    public void add(int[] cols, int[] inst, int count) {

        int[] row = Arrays.stream(columns)
                          .map(col -> ArrayUtils.indexOf(cols, col))
                          .map(i->inst[i]).toArray();

        dataTable.compute(row, (k,v)-> (v == null) ? count : v + count);
    } 
    /**
     * Add a data row to the table using a hashmap
     * @param inst
     */
    public void add(TIntIntMap inst) {
        add(inst, 1);
    }

    /** 
     * Add a TIntIntMap with the specified count. The map must contain all the keys specified in the columns
     * @param inst {@link TIntIntMap} - the row to be added
     * @param count int the number of rows being added.
     */
    public void add(TIntIntMap inst, int count) {
        int[] row = Arrays.stream(columns).map(inst::get).toArray();
        dataTable.compute(row, (k,v)-> (v == null) ? count : v + count);
    } 

    /**
     * Create a subtable for the specified columns. Columns not present in this table will be ignore and 
     * not be part of the resulting table.
     * 
     * @param cols the subset of columns
     * @return a new Table
     */
    public Table subtable(int[] cols) {
    
        int[] idx = Arrays.stream(cols).map(col -> ArrayUtils.indexOf(columns, col)).filter(a -> a >= 0).toArray();
        int[] matching = IntStream.of(idx).map(id->columns[id]).toArray();

        Table res = new Table(matching);
        for (Map.Entry<int[], Integer> entry : dataTable.entrySet()){
            int[] values = entry.getKey();
            int count = entry.getValue();

            int[] newkey = Arrays.stream(idx).map(i->values[i]).toArray();
            res.add(newkey, count);
        }
        return res;
    }
    


    /**
     * Read a table from a whitespace separated file. The whitespace can be any regex \s character.
     * 
     * @param filename
     * @return
     * @throws IOException
     */
    public static Table readTable(String filename) throws IOException {
        return readTable(filename, "\\s");
    }

    /**
     * Read table from sep separated lists of values.
     * Lists are separated by newlines and first row is the header. 
     * Header row must be integers.
     *  
     * @param filename
     * @param sep
     * @return 
     * @throws IOException
     */
    public static Table readTable(String filename, String sep) throws IOException {
        try(BufferedReader input = new BufferedReader(new FileReader(filename))){
            String[] cols = input.readLine().split(sep);
            int[] columns = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
            Table ret = new Table(columns);

            String line;
            while ((line = input.readLine()) != null) {
                cols = line.split(sep);
                
                int[] row = Arrays.stream(cols).mapToInt(Integer::parseInt).toArray();
                ret.add(row, 1);
            }
            return ret;
        } 
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String out = String.join("\t", Arrays.stream(columns).<String>mapToObj(Integer::toString).toArray(a->new String[a]));
        sb.append("count\t").append(out).append('\n');
        
        for (Map.Entry<int[], Integer> entry : dataTable.entrySet()){
            out = String.join("\t", Arrays.stream(entry.getKey()).<String>mapToObj(Integer::toString).toArray(a->new String[a]));
            sb.append(entry.getValue()).append('\t').append(out).append('\n');
        }
        return sb.toString();
    }

    @Override
    public Iterator<Map.Entry<int[], Integer>> iterator() {
        return dataTable.entrySet().iterator();
    }


    public Iterable<Pair<TIntIntMap, Integer>> mapIterable() {
        return new Iterable<Pair<TIntIntMap,Integer>>() {
            
            @Override
            public Iterator<Pair<TIntIntMap, Integer>> iterator() {
                    
                var iter = dataTable.entrySet().iterator();
                return new Iterator<Pair<TIntIntMap,Integer>>() {

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Pair<TIntIntMap, Integer> next() {
                        var nextVal = iter.next();
                        TIntIntMap ret = new FIntIntHashMap(columns, nextVal.getKey());
                        return Pair.of(ret, nextVal.getValue());
                    }
                };
            }
        };
    }
}
