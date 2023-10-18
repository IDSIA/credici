package ch.idsia.credici.utility;

import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;

import br.usp.poli.generator.BNGenerator;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.jgrapht.Graph;
import org.jgrapht.alg.clique.ChordalGraphMaxCliqueFinder;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;


public class DAGUtil {

    private static int[] nodesOperation(BinaryOperator<int[]> func, SparseDirectedAcyclicGraph... DAGs) {
        int[] out = DAGs[0].getVariables();
        for (int i = 1; i < DAGs.length; i++)
            out = func.apply(out, DAGs[i].getVariables());
        return out;

    }

    public static int[] nodesIntersection(SparseDirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::intersection, DAGs);
    }

    public static int[] nodesDifference(SparseDirectedAcyclicGraph... DAGs) {
        return nodesOperation(ArraysUtil::difference, DAGs);
    }

    public static boolean isContained(SparseDirectedAcyclicGraph subDAG, SparseDirectedAcyclicGraph DAG) {
        for (int x : subDAG.getVariables()) {
            for (int y : subDAG.getChildren(x)) { // x -> y
                if (!DAG.containsEdge(x, y))
                    return false;
            }
        }
        return true;
    }


    public static SparseDirectedAcyclicGraph getSubDAG(SparseDirectedAcyclicGraph dag, int... nodes) {
        int[] toremove = ArraysUtil.difference(dag.getVariables(), nodes);
        SparseDirectedAcyclicGraph out = dag.copy();
        for (int v : toremove) {
            out.removeVariable(v);
        }
        return out;
    }
    public static SparseDirectedAcyclicGraph ancestralDAG(SparseDirectedAcyclicGraph dag, int... nodes) {
        int[] relevant = Ints.concat(ancestors(dag, nodes),nodes);
        relevant = IntStream.of(relevant).distinct().toArray();
        return getSubDAG(dag, relevant);
    }

    public static void main2(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3, 0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x, y), new double[]{0.6, 0.5, 0.5, 0.5}));


        //int u1=2, u2=3;

        //EquationBuilder.fromVector(Strides.as(y,2))

        StructuralCausalModel model = CausalBuilder.of(bnet).setFillRandomExogenousFactors(3).build();

        CausalBuilder b = CausalBuilder.of(bnet).setFillRandomExogenousFactors(3);
        b.build();

        String str = Arrays.toString(
                nodesDifference(b.getCausalDAG(), b.getEmpiricalDAG())
        );

        System.out.println(str);

    }

    public static int[] getTopologicalOrder(DirectedAcyclicGraph dag, int... nodes) {

        if (nodes.length == 0)
            nodes = Arrays.asList(dag.vertexSet().toArray())
                    .stream().mapToInt(x -> ((Integer) x).intValue())
                    .toArray();

        int[] finalNodes = nodes;
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new TopologicalOrderIterator<Integer, DefaultEdge>(dag), Spliterator.ORDERED
                ),
                false)
                .filter(x -> ArraysUtil.contains(x, finalNodes))
                .mapToInt(x -> ((Integer) x).intValue()).toArray();
    }


    public static SparseDirectedAcyclicGraph randomDag(int numNodes) {
        Random r = RandomUtil.getRandom();

        if (numNodes < 2)
            throw new IllegalArgumentException("The minumum number of nodes is 2.");

        SparseDirectedAcyclicGraph dag = new SparseDirectedAcyclicGraph();
        for (int i = 0; i < numNodes; i++) {
            dag.addVariable(i);
        }

        // number of sets in the top. order
        int numSets = r.nextInt(numNodes - 1) + 2;

        // gather all the nodes such as no empty sets

        List nodes =
                IntStream.range(0, numNodes)                      // <-- creates a stream of ints
                        .boxed()                                // <-- converts them to Integers
                        .collect(Collectors.toList());          // <-- collects the values to a list

        Collections.shuffle(nodes);

        int sets[][] = new int[numSets][];

        for (int s = 0; s < numSets; s++) {
            int setSize = 0;
            if (s == numSets - 1)
                setSize = nodes.size();
            else
                setSize = r.nextInt(nodes.size() - (numSets - s - 1)) + 1;
            sets[s] = new int[setSize];
            for (int i = 0; i < setSize; i++) {
                sets[s][i] = (int) nodes.remove(0);
            }
        }


        // set minimal outgoing arcs
        for (int s = 0; s < numSets - 1; s++) {
            for (int i : sets[s]) {
                // random node from the next set
                int j = sets[s + 1][r.nextInt(sets[s + 1].length)];
                dag.addLink(i, j);
            }
        }


        // set minimal ingoing arcs
        for(int s=1; s<numSets; s++) {
            for(int i : sets[s]){
                if(dag.getParents(i).length==0) {
                    // random node from the previous set
                    int j = sets[s-1][r.nextInt(sets[s-1].length)];
                    dag.addLink(j,i);
                }
            }
        }

        return dag;
    }


    public static Graph moral(SparseDirectedAcyclicGraph dag){

        Graph moral = new DefaultUndirectedGraph(DefaultEdge.class);
        for(int x : dag.getVariables())
            moral.addVertex(x);

        // Delete incoming arcs to observations
        for(int x: dag.getVariables()){
            for(int y: dag.getParents(x)){
                moral.addEdge(y,x);
                for(int y2: dag.getParents(x)){
                    if(y!=y2 && !moral.containsEdge(y,y2))
                        moral.addEdge(y, y2);
                }
            }
        }
        return moral;
    }

    public static  List<int[]> connectComponents(Graph g){
        ConnectivityInspector connectInspect = new ConnectivityInspector(g);
        List connectedComponentList = connectInspect.connectedSets();
        return (List<int[]>) connectedComponentList.stream().map(s -> Ints.toArray((Set)s)).collect(Collectors.toList());
    }

    public static  boolean isConnected(Graph g){
        return new ConnectivityInspector(g).isConnected();
    }


    public static boolean dseparated(SparseDirectedAcyclicGraph dag, int a, int b, int... obs){

        if(a==b || dag.containsEdge(a,b) || dag.containsEdge(b,a))
            return false;


        int[] relevant = {a,b};
        relevant = Ints.concat(relevant, obs);

        Graph moral = DAGUtil.moral(DAGUtil.ancestralDAG(dag, relevant));
        for(int v : obs) {
            if(v!=a && v!=b)
                moral.removeVertex(v);
        }

        // check if a and b are not graphically connected in the moral graph
        return !new ConnectivityInspector(moral).pathExists(a,b);
    }


    public static int nodesDistance(SparseDirectedAcyclicGraph dag, int x, int y){
        if(x==y) return 0;
        return DijkstraShortestPath.findPathBetween(getUndirected(dag), x,y).getLength();
    }

    public static int[][] distanceMatrix(SparseDirectedAcyclicGraph graph){

        int nNodes = graph.getVariables().length;
        int[][] distanceMatrix = new int[nNodes][nNodes];
        for(int x : graph.getVariables()){
            for(int y:graph.getVariables()){
                if(x<y){
                    int d = DAGUtil.nodesDistance(graph, x,y);
                    distanceMatrix[x][y] = d;
                    distanceMatrix[y][x] = d;
                }
            }
        }
        return distanceMatrix;
    }

    public static DefaultUndirectedGraph getUndirected(SparseDirectedAcyclicGraph dag) {
        DefaultUndirectedGraph g = new DefaultUndirectedGraph(DefaultEdge.class);
        for(int v : dag.vertexSet())
            g.addVertex(v);

        for(int v : dag.vertexSet())
            for(int p: dag.getParents(v))
                g.addEdge(p, v);

        return g;
    }


    public static int[][] matrixDistances(SparseDirectedAcyclicGraph dag){
        int N = dag.vertexSet().size();

        int[][] dist = new int[N][N];
        int[] X = dag.getVariables();

        for(int i=0; i<N-1; i++){
            for(int j=i+1; j<N; j++){
                int d = DAGUtil.nodesDistance(dag, X[i], X[j]);
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }

        return dist;
    }

    public static int[] getRootNodes(SparseDirectedAcyclicGraph dag){
        return Arrays.stream(dag.getVariables()).filter(v -> dag.getParents(v).length==0).toArray();
    }

    public static int[] getNonRootNodes(SparseDirectedAcyclicGraph dag){
        return Arrays.stream(dag.getVariables()).filter(v -> dag.getParents(v).length!=0).toArray();
    }


    public int getTreewidth(DirectedAcyclicGraph dag){
        Graph moral = DAGUtil.moral((SparseDirectedAcyclicGraph) dag);
        return new ChordalGraphMaxCliqueFinder<>(moral).getClique().size() - 1;

    }

    public static SparseDirectedAcyclicGraph build(String arcs) {
        SparseDirectedAcyclicGraph dag = new SparseDirectedAcyclicGraph();


        String regex = "([0-9]+),([0-9]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(arcs);

        ArrayList nodes = new ArrayList();
        ArrayList origin = new ArrayList();
        ArrayList dest = new ArrayList();


        while (matcher.find()) {
            String str = matcher.group(0);
            int arc[] = Arrays.stream(str.split(",")).mapToInt(s -> Integer.parseInt(s)).toArray();
            origin.add(arc[0]);
            dest.add(arc[1]);
            if(!nodes.contains(arc[0])) nodes.add(arc[0]);
            if(!nodes.contains(arc[1])) nodes.add(arc[1]);
        }

        Collections.sort(nodes);

        for(int n: CollectionTools.toIntArray(nodes))
            dag.addVariable(n);

        for(int i=0; i<origin.size(); i++) {
            int x = ((Integer) origin.get(i)).intValue();
            int y = ((Integer) dest.get(i)).intValue();
            if(x!=y)
                dag.addLink(x, y);
        }
        return dag;
    }






    /**
     * Generate network using BNGenerator
     * 
     * @param numberNodes number of nodes in the network (must be at least 4)
     * @param maxInducedWidth set to -1 for no bounds
     * @return
     */
    public static SparseDirectedAcyclicGraph randomFromBNGenerator(int numberNodes, int maxInducedWidth) {

        // Silence stdout in this function
        PrintStream out = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        int nGraphs=1;
        String structure="multi"; // default structure
        String format="xml"; // default format
        String baseFileName = "Graph"; // default file name
        Integer line = null; // Default value should also be provided
        int maxValues=2; //  Default is binary nodes.
        boolean fixed_nVal = false;
        int nIterations=0;
    
        int numberMaxDegree=3;
        int numberMaxInDegree=3;
        int numberMaxOutDegree=3;

        boolean maxDegreeWasSet = false;
        boolean maxInDegreeWasSet = false;
        boolean maxOutDegreeWasSet = false;

        boolean maxArcsWasSet = false;
        int numberMaxArcs=6;


        int nPoints=3; // default number of points used to generate credal sets
		BNGenerator bn;
        

		float lowerP=0;
        float upperP=1;

  
              
        format = "dag";
        
        
        // set default values, if it was not set
        if (!maxDegreeWasSet)       numberMaxDegree = numberNodes-1;
        if (!maxInDegreeWasSet)     numberMaxInDegree=numberMaxDegree;
        if (!maxOutDegreeWasSet)    numberMaxOutDegree=numberMaxDegree;
        if (!maxArcsWasSet)         numberMaxArcs = numberNodes*numberMaxDegree/2;	// Set a default maximum number of arcs (all combinations)
        
        // verify some incoherent data
        if (maxArcsWasSet)  {
            int auxNumber=numberNodes*numberMaxDegree/2;
            if (numberMaxArcs > auxNumber)  numberMaxArcs = auxNumber;
            if  (numberMaxArcs < (numberNodes-1) ) numberMaxArcs=numberNodes;
        }

        if (maxInDegreeWasSet || maxOutDegreeWasSet)  {
            if (structure.compareTo("multi")==0) {
                if (numberMaxOutDegree==1)  numberMaxOutDegree=2;
                if (numberMaxInDegree==1)   numberMaxInDegree=2;
            }
        }
        if (maxInDegreeWasSet && maxOutDegreeWasSet)  {
            if ((numberMaxInDegree+numberMaxOutDegree)==2) numberMaxOutDegree=2;
        }

        // Set global variables
        bn = new BNGenerator(numberNodes,numberMaxDegree);
        bn.setnNodes(numberNodes);  // set nNodes
        bn.setMaxDegree(numberMaxDegree);  // set maxDegree
        bn.setMaxInDegree(numberMaxInDegree);  // set maximum number of incoming arcs
        bn.setMaxOutDegree(numberMaxOutDegree);  // set maximum number of outgoing arcs
        bn.setMaxArcs(numberMaxArcs);  // set maxArcs(a global variable)
        bn.setDataFactory(embayes.data.impl.DataBasicFactory.getInstance());
        bn.setInferFactory(embayes.infer.impl.InferBasicFactory.getInstance(bn.getDataFactory()));
        bn.setFixed_nValue(fixed_nVal);
		bn.setnPointProb(nPoints);
		bn.setLowerP(lowerP);
		bn.setUpperP(upperP);

        // Determining the number of iterations for
        // the chain to converge is a difficult task.
        // This value follows the DagAlea (see Melancon;Bousque,2000) suggestion,
        // and we verified that this number is satisfatory:
        nIterations = 6*bn.getnNodes()*bn.getnNodes();
        
        bn.inicializeGraph(); // Inicialize a simple ordered tree as a BN structure
        
        //// Generating process ////
        try {
            bn.generate(structure, nGraphs,nIterations,maxValues,format,baseFileName,maxInducedWidth);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Reset the stdout
        System.setOut(out);

        String dag = bn.toDag();
        return DAGUtil.build(dag);
    }




    public static SparseDirectedAcyclicGraph random(int numNodes, int lambda, int maxIndegree) {



        PoissonDistribution poiss = new PoissonDistribution(lambda);
        poiss.reseedRandomGenerator(RandomUtil.getRandom().nextInt());
        SparseDirectedAcyclicGraph graph = new SparseDirectedAcyclicGraph();

        for(int i=0; i<numNodes; i++) graph.addVariable(i);

        for(int i=1; i<numNodes; i++){
            int p = poiss.sample();
            p = Math.min(p, Math.min(i, maxIndegree));
            int[] Pa = CollectionTools.choice(p, IntStream.range(0,i).toArray());
            for(int j: Pa) {
                graph.addLink(j, i);
                //System.out.println(j+","+i);
            }
        }


        while(!DAGUtil.isConnected(graph)){
            int[] candidate = graph.vertexSet().stream().mapToInt(x->x).filter(x -> x != 0 && graph.getParents(x).length<maxIndegree).toArray();
            int i = CollectionTools.choice(1, candidate)[0];
            int j = CollectionTools.choice(1, IntStream.range(0,i).toArray())[0];
            graph.addLink(j, i);
            //System.out.println(j+","+i);
        }



        return graph;

    }

    public static double avgIndegree(SparseDirectedAcyclicGraph graph){
        return graph.vertexSet().stream().mapToDouble(x -> graph.getParents(x).length).average().getAsDouble();
    }


    public static int[] ancestors(SparseDirectedAcyclicGraph dag, int... nodes) {
        return IntStream.of(nodes).flatMap(x-> Arrays.stream(DAGUtil.ancestors(dag, x))).distinct().toArray();
    }

    public static int[] ancestors(SparseDirectedAcyclicGraph dag, int x){

        Set ancestors = new HashSet<>();
        Set checked = new HashSet<>();
        Stack toCheck = new Stack();
        toCheck.add(x);

        do {
            int v = (int) toCheck.pop();
            int[] paV = dag.getParents(v);
            for (int p : paV) {
                ancestors.add(p);
                if (!checked.contains(p)) toCheck.add(p);
            }
            checked.add(v);

        }while(!toCheck.isEmpty());

        return CollectionTools.toIntArray((List<Integer>) ancestors.stream().collect(Collectors.toList()));

    }

    public static String getLabelledEdges(SparseDirectedAcyclicGraph dag, HashMap<Integer,String> varnames){
        List<String> edges = new ArrayList();
        for(int x : dag.getVariables()){
            for(int y : dag.getParents(x)){
                edges.add("("+varnames.get(y)+","+varnames.get(x)+")");
            }
        }

        return String.join(", ",edges);
    }


}
