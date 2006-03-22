package topali.cluster.trees;

import java.io.*;
import javax.servlet.http.*;

import org.apache.axis.*;
import org.apache.axis.transport.http.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

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
			
			result.mbPath = getParameter("mb-path");
			result.tmpDir = getParameter("tmp-dir");
			result.jobId = jobId;
				
			// We put the starting of the job into its own thread so the web
			// service can return as soon as possible
			RunMBTree runMBTree = new RunMBTree(jobDir, ss, result);
			runMBTree.start();
			
			accessLog.info("MBT request from " + jobId);
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
			return new CollateMBTree(jobDir).getPercentageComplete();
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
			
			MBTreeResult result = new CollateMBTree(jobDir).getResult();
			return Castor.getXML(result);
		}
		catch (Exception e)
		{
			throw AxisFault.makeFault(e);
		}
	}

	/*
	 * Creates the script that each instance of a job running on the cluster
	 * calls to execute that job. In this case, a java command to run an
	 * MBTreeAnalysis on a given directory.
	 */
	static void runScript(File jobDir)
		throws Exception
	{
		// Read...
		String template = ClusterUtils.readFile(new File(scriptsDir, "mbt.sh"));
		
		// Replace...
		template = template.replaceAll("\\$JAVA", javaPath);
		template = template.replaceAll("\\$TOPALi", topaliPath);
		template = template.replaceAll("\\$JOB_DIR", jobDir.getPath());
		
		// Add header...
		template = ClusterUtils.readFile(new File(scriptsHdr)) + template;
		
		// Write...
		writeFile(template, new File(jobDir, "mbt.sh"));
		
		// Run...
		submitJob("mbt.sh", jobDir);
	}
}