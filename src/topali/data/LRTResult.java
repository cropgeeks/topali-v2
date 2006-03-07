// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.*;

import org.exolab.castor.xml.*;

import topali.fileio.*;

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
}