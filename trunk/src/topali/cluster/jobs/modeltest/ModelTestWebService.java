// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest;

import java.io.*;
import java.util.concurrent.RejectedExecutionException;

import org.apache.axis.AxisFault;
import org.apache.log4j.Level;

import topali.cluster.*;
import topali.cluster.jobs.dss.CollateDSS;
import topali.cluster.jobs.lrt.CollateLRT;
import topali.data.*;
import topali.fileio.Castor;

public class ModelTestWebService extends WebService
{

	ModelTestMonitor mon;
	
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
			
			ModelTestResult result = (ModelTestResult) Castor.unmarshall(resultXML);

			if(ClusterUtils.isWindows) {
				result.phymlPath = binPath + "\\phyml_win32.exe";
			}
			else {
				result.phymlPath = binPath + "/src/phyml/phyml_linux";
				//phyml's sometimes not executable
				Runtime.getRuntime().exec("chmod +x " + result.phymlPath);
			}
			
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;
			
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			ModelTestInitializer init = new ModelTestInitializer(jobDir, ss, result);
			init.start();

			accessLog.info("ModelTest request from " + jobId);
			logger.info(jobId + " - ModelTest request received");
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
			if(mon==null)
				mon =  new ModelTestMonitor(jobDir);
			return mon.getPercentageComplete();
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
			if(mon==null) {
				File jobDir = new File(getParameter("job-dir"), jobId);
				mon =  new ModelTestMonitor(jobDir);
			}
			
			ModelTestResult result = mon.getResult();

			logger.info(jobId + " -  returning result");
			accessLog.info("MT result  to   " + jobId);
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
	static void runScript(File jobDir, ModelTestResult result) throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "modeltest.sh"));

		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", ""+result.models.size());

		// Write...
		writeFile(template, new File(jobDir, "modeltest.sh"));

		// Run...
		logger.info(jobDir.getName() + " - submitting to cluster");
		submitJob("modeltest.sh", jobDir);
	}
}
