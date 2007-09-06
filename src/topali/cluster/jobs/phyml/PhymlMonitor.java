// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.File;

import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.data.PhymlResult;
import topali.fileio.Castor;

public class PhymlMonitor
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private File jobDir;

	public PhymlMonitor(File jobDir) throws Exception
	{
		this.jobDir = jobDir;
	}

	public JobStatus getPercentageComplete() throws Exception
	{
		float progress = 0;
		
		if (new File(jobDir, "error.txt").exists())
		{
			log.warn(jobDir.getName() + " - error.txt found");
			throw new Exception("Phyml error");
		}
		
		if (new File(jobDir, "result.xml").exists())
			progress = 100f;
		
		return new JobStatus(progress, 0, "_status");
	}

	public PhymlResult getResult() throws Exception
	{
		return (PhymlResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
	
}
