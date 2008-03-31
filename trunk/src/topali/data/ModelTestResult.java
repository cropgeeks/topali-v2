// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

import topali.data.models.*;

public class ModelTestResult extends AlignmentResult 
{
	public static final String TYPE_MRBAYES = "MrBayes";
	public static final String TYPE_PHYML = "PhyML";

	public static final String SAMPLE_SEQLENGTH = "Sequence Length";
	public static final String SAMPLE_ALGNSIZE = "Alignment Size";
	
	public String phymlPath = "";
	public String treeDistPath = "";
	//this is 'n' used for calculating aic2 and bic:
	public int sampleSize = -1;
	
	
	public String sampleCrit = "";
	public String type = "";
	
	public ArrayList<Model> models = new ArrayList<Model>();
	
	public Vector<Distance<String>> rfDistances = new Vector<Distance<String>>();
	
	public ModelTestResult() {
		super();
		isResubmittable = true;
	}
	
	public ModelTestResult(int id) {
		super(id);
		isResubmittable = true;
	}
	
	public void sortModels(int mode) {
		Collections.sort(models, new ModelComparator(mode));
	}
}
