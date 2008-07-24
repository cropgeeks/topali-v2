// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.lrt;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Level;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class LRTWebService extends WebService
{
	public String submit(String alignmentXML, String resultXML)
			throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);

			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			LRTResult result = (LRTResult) Castor.unmarshall(resultXML);

			try
			{
				checkJob(ss);

				if (result.runs > 501)
					throw new RejectedExecutionException("The maximum number of "
						+ "bootstrap runs for a remote LRT job is limited to 500");

			}
			catch (RejectedExecutionException e)
			{
				throw AxisFault.makeFault(e);
			}

			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			LRTInitializer lrt = new LRTInitializer(jobDir, ss, result);
			lrt.start();

			accessLog.info("LRT request from " + jobId);
			logger.info(jobId + " - LRT request received");
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
			return new LRTMonitor(jobDir).getPercentageComplete();
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

			LRTResult result = new LRTMonitor(jobDir).getResult();

			logger.info(jobId + " - returning result");
			accessLog.info("LRT result  to   " + jobId);
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
	 * LRTAnalysis on a given directory.
	 */
	static void runScript(File jobDir, LRTResult result) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "lrt.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", "" + result.runs);

		// Write...
		writeFile(template, new File(jobDir, "lrt.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("lrt.sh", jobDir);
	}
}