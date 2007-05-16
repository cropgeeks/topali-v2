// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.File;

import topali.cluster.ClusterUtils;
import topali.cluster.LocalJobs;
import topali.data.SequenceSet;
import topali.data.MGResult;
import topali.fileio.Castor;

public class RunModelGenerator extends Thread
{

	private SequenceSet ss;

	private MGResult result;

	// Directory where the job will run
	private File jobDir;

	public RunModelGenerator(File jobDir, SequenceSet ss, MGResult result)
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
		// // Create the percent tracking directory
		// new File(jobDir, "percent").mkdir();

		// Store the MBTreeResult object where it can be read by the sub-job
		Castor.saveXML(result, new File(jobDir, "submit.xml"));
		// Store the SequenceSet where it can be read by the sub-job
		Castor.saveXML(ss, new File(jobDir, "ss.xml"));

		if (result.isRemote)
			MGWebservice.runScript(jobDir);
		else
			new MGAnalysis(jobDir).start(LocalJobs.manager);
	}
	
}
