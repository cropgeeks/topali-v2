package topali.cluster.dss;

import java.io.*;
import org.apache.axis.*;
import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

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
			
			result.fitchPath = getParameter("fitch-path");
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;			
				
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunDSS dss = new RunDSS(jobDir, ss, result);
			dss.start();
			
			accessLog.info("DSS request from " + jobId);
			logger.info("DSS request from " + jobId);
			
			return jobId;
		}
		catch (Exception e)
		{
			logger.warning(""+e);
			throw AxisFault.makeFault(e);
		}
	}
	
	protected float getPercentageComplete(File jobDir)
		throws AxisFault
	{
		try
		{
			return new CollateDSS(jobDir).getPercentageComplete();
		}
		catch (Exception e)
		{
			logger.warning(""+e);
			throw AxisFault.makeFault(e);
		}
	}
	
	public String getResult(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			DSSResult result = new CollateDSS(jobDir).getResult();
			
			logger.info("returning result for " + jobId);
			return Castor.getXML(result);
		}
		catch (Exception e)
		{
			logger.warning(""+e);
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run a
	 * DSSAnalysis on a given directory.
	 */
	static void runScript(File jobDir, DSSResult result)
		throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "dss.sh"));
		
		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", "" + result.runs);
		
		// Add header...
		template = ClusterUtils.readFile(new File(scriptsHdr)) + template;
		
		// Write...
		writeFile(template, new File(jobDir, "dss.sh"));
		
		// Run...
		submitJob("dss.sh", jobDir);
	}
}