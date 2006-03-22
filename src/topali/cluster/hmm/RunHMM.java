// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.hmm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

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
			// Ensure the directory for this job exists
			jobDir.mkdirs();
			// Create the percent tracking directory
			new File(jobDir, "percent").mkdir();
			
			// Store the HMMResult object where it can be read by the sub-job
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			// Store the SequenceSet where it can be read by the sub-job
			ss.save(new File(jobDir, "hmm.fasta"), Filters.FAS, false);
						
			// Run the analysis
			runAnalysis();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void runAnalysis()
		throws Exception
	{
		if (result.isRemote)
			HMMWebService.runScript(jobDir);
		else
		{
			ThreadManager manager = new ThreadManager();
						
			HMMAnalysis analysis = new HMMAnalysis(jobDir);
			analysis.startThread(manager);
		}
	}
}