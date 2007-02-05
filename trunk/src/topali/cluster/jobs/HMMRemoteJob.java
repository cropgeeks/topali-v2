package topali.cluster.jobs;

import javax.xml.namespace.QName;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class HMMRemoteJob extends RemoteJob
{
	private SequenceSet ss;	
		
	public HMMRemoteJob(HMMResult result, AlignmentData data)
	{
		super("topali-hmm", result);
		
		this.data = data;
		this.ss = data.getSequenceSet();
		
		if (result.startTime == 0)
			result.startTime = System.currentTimeMillis();
	}
			
	public String ws_submitJob()
		throws Exception
	{
		determineClusterURL();
		
		call = getCall();			
		call.setOperationName(new QName("topali-hmm", "submit"));
		
		String alignmentXML = Castor.getXML(ss);
		String resultXML = Castor.getXML(result);
					
		result.jobId = (String) call.invoke(
			new Object[] { alignmentXML, resultXML } );
		
		System.out.println("Job in progress: " + result.jobId);

		result.status = JobStatus.QUEUING;
		return result.jobId;
	}
	
	public JobStatus ws_getProgress()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-hmm", "getPercentageComplete"));
		
		String statusXML = (String) call.invoke(new Object[] { result.jobId } );
		JobStatus status = (JobStatus) Castor.unmarshall(statusXML);
			
		result.status = status.status;
		return status;
	}
	
	public AnalysisResult ws_downloadResult()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-hmm", "getResult"));
		
		String resultXML = (String) call.invoke(new Object[] { result.jobId } );
		result = (HMMResult) Castor.unmarshall(resultXML);
		
		result.status = JobStatus.COMPLETING;
		return result;
	}
}