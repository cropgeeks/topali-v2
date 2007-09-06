// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class RunFastML extends Thread
{

	private SequenceSet ss;

	private FastMLResult result;

	// Directory where the job will run
	private File jobDir;

	public RunFastML(File jobDir, SequenceSet ss, FastMLResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	@Override
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
		// // Create the percent tracking directory
		// new File(jobDir, "percent").mkdir();

		// Store the MBTreeResult object where it can be read by the sub-job
		Castor.saveXML(result, new File(jobDir, "submit.xml"));
		// Store the SequenceSet where it can be read by the sub-job
		Castor.saveXML(ss, new File(jobDir, "ss.xml"));

		if (result.isRemote)
			FastMLWebService.runScript(jobDir);
		else {
			FastMLAnalysis ana = new FastMLAnalysis(jobDir);
			ana.start(LocalJobs.manager);
		}
	}
	
	
	
}
