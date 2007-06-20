// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import javax.xml.namespace.QName;

import org.apache.axis.client.*;
import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.data.AnalysisResult;
import topali.gui.Prefs;

public abstract class RemoteJob extends AnalysisJob
{
	Logger log = Logger.getLogger(this.getClass());
	
	protected Call call = null;

	protected String url = null;

	protected String serviceName = null;

	public RemoteJob()
	{
	}

	public RemoteJob(String serviceName, AnalysisResult result)
	{
		this.serviceName = serviceName;
		this.result = result;

		// Set the URL to its current value (which will be null for a new job,
		// but a real URL for a job that's been in progress before)
		log.info("RemoteJob: setting URL to " + result.url);
		url = result.url;
	}

	protected void determineClusterURL() throws Exception
	{
		if (Prefs.web_use_rbroker)
		{
			// First form of the URL...this points to the ResourceBroker service
			url = Prefs.web_broker_url + "/services/resource-broker";

			getCall();
			call.setOperationName(new QName("ResourceBroker", "queryForResource"));

			// The URL is now set to the return from this call - which will be
			// the actual server we run the job on
			String broker = (String) call.invoke(new Object[] {});

			// Append what we need for a TOPALi job onto the server's URL
			url = broker + "/services/" + serviceName;
			result.url = url;

			// Reset the call object
			call = null;
		}
		else
		{
			url = Prefs.web_direct_url + "/services/" + serviceName;
			result.url = url;
		}

		log.info("RemoteJob: actual URL is now " + result.url);
	}

	protected Call getCall() throws Exception
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
				// call.setMaintainSession(true);
				call.setTimeout(60000);

				// Compress request (if possible)
				// call.setProperty(HTTPConstants.MC_GZIP_REQUEST,
				// Boolean.TRUE);
				// Compress response (if possible)
				// call.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);
			}
			catch (Exception exception)
			{
				call = null;
				throw exception;
			}
		}

		return call;
	}

	public void ws_cleanup() throws Exception
	{
		call = getCall();
		call.setOperationName(new QName(serviceName, "cleanup"));
		call.invoke(new Object[] { result.jobId });

		result.status = JobStatus.COMPLETED;
	}

	public void ws_cancelJob() throws Exception
	{
		call = getCall();
		call.setOperationName(new QName(serviceName, "deleteJob"));
		call.invoke(new Object[] { result.jobId });

		result.status = JobStatus.CANCELLED;
	}
}