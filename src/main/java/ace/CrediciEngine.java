package ace;

import edu.ucla.belief.ace.OnlineEngineSop;

public class CrediciEngine extends OnlineEngineSop {
    private String lmfilename; 
    private String acfilename;

	public CrediciEngine(String lmFilename, String acFilename, boolean enableDifferentiation) throws Exception {
		super(lmFilename, acFilename, enableDifferentiation); 

        this.lmfilename = lmFilename;
        this.acfilename = acFilename;

        //super.fSpace = Space.LOG_E;
	}

    public void updateFactor(int index, double value) {
        fAcVarToDefaultPosWeight[index] = value;
    }

    public String getAcfilename() {
        return acfilename;
    }

    public String getLmfilename() {
        return lmfilename;
    }
}
