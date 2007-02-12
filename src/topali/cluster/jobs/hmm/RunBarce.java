// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.hmm;

import java.io.*;

import topali.cluster.*;
import topali.data.HMMResult;

class RunBarce extends StoppableProcess
{
	private File wrkDir, jobDir;

	RunBarce(HMMResult result, File wrkDir, File jobDir)
	{
		this.result = result;
		this.wrkDir = wrkDir;
		this.jobDir = jobDir;

		runCancelMonitor();
	}

	void runBarce() throws Exception
	{
		HMMResult hmmResult = (HMMResult) result;

		ProcessBuilder pb = new ProcessBuilder(hmmResult.barcePath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		// Read output from barce
		new BarceCatcher(proc.getInputStream(), false);

		// Send barce all its settings!

		// Model settings
		writer.println("1");

		if (hmmResult.hmm_model.equals("JC+gaps"))
			writer.println("m");
		else if (hmmResult.hmm_model.equals("K2P+gaps"))
		{
			writer.println("m");
			writer.println("m");
		} else if (hmmResult.hmm_model.equals("F81+gaps"))
		{
			writer.println("m");
			writer.println("m");
			writer.println("m");
		}

		if (hmmResult.hmm_initial.equals("No"))
		{
			writer.println("e");

			writer.println("f");
			writer.println("y");
			writer.println(hmmResult.hmm_freq_est_1 + " "
					+ hmmResult.hmm_freq_est_2 + " " + hmmResult.hmm_freq_est_3
					+ " " + hmmResult.hmm_freq_est_4);
		}

		if (hmmResult.hmm_transition.equals("No"))
			writer.println("r");

		// writer.println("p");
		// writer.println(hmmResult.hmm_freq_1 + " " + hmmResult.hmm_freq_2 + "
		// "
		// + hmmResult.hmm_freq_3);

		writer.println("d");
		writer.println(hmmResult.hmm_difficulty);

		// //////////////////////////
		// if (hmmResult.hmm_use_mosaic)
		writer.println("j");
		// //////////////////////////

		writer.println("x");
		writer.flush();

		// Run settings
		writer.println("2");

		writer.println("b");
		writer.println(hmmResult.hmm_burn);

		writer.println("n");
		writer.println(hmmResult.hmm_points);

		writer.println("i");
		writer.println(hmmResult.hmm_thinning);

		writer.println("c");
		writer.println(hmmResult.hmm_tuning);

		if (hmmResult.hmm_lambda.equals("No"))
			writer.println("w");
		else
		{
			if (hmmResult.hmm_annealing.equals("PAR"))
				writer.println("q");
			else if (hmmResult.hmm_annealing.equals("PROB"))
			{
				writer.println("q");
				writer.println("q");
			}
		}

		if (hmmResult.hmm_station.equals("No"))
			writer.println("u");

		if (hmmResult.hmm_update.equals("No"))
			writer.println("a");

		writer.println("o");
		writer.println(hmmResult.hmm_branch);

		writer.println("x");
		writer.flush();

		writer.println("y");
		writer.flush();
		writer.close();

		System.out.println("ALL SETTINGS SENT");

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");
		}

		isRunning = false;
	}

	// This is an extension of the normal StreamCatcher that deals with Barce's
	// specific output, in particular, reading the numbers it prints out in
	// order to try and determine how far through the run (percentage complete)
	// Barce has reached.
	class BarceCatcher extends StreamCatcher
	{
		BarceCatcher(InputStream in, boolean showOutput)
		{
			super(in, showOutput);
		}

		public void run()
		{
			File pctDir = new File(jobDir, "percent");

			int read = 0;

			try
			{
				String line = reader.readLine();

				while (line != null)
				{
					line = reader.readLine();

					if (showOutput)
						System.out.println(line);

					// If "::END" is read, then we know it's 100% complete
					if (line.equals("::END"))
						read = 100;

					if (line.startsWith("p="))
						read = Integer.parseInt(line.substring(2));

					ClusterUtils.setPercent(pctDir, read);
				}
			} catch (Exception e)
			{
			}

			try
			{
				reader.close();
			} catch (IOException e)
			{
			}
		}
	}
}