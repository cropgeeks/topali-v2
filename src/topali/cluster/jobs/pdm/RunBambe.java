// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

import java.io.*;

import topali.cluster.StreamCatcher;
import topali.data.PDMResult;

class RunBambe
{
	private File wrkDir;

	private PDMResult result;

	RunBambe(File wrkDir, PDMResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}

	private void deleteFiles()
	{
		// Delete any existing output from Bambe
		File[] file = wrkDir.listFiles();
		for (int i = 0; i < file.length; i++)
			if (file[i].getName().toLowerCase().startsWith("run1"))
			{
				while (!file[i].delete())
				{
					System.gc();
					System.out.println("DELETE fail on " + file[i]);

					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e)
					{
					}
				}
			}
	}

	void runBambe() throws Exception
	{
		deleteFiles();

		ProcessBuilder pb = null;
		// if (ClusterUtils.isWindows)
		// pb = new ProcessBuilder("" + new File(wrkDir, "runbambe.bat"));
		// else
		// pb = new ProcessBuilder("" + new File(wrkDir, "runbambe"));
		pb = new ProcessBuilder(result.bambePath);

		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();

		PrintWriter out = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));
		new StreamCatcher(proc.getInputStream(), true);

		writeFile(out);
		out.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	private void writeFile(PrintWriter out) throws Exception
	{
		String sep = System.getProperty("line.separator");

		out.write("seed = " + result.pdm_seed);
		out.write(sep + "burn = " + result.pdm_burn);
		out.write(sep + "cycles = " + result.pdm_cycles);
		out.write(sep + "burn-algorithm = " + result.pdm_burn_algorithm);
		out.write(sep + "main-algorithm = " + result.pdm_main_algorithm);
		out.write(sep + "use-beta = " + result.pdm_use_beta);
		out.write(sep + "molecular-clock = " + result.pdm_molecular_clock);
		out.write(sep + "category-list = " + result.pdm_category_list);
		// out.write(sep + "initial-ttp = " + result.pdm_initial_ttp);
		// out.write(sep + "initial-gamma = " + result.pdm_initial_gamma);
		out.write(sep + "parameter-update-interval = "
				+ result.pdm_parameter_update_interval);
		// out.write(sep + "update-kappa = " + result.pdm_update_kappa);
		out.write(sep + "update-theta = " + result.pdm_update_theta);
		// out.write(sep + "update-pi = " + result.pdm_update_pi);
		// out.write(sep + "update-ttp = " + result.pdm_update_ttp);
		// out.write(sep + "update-gamma = " + result.pdm_update_gamma);
		out.write(sep + "tune-interval = " + result.pdm_tune_interval);
		out.write(sep + "global-tune = " + result.pdm_global_tune);
		out.write(sep + "local-tune = " + result.pdm_local_tune);
		out.write(sep + "theta-tune = " + result.pdm_theta_tune);
		// out.write(sep + "pi-tune = " + result.pdm_pi_tune);
		// out.write(sep + "kappa-tune = " + result.pdm_kappa_tune);
		// out.write(sep + "ttp-tune = " + result.pdm_ttp_tune);
		// out.write(sep + "gamma-tune = " + result.pdm_gamma_tune);
		if (result.pdm_use_beta.equals("true"))
			out.write(sep + "beta-tune = " + result.pdm_beta_tune);
		if (result.pdm_molecular_clock.equals("true"))
			out.write(sep + "outgroup = " + result.pdm_outgroup);

		// // out.write(sep + "initial-tree-type = " + "random");
		// out.write(sep + "max-initial-tree-height = " +
		// result.pdm_max_initial_tree_height);
		// out.write(sep + "tree-file = " + result.pdm_tree_file);

		out.write(sep + "window-interval = 10000");
//		out.write(sep + "sample-interval = " + ((int)(result.pdm_cycles / 30))); // also in
													// AutoCorrelationTime.lengthBurnIn()
													// and sampleSize()
		out.write(sep + "sample-interval = 200");													
													
		out.write(sep + "file-root = run1");
		out.write(sep + "newick-format = true");
		out.write(sep + "likelihood-model = F84");
		out.write(sep + "single-kappa = true");
		out.write(sep + "initial-theta = 1.0");
		out.write(sep + "estimate-pi = false");
		out.write(sep + "data-file = dna.in");
		out.write(sep + "initial-tree-type = neighbor-joining");

		double[] freqs = result.frequencies;
		out.write(sep + "initial-pia = " + freqs[0]); // +
														// result.pdm_initial_pia);
		out.write(sep + "initial-pic = " + freqs[1]); // +
														// result.pdm_initial_pig);
		out.write(sep + "initial-pig = " + freqs[2]); // +
														// result.pdm_initial_pic);
		out.write(sep + "initial-pit = " + freqs[3]); // +
														// result.pdm_initial_pit);

		out.write(sep + "initial-kappa = " + result.kappa);

		System.out.println("PIA: " + freqs[0]);
	}
}