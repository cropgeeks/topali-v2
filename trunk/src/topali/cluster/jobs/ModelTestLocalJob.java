// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import java.io.File;

import topali.cluster.*;
import topali.cluster.jobs.modeltest.*;
import topali.data.*;
import topali.gui.*;
import topali.var.Utils;

public class ModelTestLocalJob extends AnalysisJob
{

	private SequenceSet ss;
	public File jobDir;
	
	ModelTestMonitor mon;
	
	public ModelTestLocalJob(ModelTestResult result, AlignmentData data) {
		this.result = result;    
		this.data = data;
		this.ss = data.getSequenceSet();
		result.startTime = System.currentTimeMillis();
		result.jobId = "" + System.currentTimeMillis();
		result.tmpDir = Prefs.tmpDir.getPath();
		
		if (Prefs.isWindows)
			result.phymlPath = Utils.getLocalPath() + "phyml_win32.exe";
		else if(Prefs.isMacOSX)
			result.phymlPath = Utils.getLocalPath() + "phyml/phyml_macOSX";
		else 
			result.phymlPath = Utils.getLocalPath() + "phyml/phyml_linux";
		
		jobDir = new File(Prefs.tmpDir, result.jobId);
		
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
		if(mon==null)
			mon = new ModelTestMonitor(jobDir);
		
		result = mon.getResult();
		result.status = JobStatus.COMPLETING;
		return result;
	}

	@Override
	public JobStatus ws_getProgress() throws Exception
	{
		if(mon==null)
			mon = new ModelTestMonitor(jobDir);
		
		return mon.getPercentageComplete();
	}

	@Override
	public String ws_submitJob() throws Exception
	{
		try
		{
			ModelTestInitializer mt = new ModelTestInitializer(jobDir, ss, (ModelTestResult)result);
			mt.start();
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
