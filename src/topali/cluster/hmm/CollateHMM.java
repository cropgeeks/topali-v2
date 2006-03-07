package topali.cluster.hmm;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollateHMM
{
	private File jobDir;
	private HMMResult result;
	
	public CollateHMM(File jobDir)
		throws Exception
	{
		this.jobDir = jobDir;
		
//		String xml = ClusterUtils.readFile(new File(jobDir, "result.xml"));
//		result = Castor.getHMMResult(xml);
		result = (HMMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
	
	public float getPercentageComplete()
		throws Exception
	{
		if (new File(jobDir, "error.txt").exists())
			throw new Exception("HMM error.txt");
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		return new File(jobDir, "percent").listFiles().length;
	}
	
	public HMMResult getResult()
		throws Exception
	{
		return result;
	}
}