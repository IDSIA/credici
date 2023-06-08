package ch.idsia.credici.utility.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.collections.IteratorUtils;
import org.checkerframework.common.value.qual.IntRange;

import com.opencsv.exceptions.CsvException;

import ch.idsia.credici.Table;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM.StopCriteria;
import ch.idsia.credici.learning.inference.AceMethod;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.dot.DotSerialize;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.Probability;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

public class PNS {

    private static Options setup() {

        Option iter = new Option("i", "iter", true, "Set maximum number of iterations");
        iter.setArgs(1);
        iter.setOptionalArg(false);
        iter.setType(PatternOptionBuilder.NUMBER_VALUE);

        Option runs = new Option("r", "runs", true, "Set number of EM runs");
        runs.setArgs(1);
        runs.setOptionalArg(false);
        runs.setType(PatternOptionBuilder.NUMBER_VALUE);

        Option method = new Option("m", "method", true, "Inference method to be used [ace|ve] (ace)");
        method.setArgs(1);
        method.setType(PatternOptionBuilder.STRING_VALUE);
        
        Option model = new Option("f", "model", true, "Path of data file");
        model.setArgs(1);
        model.setRequired(true);
        model.setOptionalArg(false);
        model.setType(PatternOptionBuilder.FILE_VALUE);

        Option data = new Option("d", "data", true, "Path of data file");
        data.setArgs(1);
        data.setType(PatternOptionBuilder.FILE_VALUE);
        
        Option cc = new Option("c", "components", false, "Split model into components");
    
        Option acePath = new Option("a", "ace", true, "Path to the ace compiler script");
        acePath.setArgs(1);
        acePath.setType(PatternOptionBuilder.FILE_VALUE);

        Option threads = new Option("t", "threads", true, "Number of threads for parallel compoennts");
        threads.setArgs(1);
        threads.setType(PatternOptionBuilder.NUMBER_VALUE);
        
        Option c2d = new Option(null, "c2d", false, "Whether ace should use c2d rather than table (default)");
        
        Option output = new Option("o", "output", true, "Name of output file (default stdout)");
        output.setArgs(1);
        output.setRequired(false);

        Option header = new Option("h", "header", false, "Include header row in output");

        Option seed = new Option("s", "seed", true, "Random seed (default none)");
        seed.setArgs(1);
        seed.setType(PatternOptionBuilder.NUMBER_VALUE);

        Options options = new Options();
        options.addOption(new Option(null, "help", false, "print this message"));
        options.addOption(header);
        options.addOption(output);
        options.addOption(seed);
        options.addOption(iter);
        options.addOption(runs);
        options.addOption(method);
        options.addOption(model);
        options.addOption(data);
        options.addOption(cc);
        options.addOption(threads);

        options.addOption(acePath);
        options.addOption(c2d);
        
        return options;
    }

    static int intOrDefault(CommandLine cl, String option, int def) {
        String value = cl.getOptionValue(option);
        if (value == null) return def;
        else return Integer.parseInt(value);
    }

    static String stringOrDefault(CommandLine cl, String option, String def) {
        String value = cl.getOptionValue(option);
        if (value == null) return def;
        else return value;
    }




