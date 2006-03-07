// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.*;

import topali.fileio.*;

/*
 * Class that stores both the results from running a PDM analysis and the
 * settings required to make the run (although not the data itself).
 */
public class PDMResult extends AlignmentResult
{
	// The location of the Bambe binary
	public String bambePath;
	// The location of the TreeDist binary
	public String treeDistPath;
		
	public int pdm_window;
	public int pdm_step;
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
	
	
	// Data for the global and local statistic graphs
	public float[][] glbData;
	public float[][] locData;
	
	// Data for the (tree) histograms
	public float[][] histograms;
	
	// And current threshold cutoff point
	public float thresholdCutoff = 0.95f;
	// Variables required to work out threshold
	public int df, N;
	
	public PDMResult()
	{
	}
}