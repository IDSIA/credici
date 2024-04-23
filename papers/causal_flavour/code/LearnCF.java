package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.*;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.*;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

/*


-m 1 -output ./papers/causal_flavour/learntmodels -rw -s 1234 ./papers/causal_flavour/models/simple_learner_10Q.uai ./papers/causal_flavour/data/simple_learner_10Q_data.csv


*/
//
public class LearnCF extends Terminal {
    @CommandLine.Parameters(description = "Model path in UAI format.")
    private String modelPath;

    @CommandLine.Option(names = {"-d", "--data"}, description = "Dataset in format csv")
    private String dataPath;

    @CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM internal iterations. Default to 500")
    private int maxIter = 500;

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names={"-rw", "--rewrite"}, description = "If activated, results are rewritten. Otherwise, process is stopped if there are existing results.")
    boolean rewrite = false;




    /// Global ///
    TIntIntMap[] data = null;
    StructuralCausalModel model = null;
    Path wdir = null;


    double ll_max = 0;

    Path outputFolder = null;
    Path outputFile = null;
    String modelname = null;




    @Override
    protected void entryPoint() throws Exception {
        init();
        learn();
    }

    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new code.GenerateNparents(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }

    public void learn() throws IOException, CsvException, InterruptedException {
        // Get the labels from the model
        LabelInfo info = LabelInfo.from(modelPath.replace(".uai","/domains.csv"));
        HashMap<Integer,String> varnames = info.getVarNames();
        HashMap<Integer,String[]> domainnames = info.getDomainNames();

        // Read data and model
        StructuralCausalModel model = (StructuralCausalModel) IO.read(modelPath);
        this.logger.info("Loaded model and info: "+modelPath);

        TIntIntMap[] data = DataUtil.fromCSV(dataPath);
        this.logger.info("Loaded "+data.length+" data instances");


        this.logger.info("Starting learning up to "+maxIter+" iterations");
        Watch.start();
        CausalEMVE inf = new CausalEMVE(model, data, 1, maxIter);
        Watch.stopAndLog(this.logger);



        // Save the models
        String filepath = String.valueOf(outputFile);
        IO.write(inf.getInputModels().get(0), filepath);
        this.logger.info("Saved model at "+filepath);




    }


    public void init() throws IOException, CsvException, ExecutionControl.NotImplementedException, InterruptedException {

        wdir = Paths.get(".");
        RandomUtil.setRandomSeed(seed);
        logger.info("Starting logger with seed " + seed);

        modelname = Arrays.stream(modelPath.split("/")).reduce((f,s)->s).get().replace(".uai","");


        outputFolder  = Path .of(output, "/"+modelname+"/");
        File f = new File(outputFolder.toUri());
        if (!f.exists()) {
            String msg = "Creating folder: " + outputFolder;
            f.mkdirs();
        }

        outputFile = outputFolder.resolve("model_"+seed+".uai");
        if(!rewrite && new File(output).exists()){
            String msg = "Not rewriting. File exits: "+outputFile;
            logger.severe(msg);
            throw new IllegalStateException(msg);
        }


/*
        // Load model
        String fullpath = wdir.resolve(modelPath).toString();
        model = (StructuralCausalModel) IO.readUAI(fullpath);
        logger.info("Loaded model from: " + fullpath);

        String modelname = Arrays.stream(modelPath.split("/")).reduce((f,s)->s).get().replace(".uai","")

        LabelInfo info = LabelInfo.from(modelPath.replace(".uai","/domains.csv"));
        HashMap<Integer,String> varnames = info.getVarNames();
        HashMap<Integer,String[]> domainnames = info.getDomainNames();
*/
        /*

        // Get the labels from the model
        LabelInfo info = LabelInfo.from(modelsFolder.resolve(modelname+"/domains.csv"));
        HashMap<Integer,String> varnames = info.getVarNames();
        HashMap<Integer,String[]> domainnames = info.getDomainNames();

        // Read data and model
        String modelpath = modelsFolder.resolve(modelname+".uai").toString();
        StructuralCausalModel model = (StructuralCausalModel) IO.read(modelpath);
        String datapath = dataFolder.resolve(modelname+"_data.csv").toString();
        TIntIntMap[] data = DataUtil.fromCSV(datapath);
*/

    }

}
