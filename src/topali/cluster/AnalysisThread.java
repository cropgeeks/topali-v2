// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.io.File;

import sbrn.commons.multicore.TokenThread;

// Base class for the analysis tasks that run the TOPALi jobs. Just holds some
// common fields for them (the directory for results and working directory)
// and starts the analysis when the run method is called (which may or may NOT
// be a threaded call). (It's threaded for local jobs, but not for cluster).
public abstract class AnalysisThread extends TokenThread
{
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	protected File runDir;

	protected File wrkDir;

	protected AnalysisThread(File runDir)
	{
		this.runDir = runDir;
	}

	// The run wraps the main runAnalysis() method, catching any exceptions and
	// writing an error file to the runDir for this task
	public void run()
	{
		try
		{
			runAnalysis();
		}
		// Catch the error, but only log it if it wasn't a cancel request
		catch (Exception e)
		{
			if (e.getMessage().equals("cancel") == false)
				ClusterUtils.writeError(new File(runDir, "error.txt"), e);
		}

		// Release the shared token that will have been assigned to this job if
		// it's running locally rather than on the cluster
		giveToken();
	}

	// Sub classes must implement this to do their actual work
	public abstract void runAnalysis() throws Exception;
}
