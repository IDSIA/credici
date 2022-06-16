# Bounding Counterfactuals under Selection Bias

This bundle contains the manuscript submited to the PGM2022 and entitled  "Bounding Counterfactuals under Selection Bias".
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


## Running EMCC under selection bias

Here we present a minimal example in java for applying the method. This requires either the packages in `lib` folder, or importing
CREDICI 0.1.5 (through maven). The full code is available at `examples/ToyExample.java`

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

For the running example, let's consider that the joint states `(X=0,Y=0)` and `(X=1,Y=1)` are not available. So these are defined
in the array `hidden_conf`. The parents of the selector variable are `X` and `Y`. Now we can create the extended model for bias
selection and the selected data as follows.

```java    
// Non available configurations for X,Y
int[][] hidden_conf = new int[][]{{0,0},{1,1}};
int[] Sparents = new int[]{X,Y};

// Model with Selection Bias structure
StructuralCausalModel modelBiased = SelectionBias.addSelector(model, Sparents, hidden_conf);
int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];

// Biased data
TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);
```

Finally we can run EMCC and obtain a set of precise SCMs from which we can bound any query.

```java
logger.info("Running EMCC with selected data.");

// Learn the model
List endingPoints = SelectionBias.runEM(modelBiased, selectVar, dataBiased, maxIter, executions);

// Run inference
CausalMultiVE multiInf = new CausalMultiVE(endingPoints);
VertexFactor p = (VertexFactor) multiInf.probNecessityAndSufficiency(X, Y);
logger.info("PNS: ["+p.getData()[0][0][0]+", "+p.getData()[0][1][0]+"]");
```

## Pearl's model example

The code for replicating the running example of the manuscript is provided in 'examples/PearlBias.java'.

## Experiments with the synthetic models

The experiemnts has been conducted with the code  in `experiments/RunSBEMCC.java`. As an example, let us consider the model
in `./models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0.uai`. Then the following command run the experiments with r=10 and
a maximum number of iterations of 100.

```bash
java -cp lib/credici.jar experiments/RunSBEMCC.java -w -x 10 --maxiter 100 --seed 0 ./models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0.uai
```


The output generated is:

