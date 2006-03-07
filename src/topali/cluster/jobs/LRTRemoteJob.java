package topali.cluster.jobs;

import java.io.*;
import java.net.*;
import javax.xml.namespace.QName;

import org.apache.axis.*;
import org.apache.axis.client.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.gui.*;

public class LRTRemoteJob extends RemoteJob
{
	private SequenceSet ss;	
	
	public LRTRemoteJob(LRTResult result, AlignmentData data)
	{
		super("topali-lrt");
		
		this.result = result;
		this.data = data;
		this.ss = data.getSequenceSet();
		
		if (result.startTime == 0)
			result.startTime = System.currentTimeMillis();
	}
			
	public String ws_submitJob()
		throws Exception
	{
		call = getCall();			
		call.setOperationName(new QName("topali-lrt", "submit"));
		
		String alignmentXML = Castor.getXML(ss);
		String resultXML = Castor.getXML(result);
					
		result.jobId = (String) call.invoke(
			new Object[] { alignmentXML, resultXML } );
		
		System.out.println("Job in progress: " + result.jobId);

		result.status = JobStatus.QUEUING;
		return result.jobId;
	}
	
	public float ws_getProgress()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-lrt", "getPercentageComplete"));
			
		String statusXML = (String) call.invoke(new Object[] { result.jobId } );
		JobStatus status = (JobStatus) Castor.unmarshall(statusXML);
			
		result.status = status.status;
		return status.progress;
	}
	
	public AnalysisResult ws_downloadResult()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-lrt", "getResult"));
		
		String resultXML = (String) call.invoke(new Object[] { result.jobId } );
		result = (LRTResult) Castor.unmarshall(resultXML);
		
		result.status = JobStatus.COMPLETING;
		return result;
	}
}