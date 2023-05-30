package examples;

import ch.idsia.credici.inference.CausalEMVE;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.DomainUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class necesityExt {
    public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException, InterruptedException, CsvException {


        // Relevant paths (update)
        String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
        Path dataPath = Path.of(wdir, "/papers/CPPcausal/data/");

        String filename = dataPath.resolve("./outcomes.csv").toString();

        String classvar = "outcome";


        List dataOriginal = DataUtil.fromCSVtoStrMap(filename, '\t');
        dataOriginal = DataUtil.removeWithKey(dataOriginal, "");
        //dataOriginal = DataUtil.removeWithKey(dataOriginal, "Feature3");
        //dataOriginal = DataUtil.removeWithKey(dataOriginal, "Feature2");
        //dataOriginal = DataUtil.removeWithKey(dataOriginal, "Feature1");

        // Check binary variables

        // class var present

        // more than 1 variable


        String[] vars = (String[]) ((HashMap)dataOriginal.get(0)).keySet().toArray(String[]::new);

        String[] features = Arrays.stream(vars).filter(s -> !s.equals(classvar)).toArray(String[]::new);


        TIntIntMap[] data = (TIntIntMap[]) DataUtil.dataToNumeric(dataOriginal,vars).toArray(TIntIntMap[]::new);
        System.out.println(data);

        int y = List.of(vars).indexOf(classvar);
        int[] X = Arrays.stream(((TIntIntMap) data[0]).keys()).filter(x -> x!=y).toArray();

        /// Build model

        SparseDirectedAcyclicGraph dag = new SparseDirectedAcyclicGraph();
        dag.addVariable(y);
        for(int i=0; i<X.length; i++){
            int x = X[i];
            dag.addVariable(x);
            dag.addLink(x,y);
            //if(i>0) dag.addLink(X[i-1], X[i]);
        }




        // build markovian model
        StructuralCausalModel model = CausalBuilder.of(dag,2).build();
        //model = Cofounding.mergeExoParents(model, 0,1);

        System.out.println(model);

        //Learning
        //CausalInference inf = new CredalCausalVE(model, data);
        //CausalInference inf = new CredalCausalApproxLP(model, data);

        // Todo: deactivate convexhull
        CausalInference inf = new CausalEMVE(model, data, 50, 1000);

        System.out.println("Built inference");


        TIntIntMap[] xvalues = DomainUtil.getObservationSpace(model.getDomain(X));

        IntervalFactor ires = null;
        double lb, ub;

        int positive = 0;
        int negative = 1;

        for(int x:X){
            ires = (IntervalFactor) new VertexToInterval().apply((VertexFactor) inf.probNecessityAndSufficiency(x,y, positive, negative),y);
            lb = ires.getDataLower()[0][0];
            ub = ires.getDataUpper()[0][0];
            System.out.println("PNS_"+vars[x]+":["+lb+","+ub+"]");

        }
        for(int x:X){
            ires = (IntervalFactor) new VertexToInterval().apply((VertexFactor) inf.probNecessity(x,y, positive, negative),y);
            lb = ires.getDataLower()[0][0];
            ub = ires.getDataUpper()[0][0];
            System.out.println("PN_"+vars[x]+":["+lb+","+ub+"]");
        }

        for(int x:X){
            ires = (IntervalFactor) new VertexToInterval().apply((VertexFactor) inf.probSufficiency(x,y, positive, negative),y);
            lb = ires.getDataLower()[0][0];
            ub = ires.getDataUpper()[0][0];
            System.out.println("PS_"+vars[x]+":["+lb+","+ub+"]");
        }

        for(int x:X){
            ires = (IntervalFactor) new VertexToInterval().apply((VertexFactor) inf.probEnablement(x,y, positive, negative),y);
            lb = ires.getDataLower()[0][0];
            ub = ires.getDataUpper()[0][0];
            System.out.println("PE_"+vars[x]+":["+lb+","+ub+"]");
        }



        /*


        List results = new ArrayList();

        for(int i=0; i<xvalues.length; i++){
            for(int j=0; j<xvalues.length; j++){
                if(i!=j){
                    for(int yhat=0; yhat<=1; yhat++){


                        TIntIntMap xvals = xvalues[i];
                        TIntIntMap xvals_ = xvalues[j];

                        int yhat_ = Math.abs(Math.abs(yhat)-1);

                        TIntIntMap obs = new TIntIntHashMap();
                        for(int x: X) obs.put(x, xvals.get(x));
                        obs.put(y, yhat);


                        GenericFactor res = inf.counterfactualQuery().setIntervention(xvals_).setEvidence(obs).setTarget(y).run();


                        IntervalFactor ires = null;
                        if(res instanceof VertexFactor)
                            ires = (IntervalFactor) new VertexToInterval().apply((VertexFactor) res,y);
                        else
                            ires = (IntervalFactor) res;
                        double lb = ires.getDataLower()[0][yhat_];
                        double ub = ires.getDataUpper()[0][yhat_];



                        HashMap r = new HashMap();

                        r.put("lowerP", lb);
                        r.put("upperP", ub);

                        for(int x:X){
                            r.put(vars[x], xvals.get(x));
                            r.put(vars[x]+"'", xvals_.get(x));
                        }
                        r.put(vars[y], yhat);
                        r.put(vars[y]+"'", yhat_);


                        System.out.println(r);
                        results.add(r);
                    }

                }
            }
        }

        String targetfile = filename.replace(".csv","_res.csv");
        System.out.println(targetfile);
        DataUtil.toCSV(targetfile, results);


*/
    }
    }
        /*

{lowerP=0.0, outcome'=1, Feature3=0, upperP=0.9985543620118462, Feature3'=0, Feature1=0, Feature1'=1, outcome=0}
{lowerP=0.009204103994421753, outcome'=0, Feature3=0, upperP=1.0, Feature3'=0, Feature1=0, Feature1'=1, outcome=1}
{lowerP=0.0, outcome'=1, Feature3=0, upperP=1.0, Feature3'=1, Feature1=0, Feature1'=0, outcome=0}
{lowerP=0.003884849088554637, outcome'=0, Feature3=0, upperP=0.9961151509114453, Feature3'=1, Feature1=0, Feature1'=0, outcome=1}
{lowerP=0.0, outcome'=1, Feature3=0, upperP=0.0, Feature3'=1, Feature1=0, Feature1'=1, outcome=0}
{lowerP=1.0, outcome'=0, Feature3=0, upperP=1.0, Feature3'=1, Feature1=0, Feature1'=1, outcome=1}
{lowerP=0.00919092048461217, outcome'=1, Feature3=0, upperP=0.9985676487556447, Feature3'=0, Feature1=1, Feature1'=0, outcome=0}
{lowerP=0.0, outcome'=0, Feature3=0, upperP=1.0, Feature3'=0, Feature1=1, Feature1'=0, outcome=1}
{lowerP=0.005311635864483657, outcome'=1, Feature3=0, upperP=0.9946883641355164, Feature3'=1, Feature1=1, Feature1'=0, outcome=0}
{lowerP=0.0, outcome'=0, Feature3=0, upperP=1.0, Feature3'=1, Feature1=1, Feature1'=0, outcome=1}
{lowerP=0.0, outcome'=1, Feature3=0, upperP=0.0, Feature3'=1, Feature1=1, Feature1'=1, outcome=0}
{lowerP=1.0, outcome'=0, Feature3=0, upperP=1.0, Feature3'=1, Feature1=1, Feature1'=1, outcome=1}
{lowerP=0.0039000000000000003, outcome'=1, Feature3=1, upperP=1.0, Feature3'=0, Feature1=0, Feature1'=0, outcome=0}
{lowerP=0.0, outcome'=0, Feature3=1, upperP=0.9961, Feature3'=0, Feature1=0, Feature1'=0, outcome=1}
{lowerP=0.0, outcome'=1, Feature3=1, upperP=0.99466, Feature3'=0, Feature1=0, Feature1'=1, outcome=0}
{lowerP=0.005339999999999999, outcome'=0, Feature3=1, upperP=1.0, Feature3'=0, Feature1=0, Feature1'=1, outcome=1}
{lowerP=0.0, outcome'=1, Feature3=1, upperP=0.0, Feature3'=1, Feature1=0, Feature1'=1, outcome=0}
{lowerP=1.0, outcome'=0, Feature3=1, upperP=1.0, Feature3'=1, Feature1=0, Feature1'=1, outcome=1}
{lowerP=0.50195, outcome'=1, Feature3=1, upperP=0.50195, Feature3'=0, Feature1=1, Feature1'=0, outcome=0}

 */