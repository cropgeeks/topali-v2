// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.File;
import java.util.logging.Logger;

import topali.cluster.JobStatus;
import topali.data.MBTreeResult;
import topali.fileio.Castor;

public class MrBayesMonitor
{

	private static Logger logger = Logger.getLogger("topali.cluster.info-log");

	private File jobDir;

	public MrBayesMonitor(File jobDir) throws Exception
	{
		this.jobDir = jobDir;
	}

	public JobStatus getPercentageComplete() throws Exception
	{
		float progress = 0;

		if (new File(jobDir, "error.txt").exists())
		{
			logger.severe(jobDir.getName() + " - error.txt found");
			throw new Exception("MBTree error.txt");
		}
		
		try
		{
			progress = (float)new File(jobDir, "percent").listFiles().length;
			//prevent progress to be 100% (otherwise the result will be requested before it's created)
			if(progress==100)
				progress = 99;
		} catch (RuntimeException e)
		{
			progress = 0;
		}

		if (new File(jobDir, "result.xml").exists())
			progress = 100f;

		return new JobStatus(progress, 0, "_status");
	}

	public MBTreeResult getResult() throws Exception
	{
		return (MBTreeResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
	
}
