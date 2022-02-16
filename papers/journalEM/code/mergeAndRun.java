package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class mergeAndRun extends Terminal {

    /*

    -m 1 -M 2 --seed 0 --input ./papers/journalEM/output/triangolo/1000/ --output ./papers/journalEM/results/triangolo/1000/ 3 0

     */


    // Arguments
    @CommandLine.Option(names={"-i", "--input"}, description = "Input folder with the precise models in uai format. Default working dir.")
    String input = ".";

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    //static String input = "./papers/journalEM/output/triangolo/1000/";

    @CommandLine.Parameters(index = "0", description = "ID of the cause.")
    int cause;

    @CommandLine.Parameters(index = "1", description = "ID of the effect.")
    int effect;

    private static class Binary extends ArrayList<String> {
        Binary() { super(Arrays.asList("0","1")); }
    }

    @CommandLine.Option(names = {"-t", "--truestate"},
            completionCandidates = Binary.class,
            description = "Binary value for the true state. Defaults to 0")
    public void setTrueState(int value){
        trueState = value;
        falseState = Math.abs(1 - trueState);
    }

    private int trueState = 0, falseState=1;

    @CommandLine.Option(names={"-m", "--minsize"}, description = "Minimum number of precise models considered. Defaults to 1.")
    static int minsize = 1; //  -m  --minNumModels. Defaults to 1

    @CommandLine.Option(names={"-M", "--maxsize"}, description = "Maximum number of precise models considered. Defaults to as many as found in the folder")
    static int maxsize = -1;

    // Global variables
    List<StructuralCausalModel> points = null;
    List<double[]> results;
    Path wdir = null;
    int[] idx;

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new mergeAndRun(), args);
        if(errMsg!="")
            System.exit(-1);
    }

    private  void runInference() throws InterruptedException, ExecutionControl.NotImplementedException {
        logger.info("Starting inference tasks");


        // Inference loop
        for(int n = minsize; n<= maxsize; n++) {

            Watch.start();
            CausalMultiVE inf = new CausalMultiVE(points.subList(0, n));
            double[] pns = calculatePNS(inf, cause, effect, trueState, falseState);
            long time = Watch.stop();
            System.gc();
            logger.info("PNS with "+n+" models: "+Arrays.toString(pns)+"\t ellapsed time: "+time+" ms.");
            //"pns_low","pns_up","num_points", "idx", "time"
            results.add(new double[]{pns[0], pns[1], n, idx[n-minsize], time});
            logRuntimeStats();
        }
    }

    private void readModels() {

        String fullpath = wdir.resolve(input).toString();

        File[] files = new File(fullpath).listFiles((d, name) -> name.endsWith(".uai"));

        if(files.length>0) {

            //
            // Check the max and min sizes
            if(maxsize > files.length){
                maxsize = files.length;
                logger.warn("Setting maxsize to "+maxsize);
            }

            if(maxsize < minsize) {
                if(maxsize != -1){
                    logger.warn("maxsize input parameter was lower that the maximum size: "+ maxsize +"<"+ minsize);
                }
                maxsize = points.size();
                logger.info("Setting maxsize to"+ maxsize);
            }


            //Select randomly among the available precise models
            idx = CollectionTools.shuffle(IntStream.range(0, files.length).toArray());
            idx = IntStream.range(0,maxsize).map(i->idx[i]).toArray();

            logger.info("Reading models from " + fullpath);
            points =
                    IntStream.of(idx).mapToObj(i -> files[i])
                            .map(f -> readModel(f)).collect(Collectors.toList());
            logger.info("Index permutation: " + Arrays.toString(idx));
        }else{
            logger.warn("Loaded 0 precise models");
        }

    }


    public  StructuralCausalModel readModel(File f) {
        try {
            logger.debug("Reading model "+f.getAbsolutePath());
            return (StructuralCausalModel) IO.readUAI(f.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private double[] calculatePNS(CausalInference inf, int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException, ExecutionControl.NotImplementedException {
        VertexFactor p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect, trueState, falseState);
        double[] v =  Doubles.concat(p.getData()[0]);
        Arrays.sort(v);
        return new double[]{v[0], v[v.length-1]};

    }

    @Override
    protected void entryPoint() throws Exception {

        //wdir = Paths.get(".");
        wdir = Paths.get("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/");
        results = new ArrayList<double[]>();

        RandomUtil.setRandomSeed(seed);
        readModels();

        if(points != null) {
            runInference();
            String outputRes = "results_pns_" + cause + "_" + effect + "_true" + trueState +"_"+minsize+"to"+maxsize+"_seed" + seed + ".csv";
            String fullpath = wdir.resolve(output).resolve(outputRes).toString();
            logger.info("Saving results at at " + fullpath);
            new WriterCSV(results.toArray(double[][]::new), fullpath)
                    .setVarNames("pns_low", "pns_up", "num_points", "idx", "time").write();
        }
    }
}
