package ch.idsia.credici.utility.reconciliation;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.DomainUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntIntMap;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleIntegrator {


    TIntIntMap[] extendedData = null;
    StructuralCausalModel extendedModel = null;

    public SimpleIntegrator(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException {


        if(interventions.length>0) {

            int Dcard = datasets.length;
            if (dataObs != null) Dcard += 1;

            // Add the dataset variable
            extendedModel = model.copy();
            int DecVar = extendedModel.addVariable(Dcard, false);
            int ud = extendedModel.addVariable(1, true);


            int[] Dch = Arrays.stream(interventions).map(i -> i.keys()).flatMapToInt(Arrays::stream).distinct().toArray();

            if (Dch.length > 1)
                throw new IllegalArgumentException("Method only available for one intervened variable");

            int X = Dch[0];
            extendedModel.addParent(X, DecVar);

            Strides left = model.getDomain(X);
            Strides right = DomainUtil.remove(model.getFactor(X).getDomain(), X);
            List eqs = Arrays.stream(interventions)
                    .map(i -> i.get(X)).map(v -> EquationBuilder.constant(left, right, v))
                    .collect(Collectors.toList());


            int[] varOrder = ((BayesianFactor) eqs.get(0)).getDomain().getVariables();

            if (dataObs != null) eqs.add(0, model.getFactor(X).reorderDomain(varOrder));


            BayesianFactor fnew = EquationOps.merge(X, DecVar, (BayesianFactor[]) eqs.toArray(BayesianFactor[]::new));
            extendedModel.setFactor(X, fnew);

            // Set factor at D and UD
            extendedModel.setFactor(ud, new BayesianFactor(extendedModel.getDomain(ud), new double[]{1.0}));
            int finalDcard = Dcard;
            extendedModel.setFactor(DecVar, new BayesianFactor(extendedModel.getDomain(DecVar, ud), IntStream.range(0, Dcard).mapToDouble(i -> 1.0 / finalDcard).toArray()));


            TIntIntMap[][] datasetsExt = new TIntIntMap[Dcard][];
            int i = 0;
            if (dataObs != null) {
                datasetsExt[i] = DataUtil.addConstant(dataObs, DecVar, 0);
                i++;
            }
            for (TIntIntMap[] dataset_i : datasets) {
                datasetsExt[i] = DataUtil.addConstant(dataset_i, DecVar, i);
                i++;
            }

            extendedData = DataUtil.vconcat(datasetsExt);
        }else{
            extendedModel = model;
            extendedData = dataObs;
        }

    }

    public StructuralCausalModel getExtendedModel() {
        return extendedModel;
    }

    public TIntIntMap[] getExtendedData() {
        return extendedData;
    }

    public static double loglikelihood(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException {
        SimpleIntegrator si = new SimpleIntegrator(model, dataObs, interventions, datasets);
        return si.getExtendedModel().logLikelihood(si.getExtendedData());
    }

}
