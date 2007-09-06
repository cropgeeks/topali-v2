// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.Serializable;

import pal.statistics.ChiSquareDistribution;
import sbrn.commons.MatrixXML;
import topali.analyses.AnalysisUtils;

/*
 * Class that stores both the results from running a PDM analysis and the
 * settings required to make the run (although not the data itself).
 */
public class PDMResult extends AlignmentResult implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5195818444473552358L;

	// The location of the Bambe binary
	public String bambePath;

	// The location of the TreeDist binary
	public String treeDistPath;

	public int pdm_window;

	public int pdm_step;

	// Number of PDM runs (including bootstraps)
	public int pdm_runs;

	public boolean pdm_prune;

	public float pdm_cutoff;

	public int pdm_seed;

	public int pdm_burn;

	public int pdm_cycles;

	public String pdm_burn_algorithm;

	public String pdm_main_algorithm;

	public String pdm_use_beta;

	public int pdm_parameter_update_interval;

	public String pdm_update_theta;

	public int pdm_tune_interval;

	public String pdm_molecular_clock;

	public String pdm_category_list;

	public String pdm_initial_theta;

	public int pdm_outgroup;

	public float pdm_global_tune;

	public float pdm_local_tune;

	public float pdm_theta_tune;

	public float pdm_beta_tune;

	// Frequency values for initial_pia, c, g, t
	public double[] frequencies;

	// And initial kappa
	public double kappa;

	// These properties relate to the alignment being analysed
	public double tRatio, alpha;

	// Data for the global and local statistic graphs
	public float[][] glbData;

	public float[][] locData;

	// Bootstrap information (maximum y found for each run)
	public float[] thresholds;

	// Data for the (tree) histograms
	public float[][] histograms;

	// Variables required to work out threshold
	public int df, N;

	public PDMResult()
	{
	}

	public float calculateThreshold()
	{
		// Old or new method of threshold calculation?
		if (thresholds != null)
		{
			return AnalysisUtils.getArrayValue(thresholds, (float)threshold);
		} else
		{
			double chi2 = ChiSquareDistribution.quantile(threshold, df);
			float threshold = (float) chi2 / (2 * N);

			return threshold;
		}
	}

	// Castor conversion methods so that the 2D data arrays are saved as strings
	public String getGlbData()
	{
		return MatrixXML.arrayToString(glbData);
	}

	public void setGlbData(String str)
	{
		glbData = MatrixXML.stringTo2DFloatArray(str);
	}

	public String getLocData()
	{
		return MatrixXML.arrayToString(locData);
	}

	public void setLocData(String str)
	{
		locData = MatrixXML.stringTo2DFloatArray(str);
	}

	public String getHistograms()
	{
		return MatrixXML.arrayToString(histograms);
	}

	public void setHistograms(String str)
	{
		histograms = MatrixXML.stringTo2DFloatArray(str);
	}
	
}