    public static List<StructuralCausalModel> inferenceCC(int method, String acepath, boolean useTable, int maxIter, int runs, Table table, StructuralCausalModel model, StructuralCausalModel[] random, boolean parallel, int threads, List<String> output, List<String> header) {
        ExecutorService executor = parallel ? 
            Executors.newFixedThreadPool(threads):
            Executors.newSingleThreadExecutor();

        CComponents cc = new CComponents();
        var scms = cc.apply(model, table);
      
        final Object lock = new Object();
        
        String[] lls = new String[scms.size()];
        String[] sizes = new String[scms.size()];

        String[] tjts = new String[scms.size()];
        String[] compile = new String[scms.size()];

        for (int i = 0; i < scms.size(); ++i) {
            final var x = scms.get(i);
            final int index = i;

            Runnable trace = new Runnable() {
                StructuralCausalModel cmodel = x.getLeft();
                Table csamples = x.getRight();
                int icomponent = index;
                public void run() {
                    try {
                        AceMethod ace = (method == 5) ? new AceMethod(acepath, useTable) : null;

                        EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                                .setMaxEMIter(maxIter)
                                .setNumTrajectories(runs)
                                .setThreshold(0)
                                .setStopCriteria(StopCriteria.MAX_ITER)
                                .setInferenceVariation(method)
                                .setInference(ace)
                                .setRandomModels(cmodel, random)
                                .build();

                        int size = method == 5 ? ace.getAceInference().getCircuitSize() : -1;

                        // components may be parallelly executed
                        // synchronize on a output lock
                        synchronized(lock) {
                            double[] ll = builder.getLikelihoods();
                            lls[icomponent] = Arrays.stream(ll).mapToObj(Double::toString).collect(Collectors.joining("|"));
                            sizes[icomponent] = ""+size;
                            compile[icomponent] = ""+builder.getCompileTime();
                            List<Long> tjt = builder.getTrajectoryTimes();
                            tjts[icomponent] = tjt.stream().<String>map(Object::toString).collect(Collectors.joining("|"));
                    
                        }

                        // lock (addResults is synchronized) also when reconciling the results 
                        cc.addResults(cmodel.getName(), builder.getSelectedPoints());
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            executor.submit(trace);
        }

        try{
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch(InterruptedException ex) { 
            ex.printStackTrace();
        }
        
        // just for the header alignments with Full
        header.add("FullLikelihoods");
        output.add("NA");
        header.add("FullCircuitSize");
        output.add("NA");
        header.add("FullCompileTime");
        output.add("NA");
        header.add("FullEMCCRunTimes");
        output.add("NA");

        // keep order
        header.add("CCCount");
        output.add("" + scms.size());

        
        String clikes = Arrays.stream(lls).collect(Collectors.joining(";"));
        String csizes = Arrays.stream(sizes).collect(Collectors.joining(";"));
        String ctimes = Arrays.stream(compile).collect(Collectors.joining(";"));
        String ttimes = Arrays.stream(tjts).collect(Collectors.joining(";"));
        
        header.add("CCLikelihoods");
        output.add(clikes);
        
        header.add("CCCircuitSizes");
        output.add(csizes);
        
        header.add("CCCompileTimes");
        output.add(ctimes);

        header.add("CCEMCCRunTimes");
        output.add(ttimes);

        return IteratorUtils.toList(cc.alignedIterator(), runs);
    }

    public static List<StructuralCausalModel> inferenceFull(int method, String acepath, boolean useTable, int maxIter, int runs, Table table, StructuralCausalModel model, StructuralCausalModel[] random, List<String> output, List<String> header) {
            var cmodel = model;
            var csamples = table;

            try {
                AceMethod ace = (method == 5) ? new AceMethod(acepath, useTable) : null;

                EMCredalBuilder builder = EMCredalBuilder.of(cmodel, csamples.convert())
                        .setMaxEMIter(maxIter)
                        .setNumTrajectories(runs)
                        .setThreshold(0)
                        .setStopCriteria(StopCriteria.MAX_ITER)
                        .setInferenceVariation(method)
                        .setInference(ace)
                        .setRandomModels(cmodel, random)
                        .build();

                double[] ll = builder.getLikelihoods();
                String lls = Arrays.stream(ll).mapToObj(Double::toString).collect(Collectors.joining("|"));
                header.add("FullLikelihoods");
                output.add(lls);

                int size = method == 5 ? ace.getAceInference().getCircuitSize() : -1;
                header.add("FullCircuitSize");
                output.add(""+size);

                header.add("FullCompileTime");
                output.add("" + builder.getCompileTime());

                List<Long> tjt = builder.getTrajectoryTimes();
                String tjts = tjt.stream().<String>map(v -> v.toString()).collect(Collectors.joining("|"));
                header.add("FullEMCCRunTimes");
                output.add(tjts);

                // just for the header alignments with CC
                CComponents cc = new CComponents();
                var scms = cc.apply(model, table);

                header.add("CCCount");
                output.add("" + scms.size());
        
                header.add("CCLikelihoods");
                output.add("NA");
                
                header.add("CCCircuitSizes");
                output.add("NA");
                
                header.add("CCCompileTimes");
                output.add("NA");
        
                header.add("CCEMCCRunTimes");
                output.add("NA");
                
                return builder.getSelectedPoints();
                
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
    }




    public static double[] pns(List<StructuralCausalModel> models, int cause, int effect) throws InterruptedException, NotImplementedException {
        return models.stream().mapToDouble(m->{
            CausalVE inf = new CausalVE(m);
            try {
                return inf.probNecessityAndSufficiency(cause, effect).getData()[0];
            } catch (NotImplementedException | InterruptedException e) {
                e.printStackTrace();
                return Double.NaN;
            }
        }).toArray();
    }

    static List<StructuralCausalModel> run(StructuralCausalModel causalModel, Table datatable, CommandLine cl, List<String> output, List<String> header) throws ParseException, FileNotFoundException, IOException, CsvException {
        
        int maxIter = intOrDefault(cl, "iter", 1000);
        int runs = intOrDefault(cl, "runs", 200);
        int threads = intOrDefault(cl, "threads", 1); //signle thread
        boolean parallel = threads > 1;
        int seed = intOrDefault(cl, "seed", 0);
        
        String method = stringOrDefault(cl, "method", "ace");
        
        int m;
        switch(method) {
            case "ace":
            case "ACE":
            case "5":
                m = 5;
                break;
            case "ve":
            case "2": 
                m = 2;
                break;
            default:
                m=Integer.parseInt(method);
                break;
        }
     

        StructuralCausalModel[] randomModels;
        if (cl.hasOption("seed")) {
            causalModel.initRandom(seed);
            randomModels = IntStream.range(0,runs).mapToObj(a->{
                var md = causalModel.copy();
                md.fillExogenousWithRandomFactors();
                return md;
            }).toArray(StructuralCausalModel[]::new);
        } else {
            randomModels = new StructuralCausalModel[0];
        }


        // ace only needed for m == 5
        String acepath = stringOrDefault(cl, "ace", "src/resources/ace/compile");
        boolean table = !cl.hasOption("c2d");
        
        List<StructuralCausalModel> models;
        boolean components = cl.hasOption("components");
        
        output.add(""+maxIter);     header.add("maxIter");
        output.add(""+runs);        header.add("numRuns");
        output.add(""+threads);     header.add("threads");
        String s = cl.hasOption("seed") ? ""+seed : "NA";
        output.add(""+s);        header.add("seed");
        output.add(""+method);      header.add("method");
        output.add(""+m);           header.add("methodCode");
        output.add(""+table);       header.add("forceTable");
        output.add(""+components);  header.add("components");

        long start = System.currentTimeMillis();
        if (components) {
            models = inferenceCC(m, acepath, table, maxIter, runs, datatable, causalModel, randomModels, parallel, threads, output, header);
        } else {
            models = inferenceFull(m, acepath, table, maxIter, runs, datatable, causalModel, randomModels, output, header);
        }
        start = System.currentTimeMillis() - start;
        output.add(""+start);  header.add("runtime");
        return models;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, CsvException {
        DefaultParser parser = new DefaultParser();
        Options options = setup();
        
        try {
            CommandLine cl = parser.parse(options, args);
            if (cl.hasOption("help")) throw new ParseException("Help requested");
            
            boolean rundot = false;
            boolean runace = false;
            boolean runpns = false;
            
            int cause = 0;
            int effect = 0;

            String[] left = cl.getArgList().toArray(String[]::new);
            if (left.length <= 2) {
                throw new ParseException("Action, Cause and affect must be specified!");
            } else {
                int len = left.length;
                cause = Integer.parseInt(left[len-2]);
                effect = Integer.parseInt(left[len-1]);
                for (len -= 3; len >= 0; --len) {
                    if ("ace".equals(left[len])) runace = true;
                    if ("pns".equals(left[len])) runpns = true;
                    if ("graph".equals(left[len])) rundot = true;
                }
            }
            
            File model = (File) cl.getParsedOptionValue("model");
            if (!model.exists()) throw new ParseException("No such file: " + model.getName());
            if (!model.canRead()) throw new ParseException("File not readable: " + model.getName());
    
            File data = (File) cl.getParsedOptionValue("data");
            if (data == null) {
                String fn = model.toString();
                int idx = fn.lastIndexOf(".");
                int path = fn.lastIndexOf(File.separator);
                if (idx == -1 || idx < path) data = new File(fn + ".csv");
                else data = new File(fn.substring(0, idx) + ".csv");
            }
            if (!data.exists()) throw new ParseException("No such file: " + data.getName());
    
            StructuralCausalModel causalModel =  new CausalUAIParser(model.toString()).parse();
            Table datatable = new Table(DataUtil.fromCSV(data.toString()));
            
            double maxll = Probability.maxLogLikelihood(causalModel, datatable.convert());
            
            List<String> header = new ArrayList<>();
            List<String> output = new ArrayList<>();
            output.add(model.toString()); header.add("modelpath");
            output.add(data.toString());  header.add("datapath");
            output.add("" + maxll);       header.add("maxLL");

            if (rundot) {
                DotSerialize ser = new DotSerialize();
                System.out.println(ser.run(causalModel));
            }
            if (runace || runpns) {
                List<StructuralCausalModel> models = run(causalModel, datatable, cl, output, header);
                output.add(""+cause);    header.add("cause");
                output.add(""+effect);   header.add("effect");

                if (runpns) {
                    double[] mm = pns(models, cause, effect);
                    header.add("action"); output.add("pns");
                    header.add("pnss"); output.add(DoubleStream.of(mm).mapToObj(Double::toString).collect(Collectors.joining("|")));
    
                    output.add(""+DoubleStream.of(mm).min().orElse(Double.NaN));  header.add("pns_l");
                    output.add(""+DoubleStream.of(mm).max().orElse(Double.NaN));  header.add("pns_u");
                }
            }


            String out = output.stream().collect(Collectors.joining(","));
            String h = header.stream().collect(Collectors.joining(","));
            
            if (cl.hasOption("output")) {
                String of = cl.getOptionValue("output");
                try(PrintWriter pw = new PrintWriter(new FileWriter(of))){
                    if (cl.hasOption("header")) 
                        pw.println(h);

                    pw.println(out);
                } catch(IOException ex) {
                    throw new ParseException(ex.getMessage());
                }
            } else {
                if (cl.hasOption("header")) 
                    System.out.println(h);
                System.out.println(out);
                    
            }

        } catch (ParseException pe) {
            System.err.println(pe.getMessage());
            System.err.println();

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PNS options action(ace, pns) cause effect", options);
        } catch (InterruptedException | NotImplementedException e) {
            e.printStackTrace();
        }
    }
}
