package ace;

import edu.ucla.belief.ace.OnlineEngineSop;

public class CrediciEngine extends OnlineEngineSop {
    
	public CrediciEngine(String lmFilename, String acFilename, boolean enableDifferentiation) throws Exception {
		super(lmFilename, acFilename, enableDifferentiation);    
        //super.fSpace = Space.LOG_E;
	}

    public void updateFactor(int index, double value) {
        fAcVarToDefaultPosWeight[index] = value;
    }

}
