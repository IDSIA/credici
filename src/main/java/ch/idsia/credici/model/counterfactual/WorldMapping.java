package ch.idsia.credici.model.counterfactual;

import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WorldMapping {

    public static final int None = -1;
    public static final int ALL = -2;
    private List<int[]> worldGroup;

    private GenericSparseModel model;

    public static List<WorldMapping> registry = new ArrayList<WorldMapping>();

    /**
     * Constructor from a vector of variables
     * @param vars
     */
    public WorldMapping(int[] vars){
        int[][] arr = new int[Collections.max(Ints.asList(vars)) +1][2];
        for(int i=0; i<arr.length; i++){
            arr[i][0] = None;
            arr[i][1] = None;
        }
        this.setWorldGroup(arr);
        registry.add(this);
    }


    public int[][] getWorldGroup() {
        return worldGroup.toArray(int[][]::new);
    }


    public void setWorldGroup(int[][] worldGroup) {
        this.worldGroup = Arrays.asList(worldGroup);
        //List<int[]> worldGroup2 = Arrays.asList(worldGroup);

    }


    /**
     * Returns the world to which a variable belongs
     * @param var
     * @return
     */
    public int getWorld(int var){
        return this.getWorldGroup()[var][0];
    }

    /**
     * Returns the group to which a variable belongs
     * @param var
     * @return
     */
    private int getGroup(int var){
        return this.getWorldGroup()[var][1];
    }

    public int[] getVariables(){
        return IntStream.range(0, this.getWorldGroup().length)
                .mapToObj(i-> new int[]{getWorld(i), getGroup(i), i})
                .filter(p-> p[0]!=None && p[1]!=None)
                .mapToInt(p-> p[2]).toArray();

    }

    /**
     * Obtains the associated variable in any world.
     * @param world int
     * @param var int - index in the global list of variables
     * @return
     */
    public int getEquivalentVars(int world, int var){

        if(getWorld(var)==ALL)
            return var;

        int group = getGroup(var);
        for(int v: getVariables())
            if(getWorld(v) == world && getGroup(v)==group)
                return v;
        return None;
    }

    /**
     * Obtains the associated variables in any world.
     * @param world int
     * @param variables int[] - indexes in the global list of variables
     * @return
     */
    public int[] getEquivalentVars(int world, int... variables) {
        return IntStream.of(variables)
                .map(v -> getEquivalentVars(world, v))
                .toArray();
    }

        /**
         * Array with the variables in a given world
         * @param world int
         * @return
         */
    public int[] getVariablesIn(int world){
        return IntStream.of(getVariables())
                .filter( v -> {int w = getWorld(v); return  w==world || w==ALL;})
                .toArray();
    }

    /**
     * Checks if a variable belongs to a given world
     * @param var int - variable to be checked
     * @param world int - id of the world
     * @return
     */
    public boolean varInWorld(int var, int world){
        return getWorld(var)==world;
    }


    /**
     * Removes the information about a variable
     * @param var
     */
    public void remove(int var){
        worldGroup.set(var, new int[]{None,None});
    }

    /***
     * Sets the world and group for an empty variable.
     * @param var
     * @param world
     * @param group
     */
    public void set(int var, int world, int group){
        if(getWorld(var)!=None || getGroup(var) != None)
            throw new IllegalArgumentException("A non-empty variable cannot be modified");
        worldGroup.set(var, new int[]{world,group});
    }

    /**
     * Returns an array with all the worlds
     * @return
     */
    public int[] getWorlds(){
        return ArraysUtil.sort(Stream.of(getWorldGroup()).mapToInt(t -> t[0]).distinct().toArray());
    }

    public void setModel(GenericSparseModel model) {
        this.model = model;
    }

    public GenericSparseModel getModel() {
        return model;
    }

    public static WorldMapping getMap(GenericSparseModel model){

        WorldMapping[] maps = registry.stream()
                .filter(w ->  w.getModel() == model)
                .toArray(WorldMapping[]::new);

        if(maps.length > 1)
            throw new IllegalArgumentException("Wrong number of maps: "+maps.length);
        else if(maps.length== 0)
            return null;

        return maps[0];

    }


    public TIntIntMap mapObservation(int world, TIntIntMap obs){
        TIntIntMap newObs = new TIntIntHashMap();
        for(int v : obs.keys()){
            newObs.put(this.getEquivalentVars(v)[0], obs.get(v));
        }
        return newObs;
    }

}


