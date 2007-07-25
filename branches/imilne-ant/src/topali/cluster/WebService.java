// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.io.*;
import java.util.concurrent.RejectedExecutionException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.axis.*;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.log4j.*;

import sbrn.commons.file.FileUtils;
import topali.cluster.control.*;
import topali.data.SequenceSet;
import topali.fileio.Castor;

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
		HttpServletRequest req = getHttpServletRequest();
		ServletContext sc = req.getSession().getServletContext();

		// This defines a real path to where the webapp is located on disk
		webappPath = new File(sc.getRealPath("/"));

		// We then load the properties for this install
		File configPath = FileUtils.getFile(webappPath, "WEB-INF", "cluster");
		props = new WebXmlProperties(new File(configPath, "cluster.properties"));

		// And configure some other needed values as shortcuts for the
		// subclasses
		scriptsDir = configPath.getPath();
		javaPath = getParameter("java-path");
		topaliPath = FileUtils.getFile(webappPath, "WEB-INF", "lib",
				"topali.jar").getPath();

		// Initialize logging
		logger = Logger.getLogger("topali.cluster.info-log");
		accessLog = Logger.getLogger("topali.cluster.access-log");
		
		try
		{
			File logDir = new File(webappPath, "logs");
			File infoFile = new File(logDir, "info-log.txt");
			File accessFile = new File(logDir, "access-log.txt");

			PatternLayout pLayout = new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} - %m\r\n");
		
			logger.addAppender(new FileAppender(pLayout, infoFile.getPath()));
			accessLog.addAppender(new FileAppender(pLayout, accessFile.getPath()));
		}
		catch (Exception e) {}
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

		return (HttpServletRequest) context
				.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
	}

	// Reads and returns the value for the given parameter name
	protected String getParameter(String name)
	{
		return WebXmlProperties.getParameter(name);
	}

	protected static void writeFile(String str, File filename) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		out.write(str);
		out.close();
	}

	protected static void submitJob(String cmd, File jobDir) throws Exception
	{
		ICluster cluster = (false) ? new DrmaaClient() : new SgeClient();
		cluster.submitJob(jobDir, cmd);
	}
	
	/**
	 * Check if alignment is too big for a certain webservice
	 * (limits can be set in cluster.properties, default: no limit)
	 * @param ss
	 * @throws RejectedExecutionException
	 */
	protected void checkJob(SequenceSet ss) throws RejectedExecutionException {
		String tmp = getParameter(this.getClass().getSimpleName());
		if(tmp==null || tmp.equals(""))
			return;
		
		int max = Integer.parseInt(tmp);
		if(ss.getSelectedSequences().length>max) {
			String msg = "Max. alignment size for this job type is limited to "+max+" sequences.";
			throw new RejectedExecutionException(msg);
		}
	}

	// ////////////////////////////////////////////////
	// Public access methods - the actual WEB SERVICES
	// ////////////////////////////////////////////////

	protected abstract JobStatus getPercentageComplete(File jobDir)
			throws AxisFault;

	public String getPercentageComplete(String jobId) throws AxisFault
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
			
			if (js.status == JobStatus.QUEUING)
				js.text = "" + cluster.getQueueCount(jobDir);

			// TODO: Find out WTF qstat won't always return the state
			// TODO: Following not suitable for SGE 5.3
			// if (status == JobStatus.UNKNOWN && progress < 100f)
			// throw AxisFault.makeFault(new Exception("Unknown cluster job
			// error"));

			return Castor.getXML(js);
		} catch (Exception e)
		{
			// Exceptions thrown in here mean a job (or sub job) has failed. We
			// may as well cancel the entire job at this point and free up the
			// cluster
			File jobDir = new File(getParameter("job-dir"), jobId);
			ICluster cluster = (false) ? new DrmaaClient() : new SgeClient();
			cluster.deleteJob(jobDir);
			
			logger.log(Level.ERROR, e.getMessage(), e);
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
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					Thread.sleep(60000);
				} catch (Exception e)
				{
				}

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
