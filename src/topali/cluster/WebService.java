package topali.cluster;

import java.io.*;
import java.util.logging.*;
import javax.servlet.http.*;

import org.apache.axis.*;
import org.apache.axis.transport.http.*;

import topali.cluster.control.*;
import topali.fileio.*;

import sbrn.commons.file.*;

/**
 * Base class for all TOPALi web services. Some of the PUBLIC methods used by
 * all the services are common enough to be defined here, but this class mainly
 * contains PRIVATE helper methods for the subclasses.
 */
public abstract class WebService
{
	protected static Logger accessLog;
	protected static Logger logger;
	
	protected static WebXmlProperties props;
	
	protected static String javaPath, topaliPath;
	protected static String scriptsDir, scriptsHdr;
	
	static boolean DRMAA = false;
	
	protected WebService()
	{
		if (props == null)
			initializeProperties();
		
		javaPath   = getParameter("java-path");
		topaliPath = getParameter("topali-path");
		
		scriptsDir = getParameter("scripts-dir");
		scriptsHdr = getParameter("scripts-hdr");
	}
	
	private void initializeProperties()
	{
		HttpServletRequest req = getHttpServletRequest();
		String filename =
			req.getSession().getServletContext().getInitParameter("props-file");
				
		props = new WebXmlProperties(filename);
			
		// Now that the properties are loaded, set the log FileHandler
		// TODO: Get all this into a logging.properties file
		
		// We set two loggers - one to log access only, and one for more detail
		FileHandler fh1 = null, fh2 = null;
		try
		{
			fh1 = new FileHandler(getParameter("access-log"), 0, 1, true);			
			fh2 = new FileHandler(getParameter("info-log"), 0, 1, true);
			fh1.setFormatter(new SimpleFormatter());
			fh2.setFormatter(new SimpleFormatter());
		}
		catch (IOException e) {}
		
		accessLog = Logger.getLogger("topali.cluster.access-log");
		logger = Logger.getLogger("topali.cluster.info-log");
				
		accessLog.addHandler(fh1);
		logger.addHandler(fh2);
	}
	
	protected String getJobId()
	{
		String remoteAddress = getHttpServletRequest().getRemoteAddr();
		
		return System.currentTimeMillis() + "-" + remoteAddress;
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
		return props.getParameter(name);
		
//		HttpServletRequest req = getHttpServletRequest();
//		return req.getSession().getServletContext().getInitParameter(name);
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
		ICluster cluster = (false) ? new DrmaaClient() : new SgeClient();
		cluster.submitJob(jobDir, cmd);
	}
	
	
	//////////////////////////////////////////////////
	// Public access methods - the actual WEB SERVICES
	//////////////////////////////////////////////////
	
	public String getServerStatus()
		throws AxisFault
	{
		try
		{
			File statusFile = new File(getParameter("status-file"));
			return FileUtils.readFile(statusFile);
		}
		catch (IOException e)
		{
			throw AxisFault.makeFault(e);
		}
	}
	
	protected abstract float getPercentageComplete(File jobDir)
		throws AxisFault;
	
	public String getPercentageComplete(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			// Progress...
			float progress = getPercentageComplete(jobDir);
			logger.info(jobId + " - " + progress + "%");
			
			if (progress >= 100f)
				return Castor.getXML(new JobStatus(100, JobStatus.COMPLETING));
			
			// Status (assuming Job is actually in the SGE queue)...
			int status = JobStatus.UNKNOWN;
			ICluster cluster = (DRMAA) ? new DrmaaClient() : new SgeClient();
			status = cluster.getJobStatus(jobDir);
			logger.info(jobId + " - current status = " + status);
			
			// TODO: Find out WTF qstat won't always return the state
			// TODO: Following not suitable for SGE 5.3		
//			if (status == JobStatus.UNKNOWN && progress < 100f)
//				throw AxisFault.makeFault(new Exception("Unknown cluster job error"));
			
			return Castor.getXML(new JobStatus(progress, status));
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw AxisFault.makeFault(e);
		}
	}
	
	public void deleteJob(final String jobId)
	{
		logger.info(jobId + " - delete job command received");
		
		File jobDir = new File(getParameter("job-dir"), jobId);
		
		// TODO: SGE Job deletion error checking and security
		ICluster cluster = (false) ? new DrmaaClient() : new SgeClient();
		cluster.deleteJob(jobDir);
		
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
		logger.info(jobId + " - cleaning up and removing files");
		
		File jobDir = new File(getParameter("job-dir"), jobId);
		ClusterUtils.emptyDirectory(jobDir, true);		
	}
}
