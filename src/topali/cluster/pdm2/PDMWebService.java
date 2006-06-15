package topali.cluster.pdm2;

import java.io.*;

import org.apache.axis.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

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
			PDM2Result result = (PDM2Result) Castor.unmarshall(resultXML);
			
			result.mbPath = getParameter("mb-path");
			result.treeDistPath = getParameter("treedist-path");
			result.tmpDir = getParameter("tmp-dir");
			result.nProcessors = Integer.parseInt(getParameter("n-processors"));
			result.jobId = jobId;
				
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			PDMInitializer pdm = new PDMInitializer(jobDir, ss, result);
			pdm.start();
			
			accessLog.info("PDM2 request from " + jobId);
			logger.info("PDM2 request from " + jobId);
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
			return new PDMMonitor(jobDir).getPercentageComplete();
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
			
			PDM2Result result = new PDMMonitor(jobDir).getResult();
			
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
	 * calls to execute that job. In this case, a java command to run an
	 * PDMAnalysis on a given directory.
	 * Also modified to run PDM2 script "A" or "B" depending on call
	 */
	static void runScript(File jobDir, int partitions, boolean scriptA)
		throws Exception
	{
		String name = scriptA ? "pdm2_a.sh" : "pdm2_b.sh";
		
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, name));
		
		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		if (scriptA)
			template = template.replaceAll("\\$RUN_COUNT", "" + partitions);
		
		// Write...
		writeFile(template, new File(jobDir, name));
		
		// Run...
		submitJob(name, jobDir);
	}
}