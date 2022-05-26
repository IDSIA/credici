Result files format
==========================

Each csv file corresponds to the results obtained from a single base model (i.e., SCM without the selector). The file name
indicates:

- mk0 or mk1 indicates if the model is markovian (0) or quasi-markovian (1).
- maxDist indicates the maximum distance between cofounded variables. Only for quasimarkovian.
- nEndo indicates the number of endogenous variables.
- k indicates the reduction factor of the exogenous variables.
- wtrue or wfalse indicates if the weighted EM version is used.
- sparents indicates the number of endogenous parents of the selector variable.
- x indicates the number of EM executions
- the last number indicates the random seed.

The fields in the csv file are described below. Each row corresponds to a single learning task. Note that some missing values might be present:

- method: it could be "exact" or "EMCC" indicates the learning algorithm.
- ace_l and ace_u indicates the lower and upper bounds for the average causal effects.
- pns_l and pns_u indicates the lower and upper bounds for the probability of necessity and sufficiency.
- selector: indicates if the model contains a selector variable or not.
- ps1: indicates the probability P(S=1) in the dataset.
- time_ace and time_pns time in ms. for running the queries.
- time_learn time in ms. for learning the distributions of the Us.
- n_convergence: number of EM runs required for reaching the at least the 90% of the interval (of that execution).


Notes
-------

set1 and set2 are the first bunch of experiments, though a problem in the sampling
was found: topologycal order was not always found