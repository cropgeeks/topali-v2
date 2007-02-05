package topali.cluster;

import java.io.*;
import java.util.logging.*;
import javax.servlet.*;
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
	
	protected static File webappPath;
	protected static String javaPath, topaliPath;
	protected static String scriptsDir;
	
	static boolean DRMAA = false;
	
	protected WebService()
	{
		if (props == null)
			initializeProperties();
	}
	
	private void initializeProperties()
	{
		String fs = File.separator;
		
		HttpServletRequest req = getHttpServletRequest();
		ServletContext sc = req.getSession().getServletContext();
		
		// This defines a real path to where the webapp is located on disk
		webappPath = new File(sc.getRealPath("/"));
		
		// We then load the properties for this install
		File configPath = FileUtils.getFile(webappPath, "WEB-INF", "cluster");
		props = new WebXmlProperties(new File(configPath, "cluster.properties"));
		
		// And configure some other needed values as shortcuts for the subclasses
		scriptsDir = configPath.getPath();
		javaPath   = getParameter("java-path");
		topaliPath = FileUtils.getFile(webappPath, "WEB-INF", "lib", "topali.jar").getPath();		
		
		
		
		// Now that the properties are loaded, set the log FileHandler
		// TODO: Get all this into a logging.properties file
		
		// We set two loggers - one to log access only, and one for more detail
		FileHandler fh1 = null, fh2 = null;
		try
		{
			File logsDir = new File(webappPath, "logs");
			File fAccessLog = new File(logsDir, "access-log");
			File fInfoLog = new File(logsDir, "info-log");
			
			fh1 = new FileHandler(fAccessLog.getPath(), 0, 1, true);			
			fh2 = new FileHandler(fInfoLog.getPath(), 0, 1, true);
			fh1.setFormatter(new SimpleFormatter());
			fh2.setFormatter(new SimpleFormatter());
		}
		catch (IOException e) {}
		
		accessLog = Logger.getLogger("topali.cluster.access-log");
		logger = Logger.getLogger("topali.cluster.info-log");
				
		accessLog.addHandler(fh1);
		logger.addHandler(fh2);
		
		logger.info("Initializing properties");
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
	
	// Reads and returns the value for the given parameter name
	protected String getParameter(String name)
	{
		return props.getParameter(name);
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
	
	protected abstract JobStatus getPercentageComplete(File jobDir)
		throws AxisFault;
	
	public String getPercentageComplete(String jobId)
		throws AxisFault
	{
		try
		{
			File jobDir = new File(getParameter("job-dir"), jobId);
			
			// Progress...
			JobStatus js = getPercentageComplete(jobDir);
			logger.info(jobId + " - " + js.progress + "%");
			
			if (js.progress >= 100f)
				return Castor.getXML(new JobStatus(100, JobStatus.COMPLETING));
			
			// Status (assuming Job is actually in the SGE queue)...
			js.status = JobStatus.UNKNOWN;
			ICluster cluster = (DRMAA) ? new DrmaaClient() : new SgeClient();
			js.status = cluster.getJobStatus(jobDir);
			logger.info(jobId + " - current status = " + js.status);
			
			// TODO: Find out WTF qstat won't always return the state
			// TODO: Following not suitable for SGE 5.3		
//			if (status == JobStatus.UNKNOWN && progress < 100f)
//				throw AxisFault.makeFault(new Exception("Unknown cluster job error"));
			
			return Castor.getXML(js);
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
