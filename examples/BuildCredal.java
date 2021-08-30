import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;


public class BuildCredal {

    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, BayesianFactorBuilder.as(bnet.getDomain(y), new double[]{0.3,0.7}));
        bnet.setFactor(x, BayesianFactorBuilder.as(bnet.getDomain(x,y), new double[]{0.6,0.4, 0.5,0.5}));

        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);


        /* Simple API */

        /// From a causal model to Vertex Credal Network
        DAGModel m1 = causalModel.toVCredal(bnet.getFactors());

        for(int v : m1.getVariables()){
            System.out.println(m1.getFactor(v));
        }

        /// From causal model to HalfSpace Credal Network
        DAGModel m2 =  causalModel.toHCredal(bnet.getFactors());


        /* Flexible but complex API*/

        /// From a causal model to Vertex Credal Network
        DAGModel m3 = ExactCredalBuilder.of(causalModel)
                .setEmpirical(bnet.getFactors())
                .setToVertex()
                .build().getModel();

        for(int v : m3.getVariables()){
            System.out.println(m3.getFactor(v));
        }


        /// From causal model to HalfSpace Credal Network
        DAGModel m4 = ExactCredalBuilder.of(causalModel)
                .setEmpirical(bnet.getFactors())
                .setToHalfSpace()
                .build().getModel();




        /** Putting all together (CausalBuilder and CredalBuilder) **/

        DAGModel m5 = StructuralCausalModel.of(bnet).toVCredal(bnet.getFactors());


    }
}
