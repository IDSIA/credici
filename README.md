# Causal Expectation-Maximisation

This bundle contains the manuscript submitted to Neurips 2021 and entitled  "Causal Expectation-Maximisation".

The organisation of the sub-folders is the following:

- _experiments_: Java files for replicating the experiments.
- _lib_: packages needed for running the code.
- _models_: set of structural causal models in UAI format considered in the experimentation.
- _src_: sources files with implementing the proposed method and related functionality.
- pom.xml: maven configuration file required for building a jar file with the sources.


## Setup

The code has been tested with **Java openjdk 12.0.1**. For checking The Java version running in your system use the 
following command:

```
$ java --version
```


## Running CEM

The experiments provided in the paper can be replicated with the code in `experiments/RunCEM.java` which
will require the functionality at `experiments/causalem.jar`. Basically, this JAR package contains the code
in the sources folder together with the third party dependencies. For a description of how to run this code, we
can simply run the help menu as follows. 

```
java -cp lib/causalem.jar experiments/RunCEM.java --help
```

```
Usage: <main class> [-hq] [--simpleOutput] [-d=<dataSize>] [-f=<datafile>]
                    [-g=<infGroundTruth>] [-l=<logfile>] [-m=<maxiter>]
                    [-o=<outputFolder>] [-p=<method>] [-s=<seed>]
                    [-t=<timeout>] [-x=<executions>] <modelPath>
      <modelPath>           Model path in UAI format.
      --simpleOutput        If activated, log-likelihood and kl statistics are not
                              computed.
  -d, --datasize=<dataSize> Size of the sampled data. Default 1000
  -f, --datafile=<datafile> CSV file with the data. Default null
  -g, --groundtruth=<infGroundTruth>
                            Inference method for ground truth
  -h, --help                display a help message
  -l, --logfile=<logfile>   Output file for the logs.
  -m, --maxiter=<maxiter>   Maximum EM iterations per execution. Default 200
  -o, --output=<outputFolder>
                            Output folder for the results.
  -p, --policy=<method>     Selection Policy: LAST, BISECTION_BORDER_SAME_PATH,
                              BISECTION_BORDER, BISECTION_ALL
  -q, --quiet               Controls if log messages are printed to standard output.
  -s, --seed=<seed>         Random seed. Default 0
  -t, --timeout=<timeout>   Timeout in seconds. Default 6000s.
  -x, --executions=<executions>
                            Number of EM executions. Default 10
```


### Example

Command for one example and output




<!--  todo :
 
 - add models
 - working example with a tiny description 


-->
