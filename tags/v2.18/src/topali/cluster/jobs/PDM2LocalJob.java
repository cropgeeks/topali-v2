// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.pdm2.*;
import topali.data.*;
import topali.gui.*;

public class PDM2LocalJob extends AnalysisJob
{
	private SequenceSet ss;

	private File jobDir;

	public PDM2LocalJob(PDM2Result result, AlignmentData data)
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
			PDMInitializer pdm = new PDMInitializer(jobDir, ss,
					(PDM2Result) result);
			pdm.start();

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
		return new PDMMonitor(jobDir).getPercentageComplete();
	}

	@Override
	public AnalysisResult ws_downloadResult() throws Exception
	{
		result = new PDMMonitor(jobDir).getResult();
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