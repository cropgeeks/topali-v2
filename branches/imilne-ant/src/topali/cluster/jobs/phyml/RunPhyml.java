// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class RunPhyml extends Thread
{
	private SequenceSet ss;

	private PhymlResult result;

	// Directory where the job will run
	private File jobDir;

	public RunPhyml(File jobDir, SequenceSet ss, PhymlResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	public void run()
	{
		try
		{
			startThread();
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	private void startThread() throws Exception
	{
		// Ensure the directory for this job exists
		jobDir.mkdirs();

		Castor.saveXML(result, new File(jobDir, "submit.xml"));
		
		Castor.saveXML(ss, new File(jobDir, "ss.xml"));

		if (result.isRemote)
			PhymlWebService.runScript(jobDir);
		else
			new PhymlAnalysis(jobDir).start(LocalJobs.manager);
	}
	
}
