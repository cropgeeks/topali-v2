package topali.cluster;

import java.io.*;
import javax.servlet.http.*;

import org.apache.axis.*;
import org.apache.axis.transport.http.*;

import topali.cluster.sge.*;
import topali.fileio.*;

/**
 * Base class for all TOPALi web services. Some of the PUBLIC methods used by
 * all the services are common enough to be defined here, but this class mainly
 * contains PRIVATE helper methods for the subclasses.
 */
public abstract class WebService
{
	protected static String javaPath, topaliPath;
	protected static String scriptsDir;	
	
	protected WebService()
	{
		javaPath = getParameter("java-path");
		topaliPath = getParameter("topali-path");
		
		scriptsDir = getParameter("scripts-dir");
	}
	
	protected String getJobId()
	{
		String remoteAddress = getHttpServletRequest().getRemoteAddr();
		return System.currentTimeMillis() + "." + remoteAddress;
	}

	// Returns the current HttpServletRequest object that can be used to query
	// information on the client-request that called this web service
	protected HttpServletRequest getHttpServletRequest()
	{
		MessageContext context = MessageContext.getCurrentContext();
		
		return (HttpServletRequest)
			context.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
	}
	
	// Reads and returns the value for the given parameter name (from web.xml)
	protected String getParameter(String name)
	{
		HttpServletRequest req = getHttpServletRequest();

		return req.getSession().getServletContext().getInitParameter(name);
	}
	
	protected static void writeFile(String str, File filename)
		throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		out.write(str);
		out.close();
	}
	
	protected static void submitJob(String cmd, File jobDir)
		throws Exception
	{
		SGEMonitor.submitJob(cmd, jobDir);
	}
	
	protected abstract float getPercentageComplete(File jobDir)
		throws AxisFault;
	
	//////////////////////////////////////////////////
	// Public access methods - the actual WEB SERVICES
	//////////////////////////////////////////////////
	
	public String getPercentageComplete(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			// Progress...
			float progress = getPercentageComplete(jobDir);
			int status = JobStatus.UNKNOWN;
			
			// Status (assuming Job is actually in the SGE queue)...
			SGEMonitor monitor = new SGEMonitor();			
			if (monitor.loadFile(jobDir))
			{
				status = monitor.getJobStatus();

				// TODO: Find out WTF qstat won't always return the state			
//				if (status == JobStatus.UNKNOWN && progress < 100f)
//					throw AxisFault.makeFault(new Exception("Unknown cluster job error"));
			}			
			
			return Castor.getXML(new JobStatus(progress, status));
		}
		catch (Exception e)
		{
			throw AxisFault.makeFault(e);
		}
	}
	
	public void deleteJob(final String jobId)
	{
		File jobDir = new File(getParameter("job-dir"), jobId);
		
		// TODO: SGE Job deletion error checking and security
		SGEMonitor monitor = new SGEMonitor(jobDir);		
		monitor.deleteJob();
		
		// Wait a minute before attempting to cleanup (to give SGE time to qdel)
		Runnable r = new Runnable() {
			public void run()
			{
				try { Thread.sleep(60000); }
				catch (Exception e) {}
				
				cleanup(jobId);
			}
		};
		
		new Thread(r).start();
	}
	
	public void cleanup(String jobId)
	{
		File jobDir = new File(getParameter("job-dir"), jobId);
		ClusterUtils.emptyDirectory(jobDir, true);
	}
}
