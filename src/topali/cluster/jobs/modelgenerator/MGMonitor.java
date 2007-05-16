// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.File;

import topali.cluster.JobStatus;
import topali.data.MGResult;
import topali.fileio.Castor;

public class MGMonitor
{
	
	private File jobDir;

	public MGMonitor(File jobDir) throws Exception
	{
		this.jobDir = jobDir;
	}

	public JobStatus getPercentageComplete() throws Exception
	{
		float progress = 0;

		if (new File(jobDir, "result.xml").exists())
			progress = 100f;

		return new JobStatus(progress, 0, "_status");
	}

	public MGResult getResult() throws Exception
	{
		return (MGResult) Castor.unmarshall(new File(jobDir, "result.xml"));
	}
}
