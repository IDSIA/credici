package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.experiments.ResultsManager;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class AsiaPNS extends Terminal {

    /*

    -x 5 -m 50 -a EMCC /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/models/literature/

     */

    @CommandLine.Parameters(description = "Folder with the model asia.uai")
    private String modelPath;


    @CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM internal iterations. Default to 500")
    private int maxIter = 500;


    @CommandLine.Option(names={"-rw", "--rewrite"}, description = "If activated, results are rewritten. Otherwise, process is stopped if there are existing results.")
    boolean rewrite = false;

    @CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
    String output = ".";

    @CommandLine.Option(names = {"-x", "--executions"}, description = "Number independent EM runs. Only for EM-based methods. Default to 40")
    private int executions = 40;


    @CommandLine.Option(names = {"-a", "--algorithm"}, description = "Learning and inference algorithm: ${COMPLETION-CANDIDATES}")
    private AsiaPNS.algorithms alg = AsiaPNS.algorithms.CCVE;


    public enum algorithms {
        CCVE,   // Credal causal VE
        CCALP,  // Credal causal approx LP
        EMCC,   // EM

    }


    @CommandLine.Option(names = {"-c", "--cause"}, description = "Cause variable. Default to smoke. Possible values: smoke, bronc, dysp, either, lung, tub, asia, xray")
    private String cause = "smoke";
    private String effect = "dysp";

    // Global Variables
    String[] varnames = {"smoke","bronc","dysp","either","lung","tub","asia","xray"};
    TIntIntMap[] data = null;
    StructuralCausalModel model = null;
    int yes = 0;
    int no = 1;

    ResultsManager res = null;

    // Outputs
    List<Double> pnsPoints = new ArrayList<>();
    List<Double> llk = new ArrayList<>();
    double maxllk = Double.NaN;
    double lb = Double.NaN;
    double ub = Double.NaN;
    double timelearn = Double.NaN;
    double  timeinference = Double.NaN;

    private void processResults() throws IOException {
        res.add("pns", CollectionTools.toDoubleArray(pnsPoints));
        res.add("llk", CollectionTools.toDoubleArray(llk));
        res.add("pns_u", ub);
        res.add("pns_l", lb);
        res.add("time_learn", timelearn);
        res.add("time_inf", timeinference);
        res.add("cause", cause);
        res.add("effect", effect);
        res.add("method", alg.toString());
        res.add("max_llk", maxllk);

        res.logSummary();

        Path fullpath = Path.of(output, getLabel()+".csv");

        if(rewrite || !fullpath.toFile().exists())
            res.save(fullpath.toAbsolutePath().toString());

    }


    @Override
    protected void entryPoint() throws Exception {

        init();
        loadModel();
        runInference();
        processResults();   //todo: set result path

    }

    protected void init(){
        res = new ResultsManager()
                .setIncludeLabel(false).setLogger(logger);
    }

    protected String getLabel(){
        return getLabel(this.alg);
    }

    private String getLabel(algorithms alg) {
        //mIter500_wtrue_sparents3_x20_0
        String str = "asia";

        str+="_"+alg;

        if(alg == algorithms.EMCC) {
            str += "_mIter" + this.maxIter;
            str += "_x" + this.executions;
        }
        str += "_"+cause;
        str += "_"+effect;



        if(alg != algorithms.CCVE) {
            str += "_" + this.seed;
        }

        return str;
    }
    private void runInference() throws InterruptedException, ExecutionControl.NotImplementedException {

        int y = List.of(varnames).indexOf("dysp");
        int x = List.of(varnames).indexOf(cause);

        logger.debug("X="+x+" ("+cause+"), Y="+y+" ("+effect+")");

        maxllk = Probability.maxLogLikelihood(model,data);

        logger.debug("Max llk: "+maxllk);

        String method = alg.toString();

        CausalInference inf = null;

        logger.info("Starting learning");

        Watch.start();

        if(method=="CCVE")
            inf = new CredalCausalVE(model, data);
        else if(method=="CCALP")
            inf = new CredalCausalApproxLP(model,data);
        else if(method=="EMCC")
            inf = new CausalEMVE(model, data, executions, maxIter);
        else
            throw new IllegalArgumentException("Unknown inference method");

        Watch.stopAndLog(this.logger, "Learning finished in ");
        timelearn = Watch.getTime();

        Watch.start();

        if(method != "EMCC") {
            GenericFactor res = inf.probNecessityAndSufficiency(x, y, yes, no);
            if (!(res instanceof IntervalFactor)) {
                res = new VertexToInterval().apply((VertexFactor) res, y);
            }
            lb = ((IntervalFactor) res).getDataLower()[0][0];
            ub = ((IntervalFactor) res).getDataUpper()[0][0];
            pnsPoints.add(lb);
            pnsPoints.add(ub);
        }else{
            double pns[] = (((CausalEMVE) inf).getIndividualPNS(x, y, yes, no));
            pnsPoints = DoubleStream.of(pns).boxed().collect(Collectors.toList());
            ub = Arrays.stream(pns).max().getAsDouble();
            lb = Arrays.stream(pns).min().getAsDouble();
            llk = ((CausalEMVE) inf).getInputModels().stream().map(m -> m.logLikelihood(data)).collect(Collectors.toList());

            logger.debug("PNS: "+pnsPoints);
            logger.debug("llk: "+llk);


        }

        Watch.stopAndLog(this.logger, "Inference finished  in ");
        timeinference = Watch.getTime();


        logger.info("PNS(" + cause + ","+effect+") = [" + lb + "," + ub + "]\t ("+method+")");



    }

    private void loadModel() throws IOException {

        String path = Path.of(modelPath, "asia.uai").toString();

        logger.info("Loading model from"+path);
        // Load the data and the model
        BayesianNetwork bnet = (BayesianNetwork) IO.readUAI(path);

        RandomUtil.setRandomSeed(0);
        data = bnet.samples(1000, bnet.getVariables());

        RandomUtil.setRandomSeed(seed);
        model = CausalBuilder.of(bnet.getNetwork(), 2).build();

        logger.info("Built markovian model");


    }


    public static void main(String[] args) {
        argStr = String.join(";", args);
        CommandLine.run(new AsiaPNS(), args);
        if(errMsg!="")
            System.exit(-1);
        System.exit(0);
    }


}
