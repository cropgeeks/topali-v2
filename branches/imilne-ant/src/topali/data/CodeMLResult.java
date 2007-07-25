// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.Vector;

/*
 * Class that stores both the results from running a PAML positive selection
 * analysis (via CODEML) and the settings required to make the run (although 
 * not the data itself).
 */
public class CodeMLResult extends AlignmentResult
{	
	
	public final static int TYPE_SITEMODEL = 0;
	public final static int TYPE_BRANCHMODEL = 1;
	
	public int type;
	public Vector<CMLModel> models = new Vector<CMLModel>();
	public Vector<CMLHypothesis> hypos = new Vector<CMLHypothesis>();
	
	// The location of the CodeML binary
	public String codemlPath;
	
	public CodeMLResult() {
		
	}
	
	public CodeMLResult(int type)
	{
		this.type = type;
	}

	/**
	 * Calling this method, will remove repeated models and just keep the one with the best likelihood.
	 * E.g. you ran M0 several times with different omega start values, this method will remove all M0s, just
	 * keeping the one with the best likelihood.
	 */
	public void filterModels() {
		Vector<CMLModel> models = new Vector<CMLModel>();
		CMLModel lastModel = null;
		
		for(int i=0; i<this.models.size(); i++) {
			CMLModel thisModel = this.models.get(i);
			
			if(lastModel==null) {
				lastModel = thisModel;
				continue;
			}
			
			if(thisModel.model.equals(lastModel.model)) {
				if(thisModel.likelihood>lastModel.likelihood)
					lastModel = thisModel;
			}
			else {
				models.add(lastModel);
				lastModel = thisModel;
			}
		}
		models.add(lastModel);
		this.models = models;
	}
}