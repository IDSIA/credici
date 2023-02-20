# Learning to Bound Counterfactual Inference from Observational, Biased and Randomised Data

This bundle contains the manuscript submitted to the _International Journal of Approximate Reasoning_ and entitled  "Learning to Bound Counterfactual Inference from Observational, Biased and Randomised Data".
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


## Running causal queries under selection bias

Here we present a minimal example in java for applying the selection bias method. This requires either the packages in `lib` folder, or importing
CREDICI jar file given in the lib folder. The full code is available at `examples/MuellerExampleBias.java`


In CREDICI, variables and states are defined with a integer identifyer. For simplicity of the code, let's associate them to variables:

```java
int T = 0;  //  Treatment
int S = 1;  // Survival
int G = 2;  // Gender

// states for G, T and S
int female=1, drug=1, survived=1;
int male=0, no_drug=0, dead=0;

```

Set the relevant paths, which might change depending on your setup.

```java
// Relevant paths (update)
String wdir = ".";
String dataPath = Path.of(wdir, "./papers/journalPGM/models/literature/").toString();

```

Load the data and the model.

```java
StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());
TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());
```

These are the hidden states specifyied in gray in Table 1:


```java
// Set the hidden configurations
int[][] hidden = {
{no_drug, survived, male},
{no_drug, dead, male},
{drug, survived, female},
{drug, dead, female},
};
int[] Sassig = SelectionBias.getAssignmentWithHidden(model, new int[]{T,S,G}, hidden);


```

Build the model augmented with the additional selector variable. Extend the dataset as well.

```java
// Get the extended biased model and the biased data
StructuralCausalModel modelBiased = SelectionBias.addSelector(model, new int[]{T,S,G}, Sassig);
int Svar = SelectionBias.findSelector(modelBiased);
TIntIntMap[] biasedData = SelectionBias.applySelector(dataObs, modelBiased, Svar);

```

Run the inference

```java

EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, biasedData)
.setNumTrajectories(20)
.setWeightedEM(true)
.setVerbose(false)
.setMaxEMIter(200)
.build();

CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
VertexFactor resBiased = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);
System.out.println(resBiased);

```

## Running causal queries from hybrid data

Here we illustrate how to run learn a causal model from observational and interventional data. The full code 
is available at `examples/MuellerExampleHybrid.java`

Assuming that we have loaded the observational data and model (as in previous section), now load the interventional data:

```java

// Load the intervened data
TIntIntMap[] dataDoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoDrug.csv").toString());
TIntIntMap[] dataDoNoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoNoDrug.csv").toString());
TIntIntMap[] dataInt = DataUtil.vconcat(dataDoDrug, dataDoNoDrug);
```
Now, we will define an object of class `DataIntegrator` for managing the data integration.

```java
// Set integrator object to build the extended model and data
DataIntegrator integrator = DataIntegrator.of(model);
integrator.setObservationalData(dataObs);
integrator.setData(dataInt, new int[] {T});

TIntIntMap[] dataExt = integrator.getExtendedData();
StructuralCausalModel modelExt = integrator.getExtendedModel();

```

Finally, run the inference.

```java

EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
.setNumTrajectories(20)
.setWeightedEM(true)
.setVerbose(false)
.setMaxEMIter(200)
.build();

// Rebuild a simple model with the learned parameters
List selectedPoints = builder.getSelectedPoints().stream().map(m -> m.subModel(model.getVariables())).collect(Collectors.toList());


CausalMultiVE inf = new CausalMultiVE(selectedPoints);
VertexFactor resHybrid = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);
System.out.println(resHybrid);





```

## Experiments with the synthetic models

The experiemnts has been conducted with the code  in `experiments/LearnSelectBias.java` and `experiments/LearnMultiStudy.java`. 
For details about its usage, run:

```bash
java -cp lib/credici.jar experiments/LearnSelectBias.java --help 
```
```bash
Usage: <main class> [-hqw] [--debug] [-rw] [-as=<addSeed>] [-l=<logfile>]
                    [-m=<maxIter>] [-o=<output>] [-p=<numSelectorParents>]
                    [-r=<ratioConv>] [-s=<seed>] [-to=<timeout>]
                    [-x=<executions>] <modelPath>
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
      -rw, --rewrite        If activated, results are rewritten. Otherwise, process
                              is stopped if there are existing results.
  -s, --seed=<seed>         Random seed. If not specified, it is randomly selected.
      -to, --timeout=<timeout>
                            Timeout in seconds for the exact inference. Default to
                              120
  -w, --weighted            If activated, improved weighted EM is run
  -x, --executions=<executions>
                            Number independent EM runs. Default to 20


```

```bash
java -cp lib/credici.jar experiments/LearnMultiStudy.java --help 
```
```bash

Usage: <main class> [-hqw] [--debug] [-lp] [-rw] [-l=<logfile>] [-m=<maxIter>]
                    [-o=<output>] [-s=<seed>] [-sc=<stopCriteria>]
                    [-th=<threshold>] [-tps=<targetPS>] [-x=<executions>]
                    <modelPath>
      <modelPath>           Model path in UAI format.
      --debug               Debug flag. Defaults to false
  -h, --help                Display a help message
  -l, --logfile=<logfile>   Output file for the logs.
      -lp, --localparam     One of the exogenous variables is set as local parameter
  -m, --maxiter=<maxIter>   Maximum EM internal iterations. Default to 300
  -o, --output=<output>     Output folder for the results. Default working dir.
  -q, --quiet               Controls if log messages are printed to standard output.
      -rw, --rewrite        If activated, results are rewritten. Otherwise, process
                              is stopped if there are existing results.
  -s, --seed=<seed>         Random seed. If not specified, it is randomly selected.
      -sc, --stopcriteria=<stopCriteria>
                            Stopping criteria: KL, L1, LLratio
      -th, --threshold=<threshold>
                            KL threshold for stopping EM execution. Default to 0.0
      -tps, --targetps=<targetPS>
                            Target P(S=1). Default to 0.25
  -w, --weighted            If activated, improved weighted EM is run
  -x, --executions=<executions>
                            Number independent EM runs. Only for EM-based methods.
                              Default to 40


```