package topali.cluster.jobs;

import java.io.*;

import topali.cluster.*;
import topali.cluster.dss.*;
import topali.data.*;
import topali.gui.*;

public class DSSLocalJob extends AnalysisJob
{
	private SequenceSet ss;
	private File jobDir;
		
	public DSSLocalJob(DSSResult result, AlignmentData data)
	{
		this.result = result;
		this.data = data;
		this.ss = data.getSequenceSet();
		result.startTime = System.currentTimeMillis();
		result.jobId = "" + System.currentTimeMillis();
		result.tmpDir = Prefs.tmpDir.getPath();
		
		jobDir = new File(Prefs.tmpDir, result.jobId);
	}
	
	public String ws_submitJob()
		throws Exception
	{
		try
		{
			RunDSS dss = new RunDSS(jobDir, ss, (DSSResult)result);
			dss.start();
			
			result.status = JobStatus.RUNNING;			
			return result.jobId;
		}
		catch (Exception e)
		{
			System.out.println(e);
			result.status = JobStatus.FATAL_ERROR;
			throw e;
		}
	}
	
	public float ws_getProgress()
		throws Exception
	{
		return new CollateDSS(jobDir).getPercentageComplete();
	}
	
	public AnalysisResult ws_downloadResult()
		throws Exception
	{
		result = new CollateDSS(jobDir).getResult();
		result.status = JobStatus.COMPLETING;
		
		return result;
	}
	
	public void ws_cleanup()
		throws Exception
	{
		ClusterUtils.emptyDirectory(jobDir, true);
		result.status = JobStatus.COMPLETED;
	}
	
	public void ws_cancelJob()
	{
	}
}