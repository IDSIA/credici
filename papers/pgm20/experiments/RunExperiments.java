package pgm20.experiments;

import ch.idsia.credici.model.predefined.RandomSquares;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalAproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.utility.InvokerWithTimeout;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.hash.TIntIntHashMap;

import org.apache.commons.cli.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.idsia.credici.model.predefined.RandomChainNonMarkovian.buildModel;


public class RunExperiments {


    static StructuralCausalModel model;

    static TIntIntHashMap evidence, intervention;
    static int target;
    static double eps;

    static int endoVarSize;
    static int exoVarSize;

    static String method;

    static int warmups = 0;
    static int repetitions = 1;

    static boolean verbose = false;

    static long TIMEOUT = 5*60;

    static int resultSize = 3;


    public static void main(String[] args) throws InterruptedException {
        try {
            ////////// Input arguments Parameters //////////

            String modelName = "ChainMarkovian";

            /** Number of endogenous variables in the chain (should be 3 or greater)*/
            int N = 4;
            /** Number of states in endogenous variables */
            endoVarSize = 3;
            /** Number of states in the exogenous variables */
            exoVarSize = 9;

            target = N/2;
            target = 1;

            int obsvar = N - 1;
            //obsvar = -1;

            int dovar = 0;

            /** Inference method: CVE, CCVE, CCALP, CCALPeps  **/
            method = "CCALPeps";
           // method = "CVE";

            eps = 0.0001;

            long seed = 1234;


            // ChainNonMarkovian 6 5 1 -1 0 CCALP 1234 0 1
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

                modelName = args[0];
                N = Integer.parseInt(args[1]);

                endoVarSize = Integer.parseInt(cmd.getOptionValue("endovarsize"));
                exoVarSize = Integer.parseInt(cmd.getOptionValue("exovarsize"));
                target = Integer.parseInt(cmd.getOptionValue("target"));
                method = cmd.getOptionValue("method");

                if(cmd.hasOption("obsvar")) obsvar = Integer.parseInt(cmd.getOptionValue("obsvar"));
                if(cmd.hasOption("dovar")) dovar = Integer.parseInt(cmd.getOptionValue("dovar"));
                if(cmd.hasOption("seed")) seed = Long.parseLong(cmd.getOptionValue("seed"));
                if(cmd.hasOption("warmups")) warmups = Integer.parseInt(cmd.getOptionValue("warmups"));
                if(cmd.hasOption("repetitions")) repetitions = Integer.parseInt(cmd.getOptionValue("repetitions"));
                if(cmd.hasOption("epsilon")) eps = Double.parseDouble(cmd.getOptionValue("epsilon"));


            }

            if(!method.equals("CCALPeps"))
                eps = 0.0;


            System.out.println("\n" + modelName + "\n   N=" + N + " endovarsize=" + endoVarSize + " exovarsize=" + exoVarSize + " target=" + target + " obsvar=" + obsvar + " dovar=" + dovar + " method=" + method + " seed=" + seed);
            System.out.println("=================================================================");
            long heapMaxSize = Runtime.getRuntime().maxMemory();
            System.out.println("max heap memory = -Xmx"+heapMaxSize/1024/1024/1024+"g -Xmx"+heapMaxSize/1024/1024+"m");

            /////////////////////////////////
            RandomUtil.getRandom().setSeed(seed);


            // Load the chain model

            if (modelName.equals("ChainMarkovian"))
                model = ch.idsia.credici.model.predefined.RandomChainMarkovian.buildModel(N, endoVarSize, exoVarSize);
            else if (modelName.equals("ChainNonMarkovian"))
                model = ch.idsia.credici.model.predefined.RandomChainNonMarkovian.buildModel(N, endoVarSize, exoVarSize);
            else if (modelName.equals("TerBinChainMarkovian"))
                model = ch.idsia.credici.model.predefined.TerBinChainMarkovian.buildModel(N);
            else if (modelName.equals("TerBinChainNonMarkovian"))
                model = ch.idsia.credici.model.predefined.TerBinChainNonMarkovian.buildModel(N);
            else if (modelName.equals("HMM-Markovian"))
                model = ch.idsia.credici.model.predefined.RandomHMM.buildModel(true, N, endoVarSize, exoVarSize);
            else if (modelName.equals("HMM-NonMarkovian"))
                model = ch.idsia.credici.model.predefined.RandomHMM.buildModel(false, N, endoVarSize, exoVarSize);
            else if (modelName.equals("RHMM-Markovian"))
                model = ch.idsia.credici.model.predefined.RandomRevHMM.buildModel(true, N, endoVarSize, exoVarSize);
            else if (modelName.equals("RHMM-NonMarkovian"))
                model = ch.idsia.credici.model.predefined.RandomRevHMM.buildModel(false, N, endoVarSize, exoVarSize);
            else if (modelName.equals("Squares-Markovian"))
                model = ch.idsia.credici.model.predefined.RandomSquares.buildModel(true, N, endoVarSize, exoVarSize);
            else if (modelName.equals("Squares-NonMarkovian"))
                model = RandomSquares.buildModel(false, N, endoVarSize, exoVarSize);
            else
                throw new IllegalArgumentException("Non valid model name");

            resultSize = model.getDomain(target).getCombinations();

            int[] X = model.getEndogenousVars();

            evidence = new TIntIntHashMap();
            if (obsvar >= 0) evidence.put(obsvar, 0);

            intervention = new TIntIntHashMap();
            if (dovar >= 0) intervention.put(dovar, 0);

            System.out.println("Running experiments...");

            double res[] = run();
            for(int i=0; i<res.length; i++){
                if(i!=res.length-1)
                    System.out.print(res[i]+",");
                else
                    System.out.println(res[i]);
            }

        }catch (TimeoutException e){
            System.out.println(e);
            String[] nanStrings = IntStream.range(0, resultSize*2).mapToObj(i -> "nan").toArray(String[]::new);
            System.out.println("inf,inf,"+String.join(",",nanStrings));
        }catch (Exception e){
            System.out.println(e);
            //e.printStackTrace();
            String[] nanStrings = IntStream.range(0, resultSize*2).mapToObj(i -> "nan").toArray(String[]::new);
            System.out.println("nan,nan,"+String.join(",",nanStrings));
        }catch (Error e){
            System.out.println(e);
            //e.printStackTrace();
            String[] nanStrings = IntStream.range(0, resultSize*2).mapToObj(i -> "nan").toArray(String[]::new);
            System.out.println("nan,nan,"+String.join(",",nanStrings));

        }


    }

    public static Options getArgOptions(){

                        /*
                ChainNonMarkovian 6
                    --endovarsize 5
                    --exovarsize 9
                    --target 1
                    --obsvar -1
                    --dovar 0
                    --method  CCALP
                    --seed 1234
                    --warmups 0
                    --repetitions 1
                */


        Options options = new Options();

        options.addOption(Option.builder("v").longOpt("endovarsize").hasArg(true).required().build());
        options.addOption(Option.builder("V").longOpt("exovarsize").hasArg(true).required().build());
        options.addOption(Option.builder("t").longOpt("target").hasArg(true).required().build());
        options.addOption(Option.builder("o").longOpt("obsvar").hasArg(true).required(false).build());
        options.addOption(Option.builder("d").longOpt("dovar").hasArg(true).required(false).build());
        options.addOption(Option.builder("m").longOpt("method").hasArg(true).required().build());
        options.addOption(Option.builder("s").longOpt("seed").hasArg(true).required(false).build());
        options.addOption(Option.builder("w").longOpt("warmups").hasArg(true).required(false).build());
        options.addOption(Option.builder("r").longOpt("repetitions").hasArg(true).required(false).build());
        options.addOption(Option.builder("e").longOpt("epsilon").hasArg(true).required(false).build());


        return options;

    }



    static double[] experiment() throws InterruptedException {
        Instant start = Instant.now();
        Instant queryStart = null;


        double intervalSize = 0.0;
        double[] lowerBound = new double[resultSize];
        double[] upperBound = new double[resultSize];

        if(method.equals("CVE")) {
            CausalInference inf1 = new CausalVE(model);
            queryStart = Instant.now();
            BayesianFactor result1 = (BayesianFactor) inf1.query(target, evidence, intervention);
            if(verbose) System.out.println(result1);
            lowerBound = result1.getData();
            upperBound = lowerBound;



        }else if(method.equals("CCVE")) {
            CausalInference inf2 = new CredalCausalVE(model);
            queryStart = Instant.now();
            VertexFactor result2 = (VertexFactor) inf2.query(target, evidence, intervention);
            if (verbose) System.out.println(result2);

            for(int i=0; i<resultSize; i++) {
                lowerBound[i] =Stream.of(result2.filter(target, i).getData()[0]).mapToDouble(v -> v[0]).min().getAsDouble();
                upperBound[i] = Stream.of(result2.filter(target, i).getData()[0]).mapToDouble(v -> v[0]).max().getAsDouble();
            }


        }else if (method.startsWith("CCALP")) {
            CausalInference inf3 = new CredalCausalAproxLP(model).setEpsilon(eps);
            queryStart = Instant.now();
            IntervalFactor result3 = (IntervalFactor) inf3.query(target, evidence, intervention);
            if(verbose) System.out.println(result3);

            for(int i=0; i<resultSize; i++) {
                lowerBound[i] = result3.getLower(0)[i];
                upperBound[i] = result3.getUpper(0)[i];
            }

        }else {
            throw new IllegalArgumentException("Unknown inference method");
        }

        Instant finish = Instant.now();
        double timeElapsed = Duration.between(start, finish).toNanos()/Math.pow(10,6);
        double timeElapsedQuery = Duration.between(queryStart, finish).toNanos()/Math.pow(10,6);


        double[] bounds = new double[resultSize*2];
        for(int i=0; i<resultSize; i++) {
            if (lowerBound[i] > upperBound[i]) {
                double aux = lowerBound[i];
                lowerBound[i] = upperBound[i];
                upperBound[i] = aux;
            }
            bounds[i*2]=lowerBound[i];
            bounds[i*2+1]= upperBound[i];
        }


        return Doubles.concat(new double[]{timeElapsed, timeElapsedQuery}, bounds);
    }


    public static double[] run() throws InterruptedException, TimeoutException, ExecutionException {

        double time = 0.0;
        double time2 = 0.0;
        double lbound[] = new double[resultSize];
        double ubound[] = new double[resultSize];

        ch.idsia.crema.utility.InvokerWithTimeout<double[]> invoker = new InvokerWithTimeout<>();

        // Warm-up
        for(int i=0; i<warmups; i++){
            //double[] out = RunExperiments.experiment();
            double[] out =  invoker.run(RunExperiments::experiment, TIMEOUT*2);
            System.out.println("Warm-up #"+i+" in "+out[0]+" ms.");
        }

        // Measures
        for(int i = 0; i< repetitions; i++){

            double[] out = invoker.run(RunExperiments::experiment, TIMEOUT);
            //double[] out = experiment();
            System.out.println("Measurement #"+i+" in "+out[0]+" ms.");

            time += out[0];
            time2 += out[1];

            for(int k = 0; k<resultSize; k++) {
                lbound[k] = out[k+2];
                ubound[k] = out[k+2+resultSize];
            }
        }

        System.out.println(Arrays.toString(lbound));


        return Doubles.concat(new double[] {time/repetitions, time2/repetitions},
                DoubleStream.of(lbound).map( v -> v).toArray(),
                DoubleStream.of(ubound).map( v -> v).toArray());

    }


}
