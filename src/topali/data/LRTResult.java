// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import sbrn.commons.MatrixXML;

/*
 * Class that stores both the results from running a LRT analysis and the
 * settings required to make the run (although not the data itself).
 */
public class LRTResult extends AlignmentResult
{
	// The method (JC, F84, etc)
	public int method;

	// Step size and window size
	public int window;

	public int step;

	// Number of LRT runs (including bootstraps)
	public int runs;

	// These properties relate to the alignment being analysed
	public double tRatio, alpha;

	// The LRT graph (x and y values)
	public float[][] data;

	// Bootstrap information (maximum y found for each run)
	public float[] thresholds;

	// And current threshold cutoff point
	public float thresholdCutoff = 0.95f;

	public LRTResult()
	{
	}

	// Castor conversion methods so that the 2D data array is saved as a string
	public String getData()
	{
		return MatrixXML.arrayToString(data);
	}

	public void setData(String str)
	{
		data = MatrixXML.stringTo2DFloatArray(str);
	}
}