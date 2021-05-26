package neurnips20.experiments;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToInterval;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.learning.FrequentistEM;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.user.credal.Vertex;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.Collections.*;

public class RunExperiments {


    static String modelName = "./papers/neurnips20/experiments/models/scm2.uai";
    static StructuralCausalModel causalModel;
    static int numberPoints = 5;
    static int numberEMiter = 10 ;
    static int samples = 2000;
    static  TIntIntMap[] data;

    static int target;
    static TIntIntMap intervention;
    static TIntIntMap obs;

    static IntervalFactor[] ifactors;
    static BayesianNetwork[] bnets;
    static VertexFactor exactRes;

    static Map<Integer, Map<String, Double>> results;




    public static void main(String[] args) throws IOException, InterruptedException {
        parseArgs(args);
        init();
        run();
        buildResults();
        printResults();



    }

    public static void init() throws IOException {
        causalModel = (StructuralCausalModel) IO.read(modelName);

        target = 3;
        intervention = ObservationBuilder.observe(0, 1);
        obs = ObservationBuilder.observe(causalModel.getEndogenousVars().length-1, 1);

        data =  IntStream.range(0,samples).mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars())).toArray(TIntIntMap[]::new);

        results = new HashMap<Integer, Map<String, Double>>();

        for(int i=1; i<=numberPoints; i++){
            results.put(i, new HashMap<String, Double>());
        }

        ifactors = new IntervalFactor[numberPoints];
        bnets = new BayesianNetwork[numberPoints];

    }

    public static void run() throws InterruptedException {


        // Compute the exact results
        CredalCausalVE inf = new CredalCausalVE(causalModel);
        exactRes = (VertexFactor) inf.causalQuery().setTarget(target).setIntervention(intervention).run();

        // Approximate EM-based methods
        for(int i=0; i<numberPoints; i++) {

            // randomize P(U)
            StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                    5, false
                    ,causalModel.getExogenousVars()
            );

            // Run EM in the causal model
            ExpectationMaximization em =
                    new FrequentistEM(rmodel)
                            .setVerbose(false)
                            //.setRegularization(0.000000001)
                            .setVerbose(true)
                            .setTrainableVars(causalModel.getExogenousVars());


            // run the method
            em.run(Arrays.asList(data), numberEMiter);

            // Extract the learnt model
            StructuralCausalModel postModel = (StructuralCausalModel) em.getPosterior();
            System.out.println(postModel);

            bnets[i] = postModel.toBnet();

            // Run the  query
            CausalVE ve = new CausalVE(postModel);
            ifactors[i] = new BayesianToInterval().apply(ve.doQuery(target, intervention), target);
            System.out.println(ifactors[i]);
        }

    }




    public static void parseArgs(String[] args){

        if (args.length > 0) {

            Options options = getArgOptions();
            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();

            CommandLine cmd = null;

            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                System.out.println(e.getMessage());
                formatter.printHelp("utility-name", options);

                System.exit(1);
            }

            modelName = cmd.getOptionValue("model");
            numberPoints = Integer.parseInt(cmd.getOptionValue("endovarsize"));
            if(cmd.hasOption("numberEMiter")) numberEMiter = Integer.parseInt(cmd.getOptionValue("numberEMiter"));
            if(cmd.hasOption("samples")) numberEMiter = Integer.parseInt(cmd.getOptionValue("samples"));


        }

    }


    public static Options getArgOptions(){

                        /*
                    --numberPoints 20
                    --numberEMiter 10
                    --samples 2000
                    --model ./papers/neurnips20/experiments/models/scm2.uai
                */
        Options options = new Options();

        options.addOption(Option.builder("N").longOpt("numberPoints").hasArg(true).required(true).build());
        options.addOption(Option.builder("n").longOpt("numberEMiter").hasArg(true).required(false).build());
        options.addOption(Option.builder("s").longOpt("samples").hasArg(true).required(false).build());
        options.addOption(Option.builder("m").longOpt("model").hasArg(true).required().build());

        return options;

    }


    public static void buildResults() throws InterruptedException {

        // exact result
        Map exactRes_ = intervalToDict("Pexact", new VertexToInterval().apply(exactRes, target));

        //EM-based results
        for(int i=1; i<=numberPoints; i++){

            //Store the numberOfPoints
            results.get(i).put("num_points", (double)i);

            // Store the exact result
            results.get(i).putAll(exactRes_);

            // Queries union
            IntervalFactor queriesUnion =
                IntervalFactor.mergeBounds(
                        IntStream.range(0,i).mapToObj(k -> ifactors[k]).toArray(IntervalFactor[]::new)
                );

            results.get(i).putAll(intervalToDict("Puq", queriesUnion));

            // Credal network
            SparseModel composed = VertexFactor.buildModel(true,
                    IntStream.range(0,i).mapToObj(k -> bnets[k]).toArray(BayesianNetwork[]::new)
            );

            CredalCausalVE credalVE = new CredalCausalVE(composed);
            VertexFactor res = (VertexFactor) credalVE.causalQuery().setIntervention(intervention).setTarget(target).run();

            results.get(i).putAll(intervalToDict("Pcn", new VertexToInterval().apply(res, target)));
        }
    }


    public static Map<String, Double> intervalToDict(String label, IntervalFactor f){
        Map<String, Double> out = new HashMap<String, Double>();
        double[] lbounds = Doubles.concat(f.getDataLower());
        double[] ubounds = Doubles.concat(f.getDataUpper());
        for(int i=0; i<lbounds.length; i++) {
            out.put(label + i + "_lbound", lbounds[i]);
        }
        for(int i=0; i<ubounds.length; i++){
            out.put(label+i+"_ubound", ubounds[i]);
        }
        return out;
    }


    public static void printResults(){
        System.out.println("<output>\n[");
        for(int k: results.keySet()){
            System.out.println(dictToString(results.get(k))+",");
        }
        System.out.println("]\n<\\output>");
    }

    public static String dictToString(Map<String, Double> d){
        String out = "{";
        List<String> keys = new ArrayList<String>(d.keySet());
        sort(keys);
        for(String k: keys){
            out+="'"+k+"': "+d.get(k)+",";
        }
        out = out.substring(0,out.length());
        out +="}";
        return out;

    }



}
