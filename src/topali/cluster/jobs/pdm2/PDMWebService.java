// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm2;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.Castor;

public class PDMWebService extends WebService
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

			PDM2Result result = (PDM2Result) Castor.unmarshall(resultXML);

			result.mbPath = binPath + "/src/mrbayes/mb";
			result.treeDistPath = binPath + "/src/treedist/treedist";
			result.tmpDir = getParameter("tmp-dir");
			result.nProcessors = Integer.parseInt(getParameter("n-processors"));
			result.jobId = jobId;

			Runtime.getRuntime().exec("chmod +x " + result.mbPath);
			Runtime.getRuntime().exec("chmod +x " + result.treeDistPath);

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			PDMInitializer pdm = new PDMInitializer(jobDir, ss, result);
			pdm.start();

			accessLog.info("PDM2 request from " + jobId);
			logger.info("PDM2 request from " + jobId);
			return jobId;
		} catch (Exception e)
		{
			logger.warn("" + e);
			throw AxisFault.makeFault(e);
		}
	}

	protected JobStatus getPercentageComplete(File jobDir) throws AxisFault
	{
		try
		{
			return new PDMMonitor(jobDir).getPercentageComplete();
		} catch (Exception e)
		{
			logger.warn("" + e);
			throw AxisFault.makeFault(e);
		}
	}

	public String getResult(String jobId) throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);

			PDM2Result result = new PDMMonitor(jobDir).getResult();

			logger.info("returning result for " + jobId);
			accessLog.info("PDM2 result to   " + jobId);
			return Castor.getXML(result);
		} catch (Exception e)
		{
			logger.warn("" + e);
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run an
	 * PDMAnalysis on a given directory.
	 */
	static void runScript(File jobDir, int partitions) throws Exception
	{
		// Read...
		String template = ClusterUtils
				.readFile(new File(scriptsDir, "pdm2.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", "" + partitions);

		// Write...
		writeFile(template, new File(jobDir, "pdm2.sh"));

		// Run...
		submitJob("pdm2.sh", jobDir);
	}
}