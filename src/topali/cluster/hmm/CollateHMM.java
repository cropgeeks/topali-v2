package topali.cluster.hmm;

import java.io.*;

import topali.data.*;
import topali.fileio.*;

public class CollateHMM
{
	private File jobDir;
	
	public CollateHMM(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
			throw new Exception("HMM error.txt");
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		try { return new File(jobDir, "percent").listFiles().length; }
		catch (Exception e)	{
			return 0;
		}
	}
	
	public HMMResult getResult()
		throws Exception
	{
		return (HMMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}