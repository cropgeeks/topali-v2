// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.hmm;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;
import org.apache.log4j.*;

import org.apache.axis.AxisFault;

import topali.cluster.*;
import topali.data.HMMResult;
import topali.data.SequenceSet;
import topali.fileio.Castor;

public class HMMWebService extends WebService
{
	public String submit(String alignmentXML, String resultXML)
			throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);

			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			try
			{
				checkJob(ss);
			} catch (RejectedExecutionException e)
			{
				throw AxisFault.makeFault(e);
			}
			
			HMMResult result = (HMMResult) Castor.unmarshall(resultXML);

			result.barcePath = webappPath + "/binaries/src/barce/barce";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunHMM hmm = new RunHMM(jobDir, ss, result);
			hmm.start();

			accessLog.info("HMM request from " + jobId);
			logger.info(jobId + " - HMM request received");
			return jobId;
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	protected JobStatus getPercentageComplete(File jobDir) throws AxisFault
	{
		try
		{
			return new CollateHMM(jobDir).getPercentageComplete();
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(new Exception("getPercentageComplete: "
					+ e));
		}
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			HMMResult result = new CollateHMM(jobDir).getResult();

			logger.info(jobId + " - returning result");
			accessLog.info("HMM result  to   " + jobId);
			return Castor.getXML(result);
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run an
	 * HMMAnalysis on a given directory.
	 */
	static void runScript(File jobDir) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "hmm.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "hmm.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("hmm.sh", jobDir);
	}
}