package topali.cluster.pdm;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollatePDM
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private File jobDir;
	private PDMResult result;
	
	public CollatePDM(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
		
		result = (PDMResult) Castor.unmarshall(new File(jobDir, "submit.xml"));
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe("error.txt generated for " + jobDir.getPath());
			throw new Exception("PDM error.txt");
		}

		// We need to count the total number of pXX files in all the run subdir
		// percent directories. They should add up (if finished) to the number
		// of runs times 100 files (105 files in reality due to pruning fudge)
		
		float total = 0;
		int runs = result.pdm_runs * 105; // catch the 105% fudge
				
		for (int i = 1; i <= runs; i++)
		{
			String runName = "run" + i;
			File runDir = new File(jobDir, runName);
			
			int count = 0;
			try { count = new File(runDir, "percent").listFiles().length; }
			catch (Exception e) {}
			total += count;
				
			// But also check if an error file for this run exists
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe("error.txt generated for " + jobDir.getPath() + " on run " + i);
				throw new Exception("PDM error.txt (run " + i + ")");
			}
		}
		
		// Return this total as a percentage
		return (total / ((float) runs)) * 100;
	}
	
	public PDMResult getResult()
		throws Exception
	{
		// Read the main result
		File runResult = new File(new File(jobDir, "run1"), "result.xml");
		result = (PDMResult) Castor.unmarshall(runResult);
		
		// Collect the maximum values found in the bootstrap runs
		result.thresholds = new float[result.pdm_runs-1];
		for (int i = 0; i < result.thresholds.length; i++)
		{
			File runDir = new File(jobDir, "run" + (i+2));
			String max = ClusterUtils.readFile(new File(runDir, "max.txt"));
			
			result.thresholds[i] = Float.parseFloat(max);
			System.out.println("Read " + result.thresholds[i]);
		}
		
		Arrays.sort(result.thresholds);
		
		return result;
	}
}