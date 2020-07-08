package ch.idsia.credici.utility;

import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

import java.util.stream.DoubleStream;

public class ConstraintsOps {
    public static SeparateHalfspaceFactor  removeZeroConstraints(SeparateHalfspaceFactor f){

        SeparateHalfspaceFactor newFactor = new SeparateHalfspaceFactor(f.getDataDomain(), f.getSeparatingDomain());

        for(int i=0; i<f.getSeparatingDomain().getCombinations(); i++){
            for(LinearConstraint c : f.getLinearProblem(i).getConstraints()) {
                if(!(c.getRelationship() == Relationship.EQ && c.getValue()==0
                        && DoubleStream.of(c.getCoefficients().toArray()).allMatch(x -> x == 0))) {

                    newFactor.addConstraint(c, i);
                }
            }
        }
        return newFactor;
    }
}
