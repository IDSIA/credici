package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.builder.CompatibleCausalModelGenerator;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.crema.utility.RandomUtil;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static ch.idsia.credici.utility.Assertion.assertTrue;

public class GenerateModel extends Terminal {

    /*
      -n 5 -r 0.5 --debug -s 1234 -rw

      -n 6 --reduction 0.25 --seed 1

       -n 6 --reduction 0.25 --seed 1

       -n 6 -d 1000 -di 0.5 -mid 3 -mr 0.98 -r 1.0 --seed 1 -rw

    */

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
    String outputPath = ".";

    @CommandLine.Option(names = {"-n", "--numnodes"}, description = "Number of nodes in the initial sampled DAG. Default 5")
    private int numNodes = 5;

    @CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
    private int dataSize = 1000;

    @CommandLine.Option(names = {"-di", "--dataincrement"}, description = "Data increment factor. Default 0.5")
    private double dataIncrement = 0.5;

    @CommandLine.Option(names = {"-mid", "--maxindegree"}, description = "Size of the sampled data. Default 1000")
    private int maxindegree = 3;

    @CommandLine.Option(names = {"-mr", "--minratio"}, description = "Minimum log-lk ratio between the model and the data. Defaults to 0.95")
    private double minRatio = 0.98;

    @CommandLine.Option(names = {"-r", "--reduction"}, description = "Reduction constant for the exogenous domains. Defaults to 1.0")
    private double reductionK = 1.0;

    @CommandLine.Option(names = { "-rw", "--rewrite" }, description = "Rewrite model if file exists. Defaults to false.")
    private boolean rewrite = false;


    // Global variables
    private  CompatibleCausalModelGenerator gen = null;

    @Override
    protected String getLabel(){
        String id = "random_mc2";
        id += "_n"+numNodes;
        id += "_mid"+String.valueOf(maxindegree);
        id += "_d"+dataSize+"_"+String.valueOf(dataIncrement).replace(".","");
        id += "_mr"+String.valueOf(minRatio).replace(".","");
        id += "_r"+String.valueOf(reductionK).replace(".","");
        id += "_"+seed;
        return id;
    }



    @Override
    protected void checkArguments() {
        logger.info("Checking arguments");
        assertTrue( numNodes>2, " Wrong value for numNodes: "+numNodes);
        assertTrue( dataSize>0, " Wrong value for dataSize: "+dataSize);
        assertTrue( reductionK>=0.0 && reductionK<=1.0, " Wrong value for reductionK: "+reductionK);
        assertTrue( minRatio>=0.0 && minRatio<=1.0, " Wrong value for minRatio: "+minRatio);
    }
    Path wdir = null;

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new GenerateModel(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    @Override
    protected void entryPoint() throws IOException, ExecutionException, InterruptedException, TimeoutException {

        wdir = Paths.get(".");

        logger.setLabel(getLabel());
        logger.info("Starting with seed "+seed);
        RandomUtil.setRandomSeed(seed);

        String targetFile = getTargetPath(".csv").toString();
        if(!rewrite && new File(targetFile).exists()){
            logger.info("Not rewriting. File exits: "+targetFile);
            return;
        }

        generate();
        save();

    }


    private void generate() {
        gen = new CompatibleCausalModelGenerator()
                .setMaxIndegree(maxindegree)
                .setMaxDataResamples(10)
                .setMaxExoFactorsResamples(5)
                .setMaxCofoundedVars(2)
                .setMinCompatibilityDegree(minRatio)
                .setDatasize(dataSize)
                .setDataIncrementFactor(dataIncrement)
                .setNumNodes(numNodes);

        gen.run();
    }

    private Path getTargetPath(String extension){
        String filename = getLabel();
        wdir = Paths.get(this.outputPath);

        return wdir.resolve(filename+extension).toAbsolutePath();
    }

    private void save() throws IOException {

        String fullpath;

        fullpath = getTargetPath(".csv").toString();
        logger.info("Saving data at:" +fullpath);
        DataUtil.toCSV(fullpath, gen.getData());

        fullpath = getTargetPath("_info.csv").toString();
        logger.info("Saving info at:" +fullpath);
        List<HashMap> info = new ArrayList<>();
        info.add(gen.getStatistics().getInfo());
        DataUtil.toCSV(fullpath, info);

        fullpath = getTargetPath(".uai").toString();
        logger.info("Saving model at:" +fullpath);
        IO.write(gen.getModel(), fullpath);


    }

}
