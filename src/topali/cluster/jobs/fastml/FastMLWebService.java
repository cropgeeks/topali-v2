// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class FastMLWebService extends WebService
{

	Logger log = Logger.getLogger(this.getClass());

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

			FastMLResult result = (FastMLResult) Castor.unmarshall(resultXML);

			result.fastmlPath = webappPath + "/binaries/src/fastml/fastml";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			Runtime.getRuntime().exec("chmod +x " + result.fastmlPath);

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunFastML run = new RunFastML(jobDir, ss, result);
			run.start();

			accessLog.info("FastML request from " + jobId);
			logger.info(jobId + " - FastML request received");
			return jobId;
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	protected JobStatus getPercentageComplete(File jobDir) throws AxisFault
	{
		if (new File(jobDir, "error.txt").exists())
		{
			logger.log(Level.ERROR, "error.txt found");
			throw AxisFault.makeFault(new Exception("FastML error.txt"));
		}

		if(new File(jobDir, "result.xml").exists())
			return new JobStatus(100, 0, "_status");
		else
			return new JobStatus(0, 0, "_status");
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			FastMLResult result =  (FastMLResult)Castor.unmarshall(new File(jobDir, "result.xml"));

			logger.info(jobId + " - returning result");
			accessLog.info("FastML  result  to   " + jobId);
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
		String template = ClusterUtils.readFile(new File(scriptsDir, "fastml.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "fastml.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("fastml.sh", jobDir);
	}


}
