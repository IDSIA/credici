Causal Inference
==============================


Credal Network Transformation
----------------------------------

Any object of class ``StructuralCausalModel`` can be converted into an equivalent
credal network using the methods ``toVCredal`` and ``toHCredal`` for a vertex and a
constraints specification. The input is a collection of `BayesianFactors`` which
are the empirical distributions.

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 31-33



Inference engine
----------------------------------------
First the exact and approximate inferences engines should be set up. For this
create instances of classes ``CredalCausalVE`` and ``CredalCausalAproxLP`` as shown
in the following code snippet.

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 38-41

Alternatively, engines can be instantiated from a credal network.

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 44-47


Causal Effects
----------------------------------------

Let us consider the causal effect of on a variable :math:`X_3` of a variable
:math:`X_1 = 1`, that is, :math:`P(X_3|do(X_1=1))`. This can be calculated with
the exact inference engine as follows.

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 53-58

Alternatively, for an approximate solution:


.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 63-68



Conuterfactuals
----------------------------------------

Credici also allows counterfactual queries (in a twin graph) such as :math:`P(X_3'|do(X'_1=1), X'_1=0)`.
The exact computation of this query can be done as follows.

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 77-83

On the other hand, using the approximate engine:

.. literalinclude:: ../../examples/docs/Inference.java
   :language: java
   :lines: 89-95





