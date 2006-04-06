// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.dss;

import java.io.*;
import pal.alignment.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

class DSSAnalysis extends MultiThread
{	
	private SequenceSet ss;
	
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	private File runDir, wrkDir;
	// And settings
	private DSSResult result;
	
	// Array of DSS scores for each window position
	double[][] data;
	// Maximum DSS value found
	double maximum;

	
	public static void main(String[] args)
	{ 
		DSSAnalysis analysis = null;
				
		try
		{
			analysis = new DSSAnalysis(new File(args[0]));
			analysis.run();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(analysis.runDir, "error.txt"), e);
		}
	}
	
	DSSAnalysis(File runDir)
		throws Exception
	{
		// Data directory
		this.runDir = runDir;

		// Read the DSSResult
		File resultFile = new File(runDir.getParentFile(), "submit.xml");
		result = (DSSResult) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = new SequenceSet(new File(runDir, "dss.fasta"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result,	runDir.getParentFile().getName(), runDir.getName());
	}
	
	public void run()
	{
		s = System.currentTimeMillis();
		long s2 = System.currentTimeMillis();
				
		try
		{
			int window = result.window;
			int step = result.step;			
			int windowCount = (int) ((ss.getLength() - window) / step) + 1;
			int firstWinPos = (int) (1 + (window / 2f - 0.5));
			
			// Initialize the array to hold the data results
			data = new double[windowCount][2];
			
			// Initialize the array to hold the DSS details for each window
			DSS[] dssWin = new DSS[windowCount];
			
			
			
			
			// 18/09/05 - Analysis now split into three sections
			// Rather than run each window and calculate fitch/DSS for each win
			// the analyis now runs fitch for all windows first, then calculates
			// DSS scores at the end. This is so Fitch can be run externally of
			// Java using a single script that runs all the instances of it
			
			// 1) Generate the mini alignments for each window
			int pos = firstWinPos;
			int w = 1;			
			for (int i = 0;  i < data.length; i++, pos += step, w += step)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");
				
				// Set position for each data point
				data[i][0] = pos;
				
				// 1st half window
				final int win1S = w;
				final int win1E = w+(window/2)-1;			
										
				// 2nd half window
				final int win2S = w+(window/2);
				final int win2E = w+window-1;
				
				// Strip out the two partitions
				SimpleAlignment win1 = ss.getAlignment(win1S, win1E, true);
				SimpleAlignment win2 = ss.getAlignment(win2S, win2E, true);
				
				File dssWrkDir = new File(wrkDir, "win"+(i+1));
				dssWin[i] = new DSS(dssWrkDir, result, win1, win2);
			}
			System.out.println("Data written in " + (System.currentTimeMillis()-s2));
			s2 = System.currentTimeMillis();
			
			// 2) Run all the Fitch calculations
			for (int i = 0;  i < data.length; i++)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");
				dssWin[i].calculateFitchScores();
			}
			RunFitch fitch = new RunFitch(result);
			fitch.runFitchScripts(wrkDir, windowCount);
			System.out.println("Fitch run in " + (System.currentTimeMillis()-s2));
			s2 = System.currentTimeMillis();
			
			
			// 3) Perform actual DSS calculations (using Fitch results)
			for (int i = 0;  i < data.length; i++)
			{
				if (LocalJobs.isRunning(result.jobId) == false)
					throw new Exception("cancel");
				
				// Work out the DSS statistic
				data[i][1] = dssWin[i].calculateDSS();
				
				// Is it bigger than the current maximum?
				if (data[i][1] > maximum)
					maximum = data[i][1];
			}
			
			writeResults();
			System.out.println("DSS in " + (System.currentTimeMillis()-s2));
		}
		catch (Exception e)
		{
			if (e.getMessage().equals("cancel") == false)			
				ClusterUtils.writeError(new File(runDir, "error.txt"), e);
		}
		
		ClusterUtils.emptyDirectory(wrkDir, true);		
		giveToken();
	}
	
	private void writeResults()
	{
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new FileWriter(new File(runDir, "out.xls")));
			out.write("" + data.length + "\t" + maximum);
			out.newLine();
			out.newLine();
			
			for (int i = 0; i < data.length; i++)
			{
				out.write(data[i][0] + "\t" + data[i][1]);
				out.newLine();
			}
		}
		catch (IOException e) { System.out.println(e); }
		
		try { if (out != null) out.close(); }
		catch (Exception e) {}
	}
}