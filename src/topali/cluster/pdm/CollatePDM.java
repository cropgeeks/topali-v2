package topali.cluster.pdm;

import java.io.*;
import java.util.logging.*;

import topali.data.*;
import topali.fileio.*;

public class CollatePDM
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private File jobDir;
	
	public CollatePDM(File jobDir)
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
			throw new Exception("PDM error.txt");
		}
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		// HOWEVER, we cheat a bit and don't assume completion until 105% done!
		try 
		{ 
			int count = new File(jobDir, "percent").listFiles().length;
			return ((float)count / 105f) * 100f;
		}
		catch (Exception e)	{
			return 0;
		}
	}
	
	public PDMResult getResult()
		throws Exception
	{
		return (PDMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}