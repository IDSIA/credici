package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.ResultsManager;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*

--output ./papers/zeros/models/s4 -xs 2 -ys 3 -rw -s 1234

*/
//
public class GenerateNonBin extends Terminal {




    @CommandLine.Option(names = {"-xs", "--xsize"}, description = "Cardinality of the parent X. Default to 2")
    private int xsize = 2;

    @CommandLine.Option(names = {"-ys", "--ysize"}, description = "Cardinality of Y variable (child). Default to 2")
    private int ysize = 2;


    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names={"-rw", "--rewrite"}, description = "If activated, results are rewritten. Otherwise, process is stopped if there are existing results.")
    boolean rewrite = false;


    /// Global ///
    TIntIntMap[] data = null;

    StructuralCausalModel m = null;
    Path wdir = null;

    String label = "";


    Path outputFolder = null;
    Path outputFile = null;




    @Override
    protected void entryPoint() throws Exception {
        init();
        generate();
        sampleData();

        save();

    }


    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new GenerateNonBin(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    public void generate() throws IOException, CsvException, InterruptedException {



        SparseDirectedAcyclicGraph dag = DAGUtil.build("1,0");


        int[] endoVarSizes = new int[]{ysize,xsize};


        m = CausalBuilder.of(dag,endoVarSizes).build();
        m.fillExogenousWithRandomFactors(3);
        logger.info(String.valueOf(m));

    }


    private void sampleData() {
        data = m.samples(1000, m.getEndogenousVars());
        logger.info("Sampled "+data.length+" data instances");
    }


    public void init() throws IOException, CsvException, ExecutionControl.NotImplementedException, InterruptedException {

        wdir = Paths.get(".");
        RandomUtil.setRandomSeed(seed);
        logger.info("Starting logger with seed " + seed);

        label = "simple_1parent";

        label +="_x"+String.valueOf(xsize);
        label +="_y"+String.valueOf(ysize);

        label +="_"+seed;
        logger.info(label);

        outputFolder  = Path .of(output, "/");
        File f = new File(outputFolder.toUri());
        if (!f.exists()) {
            String msg = "Creating folder: " + outputFolder;
            f.mkdirs();
        }

        outputFile = outputFolder.resolve(label+".csv");
        if(!rewrite && new File(output).exists()){
            String msg = "Not rewriting. File exits: "+outputFile;
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }

    }


    public void save() throws IOException {

        String outputfile = null;

        outputfile = outputFolder.resolve(label+".csv").toString();
        DataUtil.toCSV(outputfile,data);
        logger.info("Saved data to: "+outputfile);

        outputfile = outputFolder.resolve(label+".uai").toString();
        IO.writeUAI(m, outputfile);
        logger.info("Saved model to: "+outputfile);

    }



}
