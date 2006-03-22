package topali.cluster.control;

import java.io.*;
import java.util.logging.*;

import org.ggf.drmaa.*;

import topali.cluster.*;

public class DrmaaClient implements ICluster
{
	private static Logger logger = Logger.getLogger("topali.cluster.sge");
	
	private static Session session = null;
	
	
	// "Everything that you as a programmer will do with DRMAA, you will do
	// through a Session object..."
	private void initSession()
		throws DrmaaException
	{
		if (session == null)
		{
			SessionFactory factory = SessionFactory.getFactory();
			session = factory.getSession();
			session.init(null);
			
			// Ensures that the DRMAA session is closed, even if the VM is
			// terminated forcefully.
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run()
				{
					try { session.exit(); }
					catch (Exception e) {}
				}
			}));
		}		
	}
	
	public void submitJob(File jobDir, String scriptName)
		throws Exception
	{
		try
		{
			initSession();
			
			// Create the JobTemplate that will be used to submit the job
			JobTemplate jt = session.createJobTemplate();
			jt.setRemoteCommand(scriptName);
			jt.setWorkingDirectory(jobDir.getPath());
			
			logger.info("remote command set to: " + scriptName);
			logger.info("working directory is:  " + jobDir.getPath());
			
			// Submit it (and store the job_id)
			String jobId = session.runJob(jt);
			
			BufferedWriter out = new BufferedWriter(
				new FileWriter(new File(jobDir, ".SGE_ID")));
			out.write(jobId);
			out.close();
			
			// Clean up
			session.deleteJobTemplate(jt);
//			session.exit();
			
			logger.info("submitted job via DRMAA: " + jobId);
		}
		catch (IOException e)
		{
			logger.info("Unable to start job: " + e);
			throw e;
		}
		catch (DrmaaException e)
		{
			logger.info("Unable to start job: " + e);
			throw e;
		}
	}
	
	public int getJobStatus(File jobDir)
	{
		try
		{
			// Read the jobId from the .SGE_ID file stored in the job's dir
			String jobId = ClusterUtils.readFile(new File(jobDir, ".SGE_ID"));
			int status = JobStatus.UNKNOWN;
			
			// Query DRMAA for the job's current status
			initSession();
			switch (session.getJobProgramStatus(jobId))
			{
				case Session.UNDETERMINED:
					status = JobStatus.UNKNOWN;
					break;
				
				case Session.QUEUED_ACTIVE:
					status = JobStatus.QUEUING;
					break;
				
				case Session.SYSTEM_ON_HOLD:
				case Session.USER_ON_HOLD:
				case Session.USER_SYSTEM_ON_HOLD:
				// TODO: split these two into a new group?
				case Session.SYSTEM_SUSPENDED:
				case Session.USER_SUSPENDED:
					status = JobStatus.HOLDING;
					break;
				
				case Session.RUNNING:
					status = JobStatus.RUNNING;
					break;
				
				// TODO: Throw an Exception instead? Or write an error.txt file?
				case Session.FAILED:
					status = JobStatus.FATAL_ERROR;
					break;
				
				default: status = JobStatus.UNKNOWN;
			}
			
//			session.exit();
			
//			logger.info("returning SGE status of " + status);
			return status;
		}
		catch (Exception e)
		{
			logger.info("can't determine status: " + e);
			return JobStatus.UNKNOWN;
		}
	}
	
	public void deleteJob(File jobDir)
	{
		try
		{
			// Read the jobId from the .SGE_ID file stored in the job's dir
			String jobId = ClusterUtils.readFile(new File(jobDir, ".SGE_ID"));
			
			initSession();
			
			
		}
		catch (Exception e)
		{
			logger.info("unable to delete job: " + e);
		}
	}
	
	public static void main(String[] args)
	{
		try
		{
			SessionFactory factory = SessionFactory.getFactory();
			Session session = factory.getSession();
			session.init(null);
			
			JobTemplate jt = session.createJobTemplate();
			jt.setRemoteCommand("sleep.sh");
			jt.setWorkingDirectory("/home/tomcat/");
			
			java.util.List list = jt.getAttributeNames();
			
			System.out.println("Attribute names");
			for (Object o: list)
			{
				System.out.println("  " + o);
			}
			
			
			// Submit it (and store the job_id)
			String jobId = session.runJob(jt);
			System.out.println("Submitted job: " + jobId);
			
			// Clean up
			session.deleteJobTemplate(jt);
			session.exit();
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}