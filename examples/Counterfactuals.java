import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.predefined.Party;
import ch.idsia.crema.model.graphical.SparseModel;

import java.util.Arrays;

public class Counterfactuals {
    public static void main(String[] args) {
        StructuralCausalModel causalModel = Party.buildModel();
        SparseModel vcredal = causalModel.toVCredal(causalModel.getEmpiricalProbs());
        int[] U = CausalInfo.of((SparseModel) vcredal.counterfactual_do(1,0)).getExogenousVars();
        System.out.println(Arrays.toString(U)); // [1, 4, 5, 6, 7] while should be [4, 5, 6, 7]

        //Operations.merge(vcredal, (SparseModel) vcredal.counterfactual_do(1,0));

    }
}
