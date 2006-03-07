// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.lrt;

import java.io.*;
import java.util.*;

import pal.alignment.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

class LRTAnalysis extends MultiThread
{	
	// The two windows that will be analyzed
	private SequenceSet ss;
	
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	private File runDir, wrkDir;
	// And settings
	private LRTResult result;
	
	// Array of LRT scores for each window position
	double[][] data;
	// Maximum LRT value found
	double maximum;

	
	public static void main(String[] args)
	{ 
		LRTAnalysis analysis = null;
		
		try
		{
			analysis = new LRTAnalysis(new File(args[0]));
			analysis.run();
		}
		catch (Exception e)
		{
			System.out.println("LRTAnalysis: " + e);
			ClusterUtils.writeError(new File(analysis.runDir, "error.txt"), e);
		}
	}
	
	LRTAnalysis(File runDir)
		throws Exception
	{
		// Data directory
		this.runDir = runDir;

		// Read the LRTResult
		File resultFile = new File(runDir.getParentFile(), "result.xml");
		result = (LRTResult) Castor.unmarshall(resultFile);
		// Read the SequenceSet
		ss = (SequenceSet) Castor.unmarshall(new File(runDir, "ss.xml"));
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result,	runDir.getParentFile().getName(), runDir.getName());
	}
	
	public void run()
	{
		s = System.currentTimeMillis();
		System.out.println(runDir);
				
		try
		{
			int window = result.window;
			int step = result.step;
			
			int windowCount = (int) ((ss.getLength() - window) / step) + 1;
			int firstWinPos = (int) (1 + (window / 2f - 0.5));
			
			// Initialize the array to hold the data results
			data = new double[windowCount][2];
			
			// Initialize the array to hold the mini-alignments for each window
			SimpleAlignment[][] win = new SimpleAlignment[windowCount][3];
			
			
			int pos = firstWinPos;
			int w = 1;
			
			for (int i = 0;  i < data.length; i++, pos += step, w += step)
			{
				if (i % 100 == 0)
					System.out.println(i);
				
				new File(wrkDir, "window" + (i+1)).mkdir();
				
				// Set position for each data point
				data[i][0] = pos;
				
				// 1st half window
				final int win1S = w;
				final int win1E = w+(window/2)-1;			
										
				// 2nd half window
				final int win2S = w+(window/2);
				final int win2E = w+window-1;
				
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
			}
			
			writeResults();
		}
		catch (Exception e)
		{
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