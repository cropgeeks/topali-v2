// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import sbrn.commons.MatrixXML;

/*
 * Class that stores both the results from running a PDM2 analysis and the
 * settings required to make the run (although not the data itself).
 */
public class PDM2Result extends AlignmentResult 
{

	// The location of the MrBayes binary
	public String mbPath;

	// The location of the TreeDist binary
	public String treeDistPath;

	// The number of job-slots available to hack this job up into
	public int nProcessors;

	public int pdm_window;

	public int pdm_step;

	// Data for the global and local statistic graphs
	// public float[][] glbData;
	public float[][] locData;

	// Bootstrap information (maximum y found for each run)
	public float[] thresholds;

	// Data for the (tree) histograms
	// public float[][] histograms;

	// And current threshold cutoff point
	// public float thresholdCutoff = 0.95f;
	// Variables required to work out threshold
	// public int df, N;

	public PDM2Result()
	{
		super();
	}
	
	public PDM2Result(int id) {
		super(id);
	}

	// Castor conversion methods so that the 2D data array is saved as a string
	public String getData()
	{
		return MatrixXML.arrayToString(locData);
	}

	public void setData(String str)
	{
		locData = MatrixXML.stringTo2DFloatArray(str);
	}
	
}