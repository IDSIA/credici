package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.List;

public class CredalCausalMultiVE{

    CredalCausalVE inf = null;


    public CredalCausalMultiVE(StructuralCausalModel model, TIntIntMap[] data){
        this.inf = new CredalCausalVE(model, data);
    }

    public CredalCausalMultiVE(StructuralCausalModel model){
        this.inf = new CredalCausalVE(model);
    }

    public double[] probNecessity(int cause, int effect, int causeTrue, int causeFalse, int effectTrue, int effectFalse) throws InterruptedException {
        SparseModel vmodel = inf.getModel();

        int n = StructuralCausalModel.getNumPreciseModels(vmodel);
        double minP = Double.POSITIVE_INFINITY, maxP= Double.NEGATIVE_INFINITY;

        for(int i =0; i<n; i++) {
            StructuralCausalModel  m = StructuralCausalModel.getFromCausalVModelAt(vmodel, i);
            CausalVE cve = new CausalVE(m);
            double p = cve.probNecessity(cause, effect, causeTrue, causeFalse, effectTrue, effectFalse).getData()[0];
            if(p>maxP) maxP=p;
            if(p<minP) minP=p;
        }

        return new double[]{minP, maxP};

    }

    public double[] probSufficiency(int cause, int effect, int causeTrue, int causeFalse, int effectTrue, int effectFalse) throws InterruptedException {
        SparseModel vmodel = inf.getModel();

        int n = StructuralCausalModel.getNumPreciseModels(vmodel);
        double minP = Double.POSITIVE_INFINITY, maxP= Double.NEGATIVE_INFINITY;

        for(int i =0; i<n; i++) {
            StructuralCausalModel  m = StructuralCausalModel.getFromCausalVModelAt(vmodel, i);
            CausalVE cve = new CausalVE(m);
            double p = cve.probSufficiency(cause, effect, causeTrue, causeFalse, effectTrue, effectFalse).getData()[0];
            if(p>maxP) maxP=p;
            if(p<minP) minP=p;
        }

        return new double[]{minP, maxP};
    }


    public double[] probNecessityAndSufficiency(int cause, int effect, int causeTrue, int causeFalse, int effectTrue, int effectFalse) throws InterruptedException, ExecutionControl.NotImplementedException {
        SparseModel vmodel = inf.getModel();

        int n = StructuralCausalModel.getNumPreciseModels(vmodel);
        double minP = Double.POSITIVE_INFINITY, maxP= Double.NEGATIVE_INFINITY;

        for(int i =0; i<n; i++) {
            StructuralCausalModel  m = StructuralCausalModel.getFromCausalVModelAt(vmodel, i);
            CausalVE cve = new CausalVE(m);
            double p = cve.probNecessityAndSufficiency(cause, effect, causeTrue, causeFalse, effectTrue, effectFalse).getData()[0];
            if(p>maxP) maxP=p;
            if(p<minP) minP=p;
        }

        return new double[]{minP, maxP};
    }


}
