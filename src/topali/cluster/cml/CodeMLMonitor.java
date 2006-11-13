package topali.cluster.cml;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.data.*;
import topali.fileio.*;

public class CodeMLMonitor
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private File jobDir;
	
	public CodeMLMonitor(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		return 0;
	}
	
	public CodeMLResult getResult()
		throws Exception
	{
		return (CodeMLResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}