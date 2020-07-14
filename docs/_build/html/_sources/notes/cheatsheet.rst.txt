RST cheat sheet
==============================

Subsection
------------------------------------------

Sub-subsection
^^^^^^^^^^^^^^^^^^^


More info about RST: `Link <https://thomas-cokelaer.info/tutorials/sphinx/rest_syntax.html>`_.

*NOTE*: RST requires to leave a blank line between environments.


To add images, this should be always be placed in `_static` folder:

.. image:: ../_static/img/logo.png
   	:scale: 50 %
   	:align: center


Add code directly in the rst file:

.. code-block:: java

   System.out.println("...")
   


To add a code from a source file file:


.. literalinclude:: ../../examples/CausalModel.java
   :language: java
   :lines: 4-10



Add inline math latex: :math:`p(\mathbf{x})`


Add latex equation:

.. math::  q(z,\theta) \approx p(z,\theta | x_{train})
