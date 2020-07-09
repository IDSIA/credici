import ch.idsia.credici.model.builder.CredalBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

public class BuildCredal {

    public static void main(String[] args) {
        BayesianNetwork bnet = new BayesianNetwork();
        int y = bnet.addVariable(2);
        int x = bnet.addVariable(2);

        bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
        bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[]{0.6,0.4, 0.5,0.5}));

        StructuralCausalModel causalModel = StructuralCausalModel.of(bnet);


        /* Simple API */

        /// From a causal model to Vertex Credal Network
        SparseModel m1 = causalModel.toVCredal(bnet.getFactors());

        for(int v : m1.getVariables()){
            System.out.println(m1.getFactor(v));
        }

        /// From causal model to HalfSpace Credal Network
        SparseModel m2 =  causalModel.toHCredal(bnet.getFactors());

        for(int v : m2.getVariables()){
            System.out.println("variable "+v);
            ((SeparateHalfspaceFactor)m2.getFactor(v)).printLinearProblem();
        }


        /* Flexible but complex API*/

        /// From a causal model to Vertex Credal Network
        SparseModel m3 = CredalBuilder.of(causalModel)
                .setEmpirical(bnet.getFactors())
                .setToVertex()
                .build();

        for(int v : m3.getVariables()){
            System.out.println(m3.getFactor(v));
        }


        /// From causal model to HalfSpace Credal Network
        SparseModel m4 = CredalBuilder.of(causalModel)
                .setEmpirical(bnet.getFactors())
                .setToHalfSpace()
                .build();


        for(int v : m4.getVariables()){
            System.out.println("variable "+v);
            ((SeparateHalfspaceFactor)m4.getFactor(v)).printLinearProblem();
        }


        /** Putting all together (CausalBuilder and CredalBuilder) **/

        SparseModel m5 = StructuralCausalModel.of(bnet).toVCredal(bnet.getFactors());


    }
}
