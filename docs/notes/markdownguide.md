# Markdown entry

CreMA is a open-source java toolbox that provides multiple
learning and inference algorithms for credal models.

An example of exact inference in a credal network is given below.

```

double p = 0.2;
double eps = 0.0001;

/*  CN defined with vertex Factor  */

// Define the model (with vertex factors)
SparseModel model = new SparseModel();
int u = model.addVariable(3);
int x = model.addVariable(2);
model.addParent(x,u);

// Define a credal set of the partent node
VertexFactor fu = new VertexFactor(model.getDomain(u), Strides.empty());
fu.addVertex(new double[]{0., 1-p, p});
fu.addVertex(new double[]{1-p, 0., p});
model.setFactor(u,fu);


System.out.println(p+" "+(1-p));

// Define the credal set of the child
VertexFactor fx = new VertexFactor(model.getDomain(x), model.getDomain(u));

fx.addVertex(new double[]{1., 0.,}, 0);
fx.addVertex(new double[]{1., 0.,}, 1);
fx.addVertex(new double[]{0., 1.,}, 2);

model.setFactor(x,fx);

// Run exact inference inference
VariableElimination ve = new FactorVariableElimination(model.getVariables());
ve.setFactors(model.getFactors());
System.out.println(ve.run(x));


``` 

## Installation

Add the following code in the  pom.xml of your project:

```
    <repositories>
        <repository>
            <id>cremaRepo</id>
            <url>https://raw.github.com/idsia/crema/mvn-repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>ch.idsia</groupId>
            <artifactId>crema</artifactId>
            <version>0.1.2</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```