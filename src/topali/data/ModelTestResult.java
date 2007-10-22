// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

import topali.data.models.Model;

public class ModelTestResult extends AlignmentResult 
{

	public static final String MRBAYES = "MrBayes";
	public static final String PHYML = "PhyML";
	
	public String phymlPath = "";
	public String selection = "";
	public ArrayList<Model> models = new ArrayList<Model>();
	
	public ModelTestResult() {
		super();
	}
	
	public ModelTestResult(int id) {
		super(id);
	}
}
