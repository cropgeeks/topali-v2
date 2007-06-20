// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.hmm;

import java.io.File;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class RunHMM extends Thread
{
	private SequenceSet ss;

	private HMMResult result;

	// Directory where the job will run
	private File jobDir;

	public RunHMM(File jobDir, SequenceSet ss, HMMResult result)
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

		// Store the HMMResult object where it can be read by the sub-job
		Castor.saveXML(result, new File(jobDir, "submit.xml"));
		// Store the SequenceSet where it can be read by the sub-job
		ss.save(new File(jobDir, "hmm.fasta"), Filters.FAS, false);

		if (result.isRemote)
			HMMWebService.runScript(jobDir);
		else
			new HMMAnalysis(jobDir).start(LocalJobs.manager);
	}
}