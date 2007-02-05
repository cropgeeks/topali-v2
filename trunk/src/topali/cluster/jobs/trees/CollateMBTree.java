package topali.cluster.jobs.trees;

import java.io.*;
import java.util.logging.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollateMBTree
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	private File jobDir;
	
	public CollateMBTree(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public JobStatus getPercentageComplete()
		throws Exception
	{
		float progress = 0;
		
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("MBTree error.txt");
		}
		
		if (new File(jobDir, "tree.txt").exists())
			progress = 100f;
		
		return new JobStatus(progress, 0, "_status");
	}
	
	public MBTreeResult getResult()
		throws Exception
	{
		return (MBTreeResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}