// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.raxml;

import java.io.File;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Level;

import topali.cluster.*;
import topali.cluster.jobs.modeltest.*;
import topali.data.*;
import topali.fileio.Castor;

public class RaxmlWebService extends WebService
{
	RaxmlMonitor monitor;
	
	public String submit(String alignmentXML, String resultXML) throws AxisFault {
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
			
			RaxmlResult result = (RaxmlResult) Castor.unmarshall(resultXML);

			if(ClusterUtils.isWindows) {
				result.raxmlPath = binPath + "\\raxml.exe";
			}
			else {
				result.raxmlPath = binPath + "/src/raxml/raxmlHPC";
				//phyml's sometimes not executable
				Runtime.getRuntime().exec("chmod +x " + result.raxmlPath);
			}
			
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;
			
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RaxmlInitializer init = new RaxmlInitializer(jobDir, ss, result);
			init.start();
			
			accessLog.info("RaxML request from " + jobId);
			logger.info(jobId + " - RaxML request received");
			return jobId;
		} catch (Exception e)
		{
			logger.log(Level.ERROR, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		} 
	}
	
	@Override
	protected JobStatus getPercentageComplete(File jobDir) throws AxisFault
	{
		try
		{
			if(monitor==null)
				monitor = new RaxmlMonitor(jobDir);
			
			return monitor.getPercentageComplete();
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
			if(monitor==null) {
				File jobDir = new File(getParameter("job-dir"), jobId);
				monitor = new RaxmlMonitor(jobDir);
			}
			
			RaxmlResult result = monitor.getResult();

			logger.info(jobId + " -  returning result");
			accessLog.info("RT result  to   " + jobId);
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
	static void runScript(File jobDir, RaxmlResult result) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "raxml.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", ""+(result.bootstrap+1));

		// Write...
		writeFile(template, new File(jobDir, "raxml.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("raxml.sh", jobDir);
	}
	
}
