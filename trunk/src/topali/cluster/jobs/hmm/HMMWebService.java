package topali.cluster.jobs.hmm;

import java.io.*;

import org.apache.axis.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class HMMWebService extends WebService
{	
	public String submit(String alignmentXML, String resultXML)
		throws AxisFault
	{
		try
		{
			String jobId = getJobId();
			File jobDir = new File(getParameter("job-dir"), jobId);
						
			SequenceSet ss = (SequenceSet) Castor.unmarshall(alignmentXML);
			HMMResult result = (HMMResult) Castor.unmarshall(resultXML);
			
			result.barcePath = getParameter("barce-path");
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;
				
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunHMM hmm = new RunHMM(jobDir, ss, result);
			hmm.start();
			
			accessLog.info("HMM request from " + jobId);
			logger.info("HMM request from " + jobId);
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
			return new CollateHMM(jobDir).getPercentageComplete();
		}
		catch (Exception e)
		{
			logger.warning(""+e);
			throw AxisFault.makeFault(new Exception("getPercentageComplete: " + e));
		}
	}
	
	public String getResult(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			HMMResult result = new CollateHMM(jobDir).getResult();
			
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
	 * HMMAnalysis on a given directory.
	 */
	static void runScript(File jobDir)
		throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "hmm.sh"));
		
		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		
		// Write...
		writeFile(template, new File(jobDir, "hmm.sh"));
		
		// Run...
		submitJob("hmm.sh", jobDir);
	}
}