package topali.cluster.jobs.cml;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CodeMLMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	private File jobDir;
	
	public CodeMLMonitor(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public JobStatus getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("CML error.txt");
		}
		
		String text = "";
		
		for (int i = 1; i <= 8; i++)
		{
			File runDir = new File(jobDir, "run" + i);
			
			text += "run" + i + "=" + (new File(runDir, "ok").exists()) + " ";
			
			// But also check if an error file for this run exists
			if (new File(runDir, "error.txt").exists())
			{
				logger.severe(jobDir.getName() + " - error.txt found for run " + i);
				throw new Exception("CML error.txt (run " + i + ")");
			}
		}
		
		return new JobStatus(0, 0, text);
	}
	
	public CodeMLResult getResult()
		throws Exception
	{
		return (CodeMLResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}