// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.codonw;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class CodonWWebService extends WebService
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

			CodonWResult result = (CodonWResult) Castor.unmarshall(resultXML);

			result.codonwPath = webappPath + "/WEB-INF/binaries/src/codonW/codonw";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			Runtime.getRuntime().exec("chmod +x " + result.codonwPath);

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunCodonW run = new RunCodonW(jobDir, ss, result);
			run.start();

			accessLog.info("CW  request from " + jobId);
			logger.info(jobId + " - CW request received");
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
			return (new CodonWMonitor(jobDir)).getPercentageComplete();
		} catch (Exception e)
		{
			logger.warn(e);
			return new JobStatus(0, JobStatus.FATAL_ERROR);
		}
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			CodonWResult result = (new CodonWMonitor(jobDir)).getResult();

			logger.info(jobId + " - returning result");
			accessLog.info("CW  result  to   " + jobId);
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
	 * MBTreeAnalysis on a given directory.
	 */
	static void runScript(File jobDir) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "cw.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "cw.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("cw.sh", jobDir);
	}

}
