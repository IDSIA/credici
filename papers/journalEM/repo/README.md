# On the Efficient Bounding of Counterfactual Queries

This bundle contains the manuscript submitted to the _International Journal of Approximate Reasoning_ and entitled  "On the Efficient Bounding of Counterfactual Queries".
The organisation  is the following:

- _example_: code for replicating the examples in the paper and an additional toy example.
- _experiments_: Java files for replicating the experiments.
- _lib_: packages needed for running the code.
- _models_: set of structural causal models in UAI format considered in the experimentation.



## Setup

The code has been tested with **Java openjdk 12.0.1**. For checking The Java version running in your system use the
following command:

```bash
$ java --version
```

```
openjdk 12.0.1 2019-04-16
OpenJDK Runtime Environment (build 12.0.1+12)
OpenJDK 64-Bit Server VM (build 12.0.1+12, mixed mode, sharing)
```


## Running causal queries with CCVE, CCALP and EMMCC

Here we present a minimal example in java for applying the methods. This requires either the packages in `lib` folder, or importing
CREDICI jar file given in the lib folder. The full code is available at `examples/ToyExample.java`

First, we define two DAGs for characterizing a SCM. First the DAG only with the endogenous variables $X,Y$ and then the one
also including the exogenous variables $U,V$.

```java
// Variables IDs
int X = 0, Y = 1;
int U = 2, V = 3;

// Define endogenous DAG and complete DAG of the SCM
SparseDirectedAcyclicGraph endoDAG = DAGUtil.build("(0,1)");
SparseDirectedAcyclicGraph causalDAG = DAGUtil.build("(0,1),(2,0),(3,1)");
```

Now we can create an object representing the SCM with a conservative specification and random PMF for the exogenous variables.

```java
// Create the SCM with random probabilities for the exogenous variables
StructuralCausalModel model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();
model.fillExogenousWithRandomFactors(5);

logger.info("Model structure: "+model.getNetwork());
logger.info("Model parameters: "+model);
```

Now, the dataset (before selection) is sampled.

```java
// Sample a complete dataset
TIntIntMap[] data = model.samples(1000, model.getEndogenousVars());
// Empirical endogenous distribution from the data
HashMap empiricalDist = DataUtil.getEmpiricalMap(model, data);
empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

logger.info("Sampled complete data with size: "+data.length);
```

In this example, the probability of necessity and sufficiency (PNS) will be calculated with 3 different methods.
First, it is needed to defined the cause and the effect variables.

```java

// Determine the cause and the effect variables
int cause = 0, effect = 1;

```

With the algorithms CCVE and CCALP we just need to initialize the corresponding objects, and then invoke the method  
``probNecessityAndSufficiency`` as follows.

```java
// Credal vausal VE
CredalCausalVE ccve = new CredalCausalVE(model, empiricalDist.values());
VertexFactor pnsCCVE = ccve.```

        ```java(cause,effect);
logger.info("CCVE result: \n"+ pnsCCVE);

// Approx LP
CredalCausalApproxLP  alp = new CredalCausalApproxLP(model, empiricalDist.values());
IntervalFactor pnsALP = alp.probNecessityAndSufficiency(cause,effect);
logger.info("CCALP result: \n"+ pnsALP);

```

The code for running EMCC is sligthly different. It first requires to run the builder which will actually run
EMCC and generate a set of resulting precise SCMs. Then the inference object of class `CausalMultiVE` will carry out
the inference at each SCM and gather the results.

```java

// EMCC
EMCredalBuilder builder = EMCredalBuilder.of(model, data)
        .setMaxEMIter(maxIter)
        .setNumTrajectories(executions)
        .setWeightedEM(true)
        .build();
CausalMultiVE multiVE = new CausalMultiVE(builder.getSelectedPoints());
VertexFactor pnsEMCC = (VertexFactor) multiVE.probNecessityAndSufficiency(cause,effect);
logger.info("EMCC result: \n"+ pnsEMCC);



```

The output for this code will be:

```
[2023-01-15T20:01:10.498790][INFO][java] Model structure: ([0, 1, 2, 3], [(0,1), (2,0), (3,1)])
[2023-01-15T20:01:10.547089][INFO][java] Model parameters: 
[P([3]) [0.29855, 0.5036, 0.14176, 0.05609], P([2]) [0.73394, 0.26606], P([1, 0, 3]) [1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0], P([0, 2]) [1.0, 0.0, 0.0, 1.0]]

[2023-01-15T20:01:10.808099][INFO][java] Sampled complete data with size: 1000
[2023-01-15T20:01:13.404368][INFO][java] CCVE result: 
K(vars[]|[])
K(vars[]|[]) [0.5730299999999999]
             [0.38749]

[2023-01-15T20:01:13.815654][INFO][java] CCALP result: 
P([] | [])
	[0.3874899999999999]
	[0.5730299999999999]
[2023-01-15T20:01:14.994351][INFO][java] EMCC result: 
K(vars[]|[])
K(vars[]|[]) [0.3874948230439201]
             [0.571624184625037]


```


