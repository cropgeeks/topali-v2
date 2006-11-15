package topali.cluster.jobs.trees;

import java.io.*;
import java.util.logging.*;

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
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("MBTree error.txt");
		}
		
		if (new File(jobDir, "tree.txt").exists())
			return 100f;
		else
			return (float) Math.random() * 99;
	}
	
	public MBTreeResult getResult()
		throws Exception
	{
		return (MBTreeResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}