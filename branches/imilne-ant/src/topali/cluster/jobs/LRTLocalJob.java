// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.lrt.*;
import topali.data.*;
import topali.gui.Prefs;

public class LRTLocalJob extends AnalysisJob
{
	private SequenceSet ss;

	private File jobDir;

	public LRTLocalJob(LRTResult result, AlignmentData data)
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

	public String ws_submitJob() throws Exception
	{
		try
		{
			RunLRT lrt = new RunLRT(jobDir, ss, (LRTResult) result);
			lrt.start();

			result.status = JobStatus.RUNNING;
			return result.jobId;
		} catch (Exception e)
		{
			System.out.println(e);
			result.status = JobStatus.FATAL_ERROR;
			throw e;
		}
	}

	public JobStatus ws_getProgress() throws Exception
	{
		return new CollateLRT(jobDir).getPercentageComplete();
	}

	public AnalysisResult ws_downloadResult() throws Exception
	{
		result = new CollateLRT(jobDir).getResult();
		result.status = JobStatus.COMPLETING;

		return result;
	}

	public void ws_cleanup() throws Exception
	{
		ClusterUtils.emptyDirectory(jobDir, true);
		result.status = JobStatus.COMPLETED;

		LocalJobs.delJob(result.jobId);
	}

	public void ws_cancelJob()
	{
		LocalJobs.cancelJob(result.jobId);
	}
}