## Experiments with the synthetic models

The experiemnts has been conducted with the code  in `experiments/BoundPNS.java`. As an example, let us consider the model
in `./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai`. Then the following command run the experiments with r=10 and
a maximum number of iterations of 100.

```bash
java -cp lib/credici.jar experiments/BoundPNS.java -rw -w -x 10 -m 100 -a EMCC --debug --seed 0 ./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai

```


The output generated is:

```
[2023-01-15T20:29:06.848559][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Set up logging
[2023-01-15T20:29:06.850090][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] args: -rw;-w;-x;10;-m;100;-a;EMCC;--debug;--seed;0;./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai
[2023-01-15T20:29:06.850134][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Checking arguments
[2023-01-15T20:29:06.850977][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Starting logger with seed 0
[2023-01-15T20:29:06.871329][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Loaded model from: ./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12.uai
[2023-01-15T20:29:06.874119][DEBUG][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Markovianity: 0
[2023-01-15T20:29:06.905182][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Loaded 1000 data instances from: ./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12.csv
[2023-01-15T20:29:06.905800][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Loaded model information from: ./models/synthetic/random_mc2_n5_mid3_d1000_05_mr098_r10_12_info.csv
[2023-01-15T20:29:06.929977][DEBUG][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Empirical from data: {[0]=P([0, 4]) [0.01214, 0.98786, 0.52128, 0.47872], [1]=P([1, 4]) [0.98565, 0.01435, 0.85106, 0.14894], [2]=P([0, 1, 2]) [0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0], [4]=P([4]) [0.906, 0.094]}
[2023-01-15T20:29:06.930063][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Learning exogenous variables with algorithm: EMCC
[2023-01-15T20:29:08.109626][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Finished learning in: 1178.ms
[2023-01-15T20:29:08.109710][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Starting inference
[2023-01-15T20:29:08.109987][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Determining cause=4
[2023-01-15T20:29:08.110035][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Determining effect=2
[2023-01-15T20:29:08.110806][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Determining query: cause=4, effect=2
[2023-01-15T20:29:08.122747][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Finished inference in: 11.ms
[2023-01-15T20:29:08.123656][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] PNS interval = [0.007338211619292894,0.014173633095352967]
[2023-01-15T20:29:08.123759][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Total time: 1189 ms.
[2023-01-15T20:29:08.123889][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Checking exact results at:./random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_ccve.csv
[2023-01-15T20:29:08.124018][WARN][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Exact results not found.
[2023-01-15T20:29:08.125410][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Average trajectory size: 100.0
[2023-01-15T20:29:08.167106][DEBUG][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Average ratio: 0.9999999975396798
[2023-01-15T20:29:08.167356][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Saving info at:./random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0.csv
[2023-01-15T20:29:08.169262][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Closing log file
[2023-01-15T20:29:08.169315][INFO][random_mc2_n5_mid3_d1000_05_mr098_r10_12_uai_emcc_kl_th00_mIter100_wtrue_x10_0] Closed log file
```

For more details about its usage, run:


```bash
java -cp lib/credici.jar experiments/BoundPNS.java --help 
```
```
Usage: <main class> [-hqw] [--debug] [-rw] [-a=<alg>] [-c=<inputCause>]
                    [-e=<inputEffect>] [-ii=<initIndex>] [-l=<logfile>]
                    [-m=<maxIter>] [-o=<output>] [-s=<seed>]
                    [-sc=<stopCriteria>] [-th=<threshold>] [-x=<executions>]
                    <modelPath>
      <modelPath>            Model path in UAI format.
      --debug                Debug flag. Defaults to false
  -a, --algorithm=<alg>      Learning and inference algorithm: CCVE, CCALP, EMCC
  -c, --cause=<inputCause>   Cause in the PNS query. Default to the 1st node in the
                               topological order.
  -e, --effect=<inputEffect> Effect in the PNS query. Default to the last node in
                               the topological order.
  -h, --help                 Display a help message
      -ii, --initindex=<initIndex>
                             Initial index for the results. Default to 0
  -l, --logfile=<logfile>    Output file for the logs.
  -m, --maxiter=<maxIter>    Maximum EM internal iterations. Default to 300
  -o, --output=<output>      Output folder for the results. Default working dir.
  -q, --quiet                Controls if log messages are printed to standard output.
      -rw, --rewrite         If activated, results are rewritten. Otherwise, process
                               is stopped if there are existing results.
  -s, --seed=<seed>          Random seed. If not specified, it is randomly selected.
      -sc, --stopcriteria=<stopCriteria>
                             Stopping criteria: KL, L1, LLratio
      -th, --threshold=<threshold>
                             KL threshold for stopping EM execution. Default to 0.0
  -w, --weighted             If activated, improved weighted EM is run
  -x, --executions=<executions>
                             Number independent EM runs. Only for EM-based methods.
                               Default to 40

```