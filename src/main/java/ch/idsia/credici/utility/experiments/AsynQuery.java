package ch.idsia.credici.utility.experiments;

import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.crema.factor.GenericFactor;
import jdk.jshell.spi.ExecutionControl;

public class AsynQuery {

    static CredalCausalVE inf = null;
    static String type = "";
    static int cause;
    static int effect;

    public static void setArgs(CredalCausalVE inf, String type, int cause, int effect){
        AsynQuery.inf = inf;
        AsynQuery.type = type;
        AsynQuery.cause = cause;
        AsynQuery.effect = effect;
    }

    public  static GenericFactor run() throws InterruptedException, ExecutionControl.NotImplementedException {
        if(type.equals("ace")){
            return  inf.averageCausalEffects(cause, effect);
        }else if(type.equals("pns")){
            return inf.probNecessityAndSufficiency(cause, effect);
        }else{
            throw new IllegalArgumentException("Unknown query type");
        }

    }
}
