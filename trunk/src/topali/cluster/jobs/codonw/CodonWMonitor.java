// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.codonw;

import java.io.File;

import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.data.CodonWResult;
import topali.fileio.Castor;

public class CodonWMonitor
{
	private File jobDir;
	Logger log = Logger.getLogger(this.getClass());
	
	public CodonWMonitor(File jobDir) throws Exception
	{
		this.jobDir = jobDir;
	}

	public JobStatus getPercentageComplete() throws Exception
	{
		float progress = 0;
		
		if (new File(jobDir, "error.txt").exists())
		{
			log.warn(jobDir.getName() + " - error.txt found");
			throw new Exception("CodonW error");
		}
		
		if (new File(jobDir, "result.xml").exists())
			progress = 100f;
		
		return new JobStatus(progress, 0, "_status");
	}

	public CodonWResult getResult() throws Exception
	{
		return (CodonWResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}
