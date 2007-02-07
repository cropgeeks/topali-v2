// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.hmm;

import java.io.*;
import java.util.logging.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class CollateHMM
{
	private static Logger logger = Logger.getLogger("topali.cluster.info-log");
	
	private File jobDir;
	
	public CollateHMM(File jobDir)
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
			throw new Exception("HMM error.txt");
		}
		
		// Percentages are tracked by having one file for every 1% stored in a
		// directory called "percent"
		// HOWEVER, we cheat a bit and don't assume completion until 105% done!
		try 
		{ 
			int count = new File(jobDir, "percent").listFiles().length;
			progress = ((float)count / 105f) * 100f;
		}
		catch (Exception e)	{}
		
		return new JobStatus(progress, 0, "_status");
	}
	
	public HMMResult getResult()
		throws Exception
	{
		return (HMMResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}