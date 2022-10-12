Explanation of the results files
===================================


Filename
------------

The filename of the csv file can be splitted in 2 parts: up to the substring "_uai" it's the SCM filename. After that, some parameters
of the execution are indicated:
```
[modelfilename]_uai_[method]_[[stoppingCriteria]_th[threshold]_mIter[maximumIterations]_w[isWeightedEM]_x[numberPoints]]?_[seed].csv
```

Identificative fields per experiment
---------------------------------------
The following identify an experiment and should be unique:

- modelID and filename: indicate the filename with the SCM
- method: CCVE, CCALP, EMCC
- stop_criteria: metric used for stopping individual EM runs. Possible values: KL, L1 or LLratio.
- threshold: epsilon considered in the stopping condition.
- cause, effect, trueState, falseState: variables and states for the PNS query.
- seed: random seed.

Other descriptive fields
----------------------------------------
- datasize: size of the dataset used.
- exact: indicates if the resulting interval is the true one or it's an approximation.
- iter_max: maximum number of iterations when using EMCC
- exactPath: path where the corresponding true PNS intervals are. It could be that the file is missing.

Fields with results
---------------------------

ll_max: maximum log-likelihood that can be obtained with this data and model.
ll_i: each field of this kind (with i>=0) indicates the log-likelihood of a resulting precise SCM.
ratio_i: ratio ll_max/ll_i where the optimal value is 1.0.
iter_i: indicates the number of iteration at the ith individual EM run. It cannot be greater than iter_max.
time_learn: time in ms. for estimating the P(U).
time_pns: time in ms. for calculating the imprecise PNS.
