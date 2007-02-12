// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;
import java.util.logging.Level;

import org.apache.axis.AxisFault;

import topali.cluster.*;
import topali.data.CodeMLResult;
import topali.data.SequenceSet;
import topali.fileio.Castor;

public class CodeMLWebService extends WebService
{
	public String submit(String alignmentXML, String resultXML)
			throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);

			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			CodeMLResult result = (CodeMLResult) Castor.unmarshall(resultXML);

			result.codemlPath = webappPath + "/binaries/src/codeml/codeml";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			new CodeMLInitializer(jobDir, ss, result).start();

			accessLog.info("CML request from " + jobId);
			logger.info(jobId + " - CML request received");
			return jobId;
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	protected JobStatus getPercentageComplete(File jobDir) throws AxisFault
	{
		try
		{
			return new CodeMLMonitor(jobDir).getPercentageComplete();
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			CodeMLResult result = new CodeMLMonitor(jobDir).getResult();

			logger.info(jobId + " -  returning result");
			return Castor.getXML(result);
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run a
	 * CodeMLAnalysis on a given directory.
	 */
	static void runScript(File jobDir) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "cml.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "cml.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("cml.sh", jobDir);
	}
}