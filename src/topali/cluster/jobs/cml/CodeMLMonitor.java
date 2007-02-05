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
		return new JobStatus();
	}
	
	public CodeMLResult getResult()
		throws Exception
	{
		return (CodeMLResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}