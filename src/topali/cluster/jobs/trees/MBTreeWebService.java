// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.trees;

import java.io.File;
import java.util.logging.Level;

import org.apache.axis.AxisFault;

import topali.cluster.*;
import topali.data.MBTreeResult;
import topali.data.SequenceSet;
import topali.fileio.Castor;

public class MBTreeWebService extends WebService
{
	public String submit(String alignmentXML, String resultXML)
			throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);

			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			MBTreeResult result = (MBTreeResult) Castor.unmarshall(resultXML);

			result.mbPath = webappPath + "/binaries/src/mrbayes/mb";
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;

			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunMBTree runMBTree = new RunMBTree(jobDir, ss, result);
			runMBTree.start();

			accessLog.info("MBT request from " + jobId);
			logger.info(jobId + " - MBT request received");
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
			return new CollateMBTree(jobDir).getPercentageComplete();
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

			MBTreeResult result = new CollateMBTree(jobDir).getResult();

			logger.info(jobId + " - returning result");
			return Castor.getXML(result);
		} catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
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
		String template = ClusterUtils.readFile(new File(scriptsDir, "mbt.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());

		// Write...
		writeFile(template, new File(jobDir, "mbt.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("mbt.sh", jobDir);
	}
}