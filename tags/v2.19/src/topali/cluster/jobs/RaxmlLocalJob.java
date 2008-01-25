// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.raxml.*;
import topali.data.*;
import topali.gui.TOPALi;
import topali.var.SysPrefs;
import topali.var.utils.Utils;

public class RaxmlLocalJob extends AnalysisJob
{

	private SequenceSet ss;
	public File jobDir;
	
	RaxmlMonitor monitor;
	
	public RaxmlLocalJob(RaxmlResult result, AlignmentData data) {
		this.result = result;    
		this.data = data;
		this.ss = data.getSequenceSet();
		result.startTime = System.currentTimeMillis();
		result.jobId = "" + System.currentTimeMillis();
		result.tmpDir = SysPrefs.tmpDir.getPath();
		
		if (SysPrefs.isWindows)
			result.raxmlPath = Utils.getLocalPath() + "raxml.exe";
		else 
			result.raxmlPath = Utils.getLocalPath() + "raxml/raxmlHPC";
		
		jobDir = new File(SysPrefs.tmpDir, result.jobId);
		
		LocalJobs.addJob(result.jobId);
	}
	
	@Override
	public void ws_cancelJob() throws Exception
	{
		LocalJobs.cancelJob(result.jobId);
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
	public AnalysisResult ws_downloadResult() throws Exception
	{
		if(monitor==null)
			monitor = new RaxmlMonitor(jobDir);
		
		result = monitor.getResult();
		result.status = JobStatus.COMPLETING;
		return result;
	}

	@Override
	public JobStatus ws_getProgress() throws Exception
	{
		if(monitor==null)
			monitor = new RaxmlMonitor(jobDir);
		
		return monitor.getPercentageComplete();
	}

	@Override
	public String ws_submitJob() throws Exception
	{
		try
		{
			RaxmlInitializer rx = new RaxmlInitializer(jobDir, ss, (RaxmlResult)result);
			rx.start();
			
			result.status = JobStatus.RUNNING;
			return result.jobId;
		} catch (RuntimeException e)
		{
			System.out.println(e);
			result.status = JobStatus.FATAL_ERROR;
			throw e;
		}
	}

	
}
