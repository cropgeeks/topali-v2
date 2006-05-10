package topali.cluster.pdm2;

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
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe("error.txt generated for " + jobDir.getPath());
			throw new Exception("PDM2 error.txt");
		}
		
		// Calculate a running percentage total for each sub-job directory
		int runs = 0, total = 0;
		
		File[] files = jobDir.listFiles();
		for (File f: files)
		{
			if (f.isDirectory() == false)
				continue;
			else
				runs++;
			
			if (new File(f, "error.txt").exists())
			{
				logger.severe("error.txt generated for " + jobDir.getPath() + " on run " + runs);
				throw new Exception("PDM2 error.txt in subjob " + runs);
			}
			
			// Check the percentage complete for this job
			try { total += new File(f, "percent").listFiles().length; }
			catch (Exception e) {}
		}
		
		// Now work out the overal percentage
		return total / (float) runs;
	}
	
	public PDM2Result getResult()
		throws Exception
	{
		// Read in the result object
		// TODO: Collate results so that result.xml is read rather than submit.xml
		PDM2Result result =
			(PDM2Result) Castor.unmarshall(new File(jobDir, "submit.xml"));
		
		result.thresholdCutoff = 0.95f;
		result.thresholds = new float[5];

		// Create a (temp) vector to hold the 'y' data we'll read from each file
		Vector<Float> v = new Vector<Float>(1000);
		
		File[] files = jobDir.listFiles();
		for (File f: files)
		{
			if (f.isDirectory() == false)
				continue;
			
			BufferedReader in = new BufferedReader(
				new FileReader(new File(f, "out.xls")));
			
			String str = in.readLine();
			while (str != null && str.length() > 0)
			{
				v.add(Float.parseFloat(str));
				str = in.readLine();
			}
			
			in.close();
		}
		
		
		// Now convert this data into a 2D array, complete with x values for
		// each of the y's we've just read
		result.locData = new float[v.size()][2];
		
		int pos = 1+ (int)((result.pdm_window/2f - 0.5) + (result.pdm_step/2f));
		
		for (int i = 0; i < result.locData.length; i++, pos += result.pdm_step)
		{
			result.locData[i][0] = pos;
			result.locData[i][1] = v.get(i);
		}
		
		
		return result;
	}
}