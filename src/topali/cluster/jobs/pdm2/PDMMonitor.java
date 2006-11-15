package topali.cluster.jobs.pdm2;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.data.*;
import topali.fileio.*;

public class PDMMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private File jobDir;
	
	public PDMMonitor(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "ok").exists())
			return 100f;
		
		float percent = this.getParallelPercentageComplete();
		
//		if (true)
//			return percent;
		
		// TODO: check for completion of the PDM2PostAnalysis run
		// if (complete
			// return 100
		// else
			// Fudge the return so it doesn't appear finished until 105% complete
			return (percent / 105) * 100f;
	}
	
	// Returns the percentage completion for the parallel section of the job
	// (ie the main window-along-an-alignment bit)
	float getParallelPercentageComplete()
		throws Exception
	{
		System.out.println();
		System.out.println("Checking for progress");
		
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe("error.txt generated for " + jobDir.getPath());
			throw new Exception("PDM2 error.txt");
		}
		
		// Calculate a running percentage total for each sub-job directory
		int runs = 0, total = 0;
		
		File[] files = new File(jobDir, "nodes").listFiles();
		for (File f: files)
		{
			System.out.print(f + " ");
			runs++;
			
			if (new File(f, "error.txt").exists())
			{
				logger.severe("error.txt generated for " + jobDir.getPath() + " on run " + runs);
				throw new Exception("PDM2 error.txt in subjob " + runs);
			}
			
			// Check the percentage complete for this job
			File pDir = new File(f, "percent");
			if (pDir.exists())
				total += pDir.listFiles().length;
			
			try { System.out.println(pDir.listFiles().length); }
			catch (Exception e) { e.printStackTrace(System.out); }
			
//			try { total += new File(f, "percent").listFiles().length; }
//			catch (Exception e) { System.out.println(e); }
		}
		
		System.out.println("PERCENT: total=" + total + " and runs=" + runs);
		
		// Now work out the overal percentage
		return ((float) total) / ((float) runs);
	}
	
	public PDM2Result getResult()
		throws Exception
	{
		return (PDM2Result) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}