// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.control;

import java.io.*;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.ggf.drmaa.*;

import topali.cluster.*;

public class DrmaaClient implements ICluster
{
	private static Logger logger = Logger.getLogger("topali.cluster.sge");

	private static Session session = null;

	private String jobId;

	private int arrayStart, arrayEnd, arrayStep;

	private boolean isArrayJob = false;

	// "Everything that you as a programmer will do with DRMAA, you will do
	// through a Session object..."
	private void initSession() throws DrmaaException
	{
		if (session == null)
		{
			SessionFactory factory = SessionFactory.getFactory();
			session = factory.getSession();
			session.init(null);

			// Ensures that the DRMAA session is closed, even if the VM is
			// terminated forcefully.
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						session.exit();
					} catch (Exception e)
					{
					}
				}
			}));
		}
	}

	// Reads the jobId from the .SGE_ID file stored in the job's dir which will
	// be stored as either [id] or [id].[otherdatanotneeded]
	private void getJobId(File jobDir) throws Exception
	{
		String id = ClusterUtils.readFile(new File(jobDir, ".SGE_ID"));
		id = id.trim();

		// A simple job
		if (id.indexOf(".") == -1)
			jobId = id;
		// An array job
		else
		{
			StringTokenizer st = new StringTokenizer(id, ".-: ");
			isArrayJob = true;

			jobId = st.nextToken();
			arrayStart = Integer.parseInt(st.nextToken());
			arrayEnd = Integer.parseInt(st.nextToken());
			arrayStep = Integer.parseInt(st.nextToken());
		}
	}

	public void submitJob(File jobDir, String scriptName) throws Exception
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

			BufferedWriter out = new BufferedWriter(new FileWriter(new File(
					jobDir, ".SGE_ID")));
			out.write(jobId);
			out.close();

			// Clean up
			session.deleteJobTemplate(jt);
			// session.exit();

			logger.info("submitted job via DRMAA: " + jobId);
		} catch (IOException e)
		{
			logger.info("Unable to start job: " + e);
			throw e;
		} catch (DrmaaException e)
		{
			logger.info("Unable to start job: " + e);
			throw e;
		}
	}

	public int getJobStatus(File jobDir)
	{
		int status = JobStatus.UNKNOWN;

		try
		{
			initSession();
			getJobId(jobDir);

			// Simple case: we just need to ask about the jobId
			if (isArrayJob == false)
				return getStatus(jobId);
			// Complicated array job case: we need to ask about every job that
			// the array can run jobId.[nS] to jobId.[nE]
			else
			{
				for (int i = arrayStart; i <= arrayEnd; i += arrayStep)
				{
					int jobStatus = getStatus(jobId + "." + i);
					// The status codes are incremental, so a worse status will
					// override an ok one, setting the overall status to the job
					// to that of its worst sub task
					if (jobStatus > status)
						status = jobStatus;
				}

				return status;
			}
		} catch (Exception e)
		{
			logger.info("can't determine status (" + jobId + "): " + e);
			return JobStatus.UNKNOWN;
		}
	}

	private int getStatus(String id)
	{
		try
		{
			// Query DRMAA for the job's current status
			switch (session.getJobProgramStatus(id))
			{
			case Session.UNDETERMINED:
				return JobStatus.UNKNOWN;

			case Session.QUEUED_ACTIVE:
				return JobStatus.QUEUING;

			case Session.SYSTEM_ON_HOLD:
			case Session.USER_ON_HOLD:
			case Session.USER_SYSTEM_ON_HOLD:
				// TODO: split these two into a new group?
			case Session.SYSTEM_SUSPENDED:
			case Session.USER_SUSPENDED:
				return JobStatus.HOLDING;

			case Session.RUNNING:
				return JobStatus.RUNNING;

				// TODO: Throw an Exception instead? Or write an error.txt file?
			case Session.FAILED:
				return JobStatus.FATAL_ERROR;

			default:
				return JobStatus.UNKNOWN;
			}
		} catch (Exception e)
		{
			// logger.info("can't determine status (" + id + "): " + e);
			return JobStatus.UNKNOWN;
		}
	}

	public void deleteJob(File jobDir)
	{
		try
		{
			getJobId(jobDir);

			initSession();

		} catch (Exception e)
		{
			logger.info("unable to delete job: " + e);
		}
	}
	
	public int getQueueCount(File jobDir)
		throws Exception
	{
		return -1;
	}

	public static void main(String[] args)
	{
		DrmaaClient client = new DrmaaClient();

		System.out.println(client.getJobStatus(new File(args[0])));

		/*
		 * try { SessionFactory factory = SessionFactory.getFactory(); Session
		 * session = factory.getSession(); session.init(null);
		 * 
		 * JobTemplate jt = session.createJobTemplate();
		 * jt.setRemoteCommand("sleep.sh");
		 * jt.setWorkingDirectory("/home/tomcat/");
		 * 
		 * java.util.List list = jt.getAttributeNames();
		 * 
		 * System.out.println("Attribute names"); for (Object o: list) {
		 * System.out.println(" " + o); }
		 * 
		 *  // Submit it (and store the job_id) String jobId =
		 * session.runJob(jt); System.out.println("Submitted job: " + jobId);
		 *  // Clean up session.deleteJobTemplate(jt); session.exit(); } catch
		 * (Exception e) { e.printStackTrace(System.out); }
		 */
	}
}