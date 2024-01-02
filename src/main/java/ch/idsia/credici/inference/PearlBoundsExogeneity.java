package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;

public class PearlBoundsExogeneity extends CausalInference<BayesianNetwork, IntervalFactor> {

    public PearlBoundsExogeneity(StructuralCausalModel causalModel, TIntIntMap[] data){
        this.causalModel = causalModel;
        this.model = causalModel.getEmpiricalNet(data);
    }


    public PearlBoundsExogeneity(StructuralCausalModel causalModel){
        this.causalModel = causalModel;
        this.model = causalModel.getEmpiricalNet();
    }


    @Override
    public BayesianNetwork getInferenceModel(Query q, boolean simplify) {
        throw new NotImplementedException("");
    }


    @Override
    public IntervalFactor run(Query q) throws InterruptedException {
        throw new NotImplementedException("");
    }

    public IntervalFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        //if(!CausalOps.exogenityCheck(causalModel, cause, effect))
        //    throw new IllegalArgumentException("Exogeneity condition is not satisfied");


        int X = cause, Y=effect;
        int x = trueState, y=trueState;
        int x_ = falseState, y_=falseState;

        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);

        double pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
        double py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
        double pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);


        double lb = Math.max(0, pyGx - pyGx_);
        double ub = Math.min(pyGx, py_Gx_);
        return new IntervalFactor(Strides.empty(), Strides.empty(), new double[][]{{lb}}, new double[][]{{ub}});

    }

    public IntervalFactor probNecessity(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        if(!CausalOps.exogenityCheck(causalModel, cause, effect))
            throw new IllegalArgumentException("Exogeneity condition is not satisfied");


        int X = cause, Y=effect;
        int x = trueState, y=trueState;
        int x_ = falseState, y_=falseState;

        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);

        double pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
        double py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
        double pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);

        double lb = Math.max(0, pyGx - pyGx_)/pyGx;
        double ub = Math.min(pyGx, py_Gx_)/pyGx;
        return new IntervalFactor(Strides.empty(), Strides.empty(), new double[][]{{lb}}, new double[][]{{ub}});

    }
    public IntervalFactor probSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        if(!CausalOps.exogenityCheck(causalModel, cause, effect))
            throw new IllegalArgumentException("Exogeneity condition is not satisfied");


        int X = cause, Y=effect;
        int x = trueState, y=trueState;
        int x_ = falseState, y_=falseState;

        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);

        double pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
        double py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
        double pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);

        double lb = Math.max(0, pyGx - pyGx_)/py_Gx_;
        double ub = Math.min(pyGx, py_Gx_)/py_Gx_;
        return new IntervalFactor(Strides.empty(), Strides.empty(), new double[][]{{lb}}, new double[][]{{ub}});

    }
    public IntervalFactor probEnablement(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        if(!CausalOps.exogenityCheck(causalModel, cause, effect))
            throw new IllegalArgumentException("Exogeneity condition is not satisfied");


        int X = cause, Y=effect;
        int x = trueState, y=trueState;
        int x_ = falseState, y_=falseState;

        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        BayesianFactor joint = (BayesianFactor) ve.conditionalQuery(new int[]{X, Y});
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);

        double py_ = joint.marginalize(X).filter(Y, y_).getValueAt(0);
        double px_ = joint.marginalize(Y).filter(X, x_).getValueAt(0);

        double pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
        double py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
        double pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);

        double lb = Math.max(0, pyGx - pyGx_)*px_/py_;
        double ub = Math.min(pyGx, py_Gx_)*px_/py_;
        return new IntervalFactor(Strides.empty(), Strides.empty(), new double[][]{{lb}}, new double[][]{{ub}});


    }
    public IntervalFactor probDisablement(int cause, int effect, int trueState, int falseState) throws InterruptedException {
        if(!CausalOps.exogenityCheck(causalModel, cause, effect))
            throw new IllegalArgumentException("Exogeneity condition is not satisfied");


        int X = cause, Y=effect;
        int x = trueState, y=trueState;
        int x_ = falseState, y_=falseState;

        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(model.getFactors());
        BayesianFactor joint = (BayesianFactor) ve.conditionalQuery(new int[]{X, Y});
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);

        double py = joint.marginalize(X).filter(Y, y).getValueAt(0);
        double px = joint.marginalize(Y).filter(X, x).getValueAt(0);

        double pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
        double py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
        double pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);

        double lb = Math.max(0, pyGx - pyGx_)*px/py;
        double ub = Math.min(pyGx, py_Gx_)*px/py;
        return new IntervalFactor(Strides.empty(), Strides.empty(), new double[][]{{lb}}, new double[][]{{ub}});

    }
}
