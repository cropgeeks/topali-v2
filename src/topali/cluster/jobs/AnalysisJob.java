package topali.cluster.jobs;

import topali.data.*;

public abstract class AnalysisJob
{	
	// The owning dataset that this job is running for
	protected AlignmentData data;
	
	// The result object that stores information on this job (and its results)
	protected AnalysisResult result;
	
	public String errorInfo;
	
	public String getJobId()
		{ return result.jobId; }
	
	public void setJobId(String jobId)
		{ result.jobId = jobId; }
			
	public int getStatus()
		{ return result.status; }
	
	public void setStatus(int status)
		{ result.status = status; }
	
	public AlignmentData getAlignmentData()
		{ return data; }
	
	// Returns whatever result object is currently held
	public AnalysisResult getResult()
		{ return result; }
	
	
	// These are the actual web services (or local) job control methods
	// All must be implemented by the actual child classes
	
	public abstract String ws_submitJob()
		throws Exception;
	
	public abstract float ws_getProgress()
		throws Exception;
	
	public abstract AnalysisResult ws_downloadResult()
		throws Exception;
		
	public abstract void ws_cleanup()
		throws Exception;	
	
	public abstract void ws_cancelJob()
		throws Exception;
}