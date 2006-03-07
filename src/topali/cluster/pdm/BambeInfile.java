// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

import java.io.*;

import topali.data.*;
import topali.cluster.*;

class BambeInfile
{
	private SequenceSet ss;
	private PDMResult result;
	
	private BufferedWriter out = null;
	private String sep = System.getProperty("line.separator");
	
	void saveInfile(File wrkDir, SequenceSet ss, PDMResult result)
		throws Exception
	{
		this.ss = ss;
		this.result = result;
		
		System.out.println(wrkDir);
		
		// Write the infile data
		out = new BufferedWriter(new FileWriter(new File(wrkDir, "infile")));
		writeFile();
		out.close();
		
		// Create the unix script required to run Bambe (for unix/linux/macOS)
		if (!ClusterUtils.isWindows)
		{
			out = new BufferedWriter(new FileWriter(new File(wrkDir, "runbambe")));
			out.write("/bin/sh -c \"" + result.bambePath + "\" < infile");
			out.close();
		}
		// Create the DOS batch file required to run Bambe
		else
		{
			out = new BufferedWriter(new FileWriter(new File(wrkDir, "runbambe.bat")));
			out.write("\"" + result.bambePath + "\" < infile");
			out.close();
		}
		
		// This will only work on Unix/Linux, but we don't care if it fails on
		// the other systems as it's not needed by them
		try  { Runtime.getRuntime().exec("chmod 700 runbambe", null, wrkDir); }
		catch (Exception e) {}
		
		System.out.println("BAMBE");
	}

	private void writeFile()
		throws Exception
	{
		out.write("seed = " + result.pdm_seed);
		out.write(sep + "burn = " + result.pdm_burn);
		out.write(sep + "cycles = " + result.pdm_cycles);
		out.write(sep + "burn-algorithm = " + result.pdm_burn_algorithm);
		out.write(sep + "main-algorithm = " + result.pdm_main_algorithm);
		out.write(sep + "use-beta = " + result.pdm_use_beta);
		out.write(sep + "molecular-clock = " + result.pdm_molecular_clock);
		out.write(sep + "category-list = " + result.pdm_category_list);
//		out.write(sep + "initial-ttp = " + result.pdm_initial_ttp);
//		out.write(sep + "initial-gamma = " + result.pdm_initial_gamma);
		out.write(sep + "parameter-update-interval = " + result.pdm_parameter_update_interval);
//		out.write(sep + "update-kappa = " + result.pdm_update_kappa);
		out.write(sep + "update-theta = " + result.pdm_update_theta);
//		out.write(sep + "update-pi = " + result.pdm_update_pi);
//		out.write(sep + "update-ttp = " + result.pdm_update_ttp);
//		out.write(sep + "update-gamma = " + result.pdm_update_gamma);
		out.write(sep + "tune-interval = " + result.pdm_tune_interval);
		out.write(sep + "global-tune = " + result.pdm_global_tune);
		out.write(sep + "local-tune = " + result.pdm_local_tune);
		out.write(sep + "theta-tune = " + result.pdm_theta_tune);
//		out.write(sep + "pi-tune = " + result.pdm_pi_tune);
//		out.write(sep + "kappa-tune = " + result.pdm_kappa_tune);
//		out.write(sep + "ttp-tune = " + result.pdm_ttp_tune);
//		out.write(sep + "gamma-tune = " + result.pdm_gamma_tune);
		if (result.pdm_use_beta.equals("true"))
			out.write(sep + "beta-tune = " + result.pdm_beta_tune);
		if (result.pdm_molecular_clock.equals("true"))
			out.write(sep + "outgroup = " + result.pdm_outgroup);
		
////	out.write(sep + "initial-tree-type = " + "random");
//		out.write(sep + "max-initial-tree-height = " + result.pdm_max_initial_tree_height);
//		out.write(sep + "tree-file = " + result.pdm_tree_file);

		out.write(sep + "window-interval = 10000");
		out.write(sep + "sample-interval = 200");	// also in AutoCorrelationTime.lengthBurnIn() and sampleSize()
		out.write(sep + "file-root = run1");
		out.write(sep + "newick-format = true");
		out.write(sep + "likelihood-model = F84");
		out.write(sep + "single-kappa = true");
		out.write(sep + "initial-theta = 1.0");
		out.write(sep + "estimate-pi = false");
		out.write(sep + "data-file = dna.in");
		out.write(sep + "initial-tree-type = neighbor-joining");
		
		double[] freqs = ss.getParams().getFrequencies();		
		out.write(sep + "initial-pia = " + freqs[0]); //+ result.pdm_initial_pia);
		out.write(sep + "initial-pic = " + freqs[1]); //+ result.pdm_initial_pig);
		out.write(sep + "initial-pig = " + freqs[2]); //+ result.pdm_initial_pic);
		out.write(sep + "initial-pit = " + freqs[3]); //+ result.pdm_initial_pit);
		
		out.write(sep + "initial-kappa = " + ss.getParams().getKappa());
		
		System.out.println("PIA: " + freqs[0]);
	}
}