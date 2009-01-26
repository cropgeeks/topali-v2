// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

import topali.data.models.*;

public class ModelTestResult extends AlignmentResult
{
	public static final int SINGLE_MODEL_RUN = 0;
	public static final int CP_MODEL_RUN_CP1 = 1;
	public static final int CP_MODEL_RUN_CP2 = 2;
	public static final int CP_MODEL_RUN_CP3 = 3;

	public static final String TYPE_MRBAYES = "MrBayes";
	public static final String TYPE_PHYML = "PhyML";

	public static final String SAMPLE_SEQLENGTH = "Sequence Length";
	public static final String SAMPLE_ALGNSIZE = "Alignment Size";

	public String phymlPath = "";
	public String treeDistPath = "";
	//this is 'n' used for calculating aic2 and bic:
	public int sampleSize = -1;

	// The AIC2/BIC option setting
	public String sampleCrit = "";
	// Generate model for... "MrBayes or PhyML"
	public String type = "";

	// The index number for this run (0 for a normal run, or 1, 2 or 3 for jobs
	// where we've split the alignment into 3 interlaced coding regions)
	public int splitType = SINGLE_MODEL_RUN;

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
