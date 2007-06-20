// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.mrbayes;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Level;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class MrBayesWebService extends WebService
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
			
			MBTreeResult result = (MBTreeResult) Castor.unmarshall(resultXML);

			result.mbPath = webappPath + "/binaries/src/mrbayes/mb";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunMrBayes runMBTree = new RunMrBayes(jobDir, ss, result);
			runMBTree.start();

			accessLog.info("MB  request from " + jobId);
			logger.info(jobId + " - MB request received");
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
			return new MrBayesMonitor(jobDir).getPercentageComplete();
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

			MBTreeResult result = new MrBayesMonitor(jobDir).getResult();

			logger.info(jobId + " - returning result");
			accessLog.info("MB  result  to   " + jobId);
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
		String template = ClusterUtils.readFile(new File(scriptsDir, "mb.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "mb.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("mb.sh", jobDir);
	}

}
