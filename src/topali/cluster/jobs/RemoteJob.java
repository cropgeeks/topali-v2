// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import javax.xml.namespace.QName;

import org.apache.axis.*;
import org.apache.axis.client.*;

import topali.cluster.*;
import topali.gui.*;

public abstract class RemoteJob extends AnalysisJob
{
	protected Call call = null;
	
	protected String url = null;
	protected String serviceName = null;

	public RemoteJob() {}

	public RemoteJob(String serviceName)
	{
		this.serviceName = serviceName;
		
		url = Prefs.web_topali_url + "/services/" + serviceName;
	}

	protected Call getCall()
		throws Exception
	{
		if (call == null)
		{
			// Catch any exceptions in here because we want the Call to stay
			// null unless it has been created ok
			try
			{
				Service service = new Service();
				call = (Call) service.createCall();
		
				call.setTargetEndpointAddress(new java.net.URL(url));
				call.setMaintainSession(true);
				call.setTimeout(60000);
			}
			catch (Exception exception)
			{
				call = null;
				throw exception;
			}
		}
				
		return call;
	}
	
	public void ws_cleanup()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName(serviceName, "cleanup"));
		call.invoke(new Object[] { result.jobId } );
		
		result.status = JobStatus.COMPLETED;
	}
	
	public void ws_cancelJob()
		throws Exception
	{
		call = getCall();
		call.setOperationName(new QName(serviceName, "deleteJob"));
		call.invoke(new Object[] { result.jobId } );
		
		result.status = JobStatus.CANCELLED;
	}
}