// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.dss;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Level;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class DSSWebService extends WebService
{
	public String submit(String alignmentXML, String resultXML)
			throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);

			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			DSSResult result = (DSSResult) Castor.unmarshall(resultXML);

			try
			{
				checkJob(ss);

				if (result.runs > 501)
					throw new RejectedExecutionException("The maximum number of "
						+ "bootstrap runs for a remote DSS job is limited to 500");
			}
			catch (RejectedExecutionException e)
			{
				throw AxisFault.makeFault(e);
			}

			result.fitchPath = binPath + "/src/fitch/fitch";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			Runtime.getRuntime().exec("chmod +x " + result.fitchPath);

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			DSSInitializer dss = new DSSInitializer(jobDir, ss, result);
			dss.start();

			accessLog.info("DSS request from " + jobId);
			logger.info(jobId + " - DSS request received");
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
			return new DSSMonitor(jobDir).getPercentageComplete();
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			DSSResult result = new DSSMonitor(jobDir).getResult();

			logger.info(jobId + " -  returning result");
			accessLog.info("DSS result  to   " + jobId);
			return Castor.getXML(result);
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run a
	 * DSSAnalysis on a given directory.
	 */
	static void runScript(File jobDir, DSSResult result) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "dss.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", "" + result.runs);

		// Write...
		writeFile(template, new File(jobDir, "dss.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("dss.sh", jobDir);
	}
}