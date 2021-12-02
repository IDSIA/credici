Installation
=================

Credici can be easily included at any maven project. For this, add the following code in the  pom.xml:

.. code-block:: xml

    <repositories>
        <repository>
            <id>cremaRepo</id>
            <url>https://raw.github.com/idsia/crema/mvn-repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>ch.idsia</groupId>
            <artifactId>credici</artifactId>
            <version>0.1.4</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>

