Causal Flavour
=============================

Repository structure
------------------------
- bin/: folder with the required libraries (e.g. credici)
- code/: folder with the java code
- data/: folder with the datasets
- lerntmodels/: folder containing the precise models resulting from running EMCC (learning phase). These will be used in the inference phase.
- models/: specification of the partially-specified SCMs (in folder format and uai).


Functionality
----------------------------

Note: The corresponding .java files should be espcifically be modified for changing the parameters of each task:

- **Building**: the partially-specified SCM  are initially given in a custom format which includes information about the variable and states labels. To be used in credici, these should be converted into .uai format, for this run:
```
java -cp ./bin/credici.jar ./code/BuildModel.java
```

- **Plot** the information about the structural equations and the DAG:

```
java -cp ./bin/credici.jar ./code/PlotModel.java
```

- **Learning** phase using EMCC: 

```
java -cp ./bin/credici.jar ./code/LearnModel.java
```

- **Inference** phase using the resulting models in the previous phase:

```
java -cp ./bin/credici.jar ./code/InferenceModel.java
```
