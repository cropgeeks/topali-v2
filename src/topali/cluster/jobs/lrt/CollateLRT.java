package topali.cluster.jobs.lrt;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.data.*;
import topali.fileio.*;

public class CollateLRT
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	private File jobDir;
	private LRTResult result;
	
	public CollateLRT(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;

		result = (LRTResult) Castor.unmarshall(new File(jobDir, "submit.xml"));
	}
	
	/*
	 * Works out the percentage complete for the current LRT run. This is
	 * calculated by counting each instance of "out.xls" - there should be one
	 * file for each run directory. This is then returned as a percentage of
	 * the total number of runs.
	 */
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("LRT error.txt");
		}
		
		float count = 0;
		int runs = result.runs;
				
		for (int i = 1; i <= runs; i++)
		{
			String runName = "run" + i;
			File runDir = new File(jobDir, runName);
			
			// If the output for this run exists, increment the counter
			if (new File(runDir, "out.xls").exists())
				count++;
				
			// But also check if an error file for this run exists
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe(jobDir.getName() + " - error.txt found for run " + i);
				throw new Exception("LRT error.txt (run " + i + ")");
			}
		}
		
		
		return (count / ((float) runs)) * 100;
	}
	
	/*
	 * Returns the final LRTResult object. Each run directory is scanned for its
	 * output - run1 contains the data required to plot the graph, whereas all
	 * other run directories contain information on the maximum threshold value
	 * found during that run.
	 */
	public LRTResult getResult()
		throws Exception
	{
		result.thresholds = new float[result.runs-1];
		
		for (int i = 1; i <= result.runs; i++)
		{
			File runDir = new File(jobDir, "run" + i);
			File xlsFile = new File(runDir, "out.xls");
			
			if (i == 1)
				setLRTData(xlsFile);
			else
				setThresholdData(xlsFile, i-2);
		}
		
		// Finally, sort the threshold data
		Arrays.sort(result.thresholds);
		
		// Save out the result object (not really needed but useful for debug)
		Castor.saveXML(result, new File(jobDir, "result.xml"));
		
		return result;
	}
	
	private void setLRTData(File xlsFile)
		throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(xlsFile));
		
		// Read the header to determine how many windows were run
		StringTokenizer st = new StringTokenizer(in.readLine());
		int count = Integer.parseInt(st.nextToken());				
		in.readLine();
		
		result.data = new float[count][2];
		
		// Then read the x and y values for the graph
		for (int j = 0; j < result.data.length; j++)
		{
			st = new StringTokenizer(in.readLine());
			result.data[j][0] = Float.parseFloat(st.nextToken());
			result.data[j][1] = Float.parseFloat(st.nextToken());
		}
			
		in.close();
	}
	
	private void setThresholdData(File xlsFile, int x)
		throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(xlsFile));
		
		// Read the header to determine the maximum y value found
		StringTokenizer st = new StringTokenizer(in.readLine());
		st.nextToken();
		
		result.thresholds[x] = Float.parseFloat(st.nextToken());
			
		in.close();
	}
}