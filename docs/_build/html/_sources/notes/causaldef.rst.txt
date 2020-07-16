Causal Model Definition
==============================

Here we will consider the different ways for defining a structural causal model (SCM) in Credici.
This can be done by explicitly specifiying all the nodes, arcs and factors in the model, or
with the help of the class ``CausalBuilder``.


Explicit Definition
--------------------------

The code snippet shown below shows how to explicitly define a SCM. For this, an object of
class ``StructuralCausalModel`` is created. Then endogenous and exogenous variable are added
to the model by indicating the cardinality. In case of the exogenous ones, the second input parameter
should be set to ``true``, which indicates the type of variable. Then the parents are set and
finaly the factors are specified, which are basically objects of class ``BayesianFactor``.


.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 22-64




Causal Builder
------------------------

Additionaly, Credici provides class ``CausalBuilder`` at ``ch.idsia.credici.model.builder`` for
simplifying the code under some settings. We will assume that we have ``BayesianNetwork`` specifying
the empirical information: empirical DAG and eventually empirical probabilities. For example:


.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 70-72

The following code shows 4 equivalent ways of building a SCM from such BN under
the markovian setting.

.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 75-85

In the previous cases, factors associated to exogenous variables are empty. Instead, we could
build it with some random factors:

.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 89-100


Instead of considering the default structural equation, these could be specifyed as follows.

.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 130-147



The quasi-markovian case could be also considered by specifying the causal DAG, which should be
consistent with the empirical one.

.. literalinclude:: ../../examples/docs/CausalModelDefinition.java
   :language: java
   :lines: 115-126





