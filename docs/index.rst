.. Crema documentation master file, created by
   sphinx-quickstart.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.




Credici: A Java Toolbox for Causal Inference Using Credal Methods
=========================================================================


.. image:: _static/img/logo.png
   	:scale: 50 %
   	:align: center




Credici is an open-source Java library for causal analysis. Inference is done
using well-founded methods for inference on credal networks, which is done in
a transparently to the user.

The main features of Credici are:

* Allows to easily define Structural Causal Networks (SCM).

* Causal inference: causal effects and counterfactuals.

* Inference is based in methods for inference in credal networks (exact and approximate).

* SCMs can me transformed in equivalent credal networks can be exported in UAI format.



Installation
-----------------

Credici can be installed from maven. For further details, check the `Installation <notes/installation.html>`_ section.


Citation
-----------------

For the theoretical results in which this tool is based, refer to the following publication.

.. code:: TeX

    @InProceedings{zaffalon2020structural,
      author    = {Zaffalon, Marco and Antonucci, Alessandro and Caba\~{n}as, Rafael},
      title     = {Structural Causal Models Are Credal Networks},
      booktitle = {Proceedings of the tenth International Conference on Probabilistic Graphical Models},
      year      = {2020},
      series    = {Proceedings of Machine Learning Research},
      address   = {Aalborg, Denmark},
      month     = {23--25 Sep},
      publisher = {PMLR},
    }




.. .. toctree::
.. :hidden:

.. Home <self>


.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Quick Start
   
   notes/getting30s
   notes/requirements
   notes/installation


.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Guides
   
   notes/causaldef
   notes/guideinference
   notes/javadoc


.. JavaDoc <http://javadoc_url>






.. toctree::
   :includehidden:
   :maxdepth: 1
   :caption: Other

   notes/contact



.. Indices and tables
  ==================

  * :ref:`genindex`
  * :ref:`modindex`
  * :ref:`search`
  



.. role:: bash(code)
   :language: bash
.. role:: python(code)
   :language: python3




