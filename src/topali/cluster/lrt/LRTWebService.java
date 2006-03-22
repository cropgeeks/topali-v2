package topali.cluster.lrt;

import java.io.*;
import javax.servlet.http.*;

import org.apache.axis.*;
import org.apache.axis.transport.http.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

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
					
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;			
				
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunLRT lrt = new RunLRT(jobDir, ss, result);
			lrt.start();
			
			accessLog.info("LRT request from " + jobId);
			return jobId;
		}
		catch (Exception e)
		{
			throw AxisFault.makeFault(e);
		}
	}
	
	protected float getPercentageComplete(File jobDir)
		throws AxisFault
	{
		try
		{
			return new CollateLRT(jobDir).getPercentageComplete();
		}
		catch (Exception e)
		{
			throw AxisFault.makeFault(e);
		}
	}
	
	public String getResult(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			LRTResult result = new CollateLRT(jobDir).getResult();
			return Castor.getXML(result);
		}
		catch (Exception e)
		{
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run a
	 * LRTAnalysis on a given directory.
	 */
	static void runScript(File jobDir, LRTResult result)
		throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "lrt.sh"));
		
		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		template = template.replaceAll("\\$RUN_COUNT", "" + result.runs);
		
		// Add header...
		template = ClusterUtils.readFile(new File(scriptsHdr)) + template;
		
		// Write...
		writeFile(template, new File(jobDir, "lrt.sh"));
		
		// Run...
		submitJob("lrt.sh", jobDir);
	}
}