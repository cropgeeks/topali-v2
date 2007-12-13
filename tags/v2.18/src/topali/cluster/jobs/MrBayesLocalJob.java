// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.mrbayes.*;
import topali.data.*;
import topali.gui.*;

public class MrBayesLocalJob extends AnalysisJob
{

	private SequenceSet ss;

	private File jobDir;

	public MrBayesLocalJob(MBTreeResult result, AlignmentData data)
	{
		this.result = result;
		this.data = data;
		this.ss = data.getSequenceSet();
		result.startTime = System.currentTimeMillis();
		result.jobId = "" + System.currentTimeMillis();
		result.tmpDir = Prefs.tmpDir.getPath();

		jobDir = new File(Prefs.tmpDir, result.jobId);

		LocalJobs.addJob(result.jobId);
	}

	@Override
	public String ws_submitJob() throws Exception
	{
		try
		{
			RunMrBayes mb = new RunMrBayes(jobDir, ss, (MBTreeResult) result);
			mb.start();

			result.status = JobStatus.RUNNING;
			return result.jobId;
		} catch (Exception e)
		{
			System.out.println(e);
			result.status = JobStatus.FATAL_ERROR;
			throw e;
		}
	}

	@Override
	public JobStatus ws_getProgress() throws Exception
	{
		return new MrBayesMonitor(jobDir).getPercentageComplete();
	}

	@Override
	public AnalysisResult ws_downloadResult() throws Exception
	{
		result = new MrBayesMonitor(jobDir).getResult();
		result.status = JobStatus.COMPLETING;

		return result;
	}

	@Override
	public void ws_cleanup() throws Exception
	{
		if(!TOPALi.debugJobs)
			ClusterUtils.emptyDirectory(jobDir, true);
		result.status = JobStatus.COMPLETED;

		LocalJobs.delJob(result.jobId);
	}

	@Override
	public void ws_cancelJob()
	{
		LocalJobs.cancelJob(result.jobId);
	}
	
}
