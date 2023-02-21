package examples;

import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.HashMap;

public class TriangoloStats {
    public static void main(String[] args) throws IOException, CsvException {

//              0           1           2               3          4        5               6           7           8           9           10              11
        //varnames = ["Death", "Symptoms", "PPreference", "FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"]
        String file = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/data/triangolo_data_d1000.csv";

        TIntIntMap[] data = DataUtil.fromCSV(file);

        int S = 1;
        int K = 10;
        int A = 4;

        HashMap varnames = new HashMap();
        varnames.put(S, "Symptoms");
        varnames.put(K, "Karnofsky");
        varnames.put(A, "Age");

        BayesianFactor pS = DataUtil.getJointProb(data, Strides.as(S,2));
        FactorUtil.print(pS, varnames);

        BayesianFactor pK = DataUtil.getJointProb(data, Strides.as(K,2));
        FactorUtil.print(pK, varnames);


        //DataUtil.unique(DataUtil.selectColumns(data, A))


        BayesianFactor pSK = DataUtil.getJointProb(data, Strides.as(S,2,K,2));
        FactorUtil.print(pSK, varnames);

        BayesianFactor pK_A = DataUtil.getCondProb(data, Strides.as(K,2), Strides.as(A,2));
        FactorUtil.print(pK_A, varnames);
    }
}
