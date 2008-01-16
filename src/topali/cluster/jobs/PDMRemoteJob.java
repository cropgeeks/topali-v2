// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.fileio.Castor;
import topali.gui.Tracker;

public class PDMRemoteJob extends RemoteJob
{
	 Logger log = Logger.getLogger(this.getClass());

	private SequenceSet ss;

	public PDMRemoteJob(PDMResult result, AlignmentData data)
	{
		super("topali-pdm", result);

		this.data = data;
		this.ss = data.getSequenceSet();

		if (result.startTime == 0)
			result.startTime = System.currentTimeMillis();
	}

	@Override
	public String ws_submitJob() throws Exception
	{
		determineClusterURL("-parallel");

		call = getCall();
		call.setOperationName(new QName("topali-pdm", "submit"));

		String alignmentXML = Castor.getXML(ss);
		String resultXML = Castor.getXML(result);

		result.jobId = (String) call.invoke(new Object[]
		{ alignmentXML, resultXML });

		log.info("Job in progress: " + result.jobId);
		Tracker.log("SUBMITTED", "PDM", result.jobId, url);

		result.status = JobStatus.QUEUING;
		return result.jobId;
	}

	@Override
	public JobStatus ws_getProgress() throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-pdm", "getPercentageComplete"));

		String statusXML = (String) call.invoke(new Object[]
		{ result.jobId });
		JobStatus status = (JobStatus) Castor.unmarshall(statusXML);

		result.status = status.status;
		return status;
	}

	@Override
	public AnalysisResult ws_downloadResult() throws Exception
	{
		call = getCall();
		call.setOperationName(new QName("topali-pdm", "getResult"));

		String resultXML = (String) call.invoke(new Object[]
		{ result.jobId });
		result = (PDMResult) Castor.unmarshall(resultXML);

		result.status = JobStatus.COMPLETING;
		return result;
	}
}