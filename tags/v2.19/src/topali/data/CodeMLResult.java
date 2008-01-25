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
	public final static String TYPE_SITEMODEL = "Site Model";
	public final static String TYPE_BRANCHMODEL = "Branch Model";
	
	public String type;
	public Vector<CMLModel> models = new Vector<CMLModel>();
	public Vector<CMLHypothesis> hypos = new Vector<CMLHypothesis>();
	
	// The location of the CodeML binary
	public String codemlPath;
	
	public CodeMLResult() {
		super();
		isResubmittable = true;
	}
	
	public CodeMLResult(int id) {
		super(id);
		isResubmittable = true;
	}
	
	public CodeMLResult(String type)
	{
		super();
		this.type = type;
		isResubmittable = true;
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

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	@Override
	public String toString()
	{
	    final String TAB = "    ";
	    
	    String retValue = "";
	    
	    retValue = "CodeMLResult ( "
	        + super.toString() + TAB
	        + "type = " + this.type + TAB
	        + "models = " + this.models + TAB
	        + "hypos = " + this.hypos + TAB
	        + "codemlPath = " + this.codemlPath + TAB
	        + " )";
	
	    return retValue;
	}
	
	
}