```
[2022-06-16T17:18:50.284515][INFO][java] Set up logging
[2022-06-16T17:18:50.287614][INFO][java] args: -w;-x;10;--maxiter;100;--seed;0;./models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0.uai
[2022-06-16T17:18:50.288887][INFO][java] Starting logger with seed 0
[2022-06-16T17:18:50.308021][INFO][java] Loaded model from: ././models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0.uai
[2022-06-16T17:18:50.340237][INFO][java] Loaded 1000 data instances from: ././models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0.csv
[2022-06-16T17:18:50.341137][INFO][java] Loaded exact query results from : ././models/synthetic/rand13_mk1_maxDist2_nEndo4_k05_0_queries.csv
[2022-06-16T17:18:50.341169][INFO][java] {}
[2022-06-16T17:18:50.344052][INFO][java] Random selector parents: [0, 1, 3]
[2022-06-16T17:18:50.344576][INFO][java] Ramdom selector assignments: 9
[2022-06-16T17:18:50.344675][INFO][java] Learning unbiased model (without selector)
[2022-06-16T17:19:07.059541][INFO][java] Performed 10 EM runs (without selector) in 16713.ms
[2022-06-16T17:19:07.062863][INFO][java] Computed ACE in 2.ms
[2022-06-16T17:19:07.062985][INFO][java] ACE: [0.1294396223433627, 0.3266801411281469]
[2022-06-16T17:19:07.069961][INFO][java] Computed PNS in 6.ms
[2022-06-16T17:19:07.070080][INFO][java] PNS: [0.1455015008311564, 0.3427855946399062]
[2022-06-16T17:19:07.075069][INFO][java] Learning multiple biased model (with selector)
[2022-06-16T17:19:07.079661][INFO][java] Learning model with p(S=1)=0.0
[2022-06-16T17:19:07.913836][INFO][java] Performed 10 EM runs in 833.ms
[2022-06-16T17:19:07.915460][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:19:07.915517][INFO][java] ACE: [-0.4334304157800184, 0.7309444643235502]
[2022-06-16T17:19:07.917805][INFO][java] Computed PNS in 2.ms
[2022-06-16T17:19:07.917889][INFO][java] PNS: [7.583275440886724E-4, 0.746241398037523]
[2022-06-16T17:19:07.920792][INFO][java] Learning model with p(S=1)=0.001
[2022-06-16T17:19:22.088521][INFO][java] Performed 10 EM runs in 14167.ms
[2022-06-16T17:19:22.089529][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:19:22.089590][INFO][java] ACE: [-0.5842947407343759, 0.7344643155280453]
[2022-06-16T17:19:22.091112][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:19:22.091187][INFO][java] PNS: [8.961383008012423E-4, 0.7498349113780419]
[2022-06-16T17:19:22.093441][INFO][java] Learning model with p(S=1)=0.003
[2022-06-16T17:19:36.387476][INFO][java] Performed 10 EM runs in 14293.ms
[2022-06-16T17:19:36.388464][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:19:36.388519][INFO][java] ACE: [-0.6222649947084717, 0.7043770899484163]
[2022-06-16T17:19:36.389946][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:19:36.390022][INFO][java] PNS: [4.3178664672145946E-4, 0.7197699663293708]
[2022-06-16T17:19:36.392212][INFO][java] Learning model with p(S=1)=0.979
[2022-06-16T17:20:45.132382][INFO][java] Performed 10 EM runs in 68739.ms
[2022-06-16T17:20:45.133362][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:20:45.133416][INFO][java] ACE: [0.08497901458757647, 0.7068376116701299]
[2022-06-16T17:20:45.134809][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:20:45.134936][INFO][java] PNS: [0.0857133555431684, 0.721087699726274]
[2022-06-16T17:20:45.136952][INFO][java] Learning model with p(S=1)=0.982
[2022-06-16T17:21:54.264166][INFO][java] Performed 10 EM runs in 69126.ms
[2022-06-16T17:21:54.265184][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:21:54.265240][INFO][java] ACE: [0.1044436027828371, 0.5723587921901689]
[2022-06-16T17:21:54.266616][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:21:54.266659][INFO][java] PNS: [0.10454729440230996, 0.5904707935203908]
[2022-06-16T17:21:54.268595][INFO][java] Learning model with p(S=1)=0.982
[2022-06-16T17:22:59.553195][INFO][java] Performed 10 EM runs in 65284.ms
[2022-06-16T17:22:59.554042][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:22:59.554097][INFO][java] ACE: [0.1044436027828371, 0.5723587921901689]
[2022-06-16T17:22:59.555430][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:22:59.555474][INFO][java] PNS: [0.10454729440230996, 0.5904707935203908]
[2022-06-16T17:22:59.557154][INFO][java] Learning model with p(S=1)=0.982
[2022-06-16T17:24:04.750359][INFO][java] Performed 10 EM runs in 65192.ms
[2022-06-16T17:24:04.751199][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:24:04.751253][INFO][java] ACE: [0.1044436027828371, 0.5723587921901689]
[2022-06-16T17:24:04.752398][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:24:04.752437][INFO][java] PNS: [0.10454729440230996, 0.5904707935203908]
[2022-06-16T17:24:04.754189][INFO][java] Learning model with p(S=1)=0.998
[2022-06-16T17:25:12.212502][INFO][java] Performed 10 EM runs in 67457.ms
[2022-06-16T17:25:12.213337][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:25:12.213392][INFO][java] ACE: [0.13025500787693362, 0.5734341077972138]
[2022-06-16T17:25:12.214852][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:25:12.214891][INFO][java] PNS: [0.14642485142134326, 0.5896803574407952]
[2022-06-16T17:25:12.216473][INFO][java] Learning model with p(S=1)=1.0
[2022-06-16T17:26:20.158764][INFO][java] Performed 10 EM runs in 67941.ms
[2022-06-16T17:26:20.159755][INFO][java] Computed ACE in 0.ms
[2022-06-16T17:26:20.159834][INFO][java] ACE: [0.13025500787693362, 0.5734341077972138]
[2022-06-16T17:26:20.161116][INFO][java] Computed PNS in 1.ms
[2022-06-16T17:26:20.161170][INFO][java] PNS: [0.14642485142134326, 0.5896803574407952]
[2022-06-16T17:26:20.163548][INFO][java] Saving info at:././rand13_mk1_maxDist2_nEndo4_k05_0_mIter100_wfalse_sparents3_x10_0.csv
[2022-06-16T17:26:20.167835][INFO][java] Closing log file
[2022-06-16T17:26:20.167877][INFO][java] Closed log file

```

For more details about its usage, run:


```bash
java -cp lib/credici.jar experiments/RunSBEMCC.java --help 
```
```
Usage: <main class> [-hqw] [--debug] [-as=<addSeed>] [-l=<logfile>]
                    [-m=<maxIter>] [-o=<output>] [-p=<numSelectorParents>]
                    [-r=<ratioConv>] [-s=<seed>] [-x=<executions>] <modelPath>
      <modelPath>           Model path in UAI format.
      --debug               Debug flag. Defaults to false
      -as, --addSeed=<addSeed>
                            Aaddional seed only for staring points. Defaults to 0
  -h, --help                Display a help message
  -l, --logfile=<logfile>   Output file for the logs.
  -m, --maxiter=<maxIter>   Maximum EM internal iterations. Default to 500
  -o, --output=<output>     Output folder for the results. Default working dir.
  -p, --sparents=<numSelectorParents>
                            Number of endogenous parents of the selector. Default to
                              3
  -q, --quiet               Controls if log messages are printed to standard output.
  -r, --ratioConv=<ratioConv>
                            For the output statistics, ratio of the maximum
                              interval. Default to 0.95
  -s, --seed=<seed>         Random seed. If not specified, it is randomly selected.
  -w, --weighted            If activated, improved weighted EM is run
  -x, --executions=<executions>
                            Number independent EM runs. Default to 20

```