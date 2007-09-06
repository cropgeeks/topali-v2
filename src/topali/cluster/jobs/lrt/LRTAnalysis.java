// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.lrt;

import java.io.*;

import pal.alignment.SimpleAlignment;
import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

class LRTAnalysis extends AnalysisThread
{
	private SequenceSet ss;

	private LRTResult result;

	// Array of LRT scores for each window position
	double[][] data;

	// Maximum LRT value found
	double maximum;

	private File pctDir;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new LRTAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	LRTAnalysis(File runDir)
	{
		super(runDir);
	}

	@Override
	public void runAnalysis() throws Exception
	{
		// Read the LRTResult
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		result = (LRTResult) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "lrt.fasta"));

		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(result, runDir
				.getParentFile().getName(), runDir.getName());

		// Percent directory
		pctDir = new File(runDir, "percent");
		int window = result.window;
		int step = result.step;

		int windowCount = ((ss.getLength() - window) / step) + 1;
		int firstWinPos = (int) (1 + (window / 2f - 0.5));

		// Initialize the array to hold the data results
		data = new double[windowCount][2];

		// Initialize the array to hold the mini-alignments for each window
		SimpleAlignment[][] win = new SimpleAlignment[windowCount][3];

		int pos = firstWinPos;
		int w = 1;

		for (int i = 0; i < data.length; i++, pos += step, w += step)
		{
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");

			new File(wrkDir, "window" + (i + 1)).mkdir();

			// Set position for each data point
			data[i][0] = pos;

			// 1st half window
			final int win1S = w;
			final int win1E = w + (window / 2) - 1;

			// 2nd half window
			final int win2S = w + (window / 2);
			final int win2E = w + window - 1;

			// Strip out the two partitions
			win[i][0] = ss.getAlignment(win1S, win1E, true);
			win[i][1] = ss.getAlignment(win2S, win2E, true);
			win[i][2] = ss.getAlignment(win1S, win2E, true);

			// Work out the LRT statistic
			// alpha, ratio
			LRT lrt = new LRT(result, win[i], 1.86, 4.29);
			data[i][1] = lrt.calculate();

			// Is it bigger than the current maximum?
			if (data[i][1] > maximum)
				maximum = data[i][1];

			int percent = (int) (i / (float) data.length * 100);
			ClusterUtils.setPercent(pctDir, percent);
		}

		writeResults();

		ClusterUtils.setPercent(pctDir, 105);

		ClusterUtils.emptyDirectory(wrkDir, true);
	}

	private void writeResults()
	{
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(
					new FileWriter(new File(runDir, "out.xls")));
			out.write("" + data.length + "\t" + maximum);
			out.newLine();
			out.newLine();

			for (int i = 0; i < data.length; i++)
			{
				out.write(data[i][0] + "\t" + data[i][1]);
				out.newLine();
			}
		} catch (IOException e)
		{
			System.out.println(e);
		}

		try
		{
			if (out != null)
				out.close();
		} catch (Exception e)
		{
		}
	}
}