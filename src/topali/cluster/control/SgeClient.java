// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.control;

import java.io.*;
import java.util.logging.*;

import topali.cluster.*;

public class SgeClient implements ICluster
{
	private static Logger logger = Logger.getLogger("topali.cluster.control");
	
	private String sge_job_id;
	
	// Reads the jobId from the .SGE_ID file stored in the job's dir which will
	// be stored as either [id] or [id].[otherdatanotneeded]
	private void getJobId(File jobDir)
		throws Exception
	{
		String jobId = ClusterUtils.readFile(new File(jobDir, ".SGE_ID"));
		
		if (jobId.indexOf(".") == -1)
			sge_job_id = jobId;
		else
			sge_job_id = jobId.substring(0, jobId.indexOf("."));
		
		sge_job_id = sge_job_id.trim();
		logger.info("jobID: #" + sge_job_id + "#");
	}
	
	/*
	 * Submits (qsub) command and reads the return from qsub so that a .SGE_ID
	 * file can be placed in the job's directory BEFORE the job begins.
	 */
	public void submitJob(File jobDir, String scriptName)
		throws Exception
	{
		String cmd = "qsub " + scriptName;
		
		logger.info("submitting job (" + jobDir.getName() + ") to SGE via qsub...");
		Process p = Runtime.getRuntime().exec(cmd, null, jobDir);
		SGEStreamReader reader = new SGEStreamReader(p.getInputStream());
		
		// Run the job
		p.waitFor();
		// And give the buffer time to be read properly		
		while (reader.stillReading)
			try { Thread.sleep(100); }
			catch (InterruptedException e) {}
		
		String[] str = reader.buffer.toString().split(" ");
	
		BufferedWriter out = new BufferedWriter(
			new FileWriter(new File(jobDir, ".SGE_ID")));
	
		// "your job [n]" (SGE5/6)
		// "your job-array [n].x-y:z" (SGE5)
		// "your job [n].x-y:z" (SGE6)		
		out.write(str[2]);
		out.close();
		
		logger.info("submitting job (" + jobDir.getName() + ") to SGE via qsub...ok");
	}
	
	public int getJobStatus(File jobDir)
	{
		try
		{
			logger.info("obtain job status (" + jobDir + ")");
			
			ProcessBuilder pb = new ProcessBuilder("qstat", "-g", "d", "-xml");
			pb.redirectErrorStream(true);
		
			Process p = pb.start();
			SGEStreamReader reader = new SGEStreamReader(p.getInputStream());
			
			// Run the job
			p.waitFor();
			// And give the buffer time to be read properly		
			while (reader.stillReading)
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
			
			getJobId(jobDir);
			return processBuffer(reader.buffer.toString());
		}
		catch (Exception e)
		{
			logger.warning("unable to determine job status: " + e);
			return JobStatus.UNKNOWN;
		}
	}
	
	/* Processes the output from a "qstat" command to find the status of the
	 * current job.
	 */
/*	private int processBuffer(String output)
		throws Exception
	{
		int status = JobStatus.UNKNOWN;
		System.out.println("searching for job id " + sge_job_id);
		
		BufferedReader in = new BufferedReader(new StringReader(output));
		String str = in.readLine();
		
		while (str != null)
		{
			// qstat -f response has ID on token [0] and status on [4]
			String[] tokens = str.trim().split("\\s+");
			
			if (tokens.length >= 5 && tokens[0].equals(sge_job_id))
			{
				int newStatus = JobStatus.UNKNOWN;
				
				if (tokens[4].contains("q"))
					newStatus = JobStatus.QUEUING;
				// TODO: monitor deletions?
				if (tokens[4].contains("t") || str.contains("r"))
					newStatus = JobStatus.RUNNING;
				if (tokens[4].contains("s") || str.contains("h"))
					newStatus = JobStatus.HOLDING;
				
//				System.out.println("Code is " + str + " (" + newStatus + ")");
				if (newStatus > status)
					status = newStatus;
			}			
			
			str = in.readLine();
		}
		
		in.close();
		
		return status;
	}
*/
	
	// XML read
	private int processBuffer(String output)
		throws Exception
	{
		int status = JobStatus.UNKNOWN;
		
		BufferedReader in = new BufferedReader(new StringReader(output));
		String str = in.readLine();
		
		while (str != null)
		{
			str = str.trim();
			
			// What's the job id?
			if (str.startsWith("<JB_job_number>"))
			{
				String id = str.substring(15, str.indexOf("<", 15));
				
				// Is this a job we're interested in?
				if (id.equals(sge_job_id))
				{
					// Now keep reading lines until we find the status
					str = in.readLine();
					while (str != null)
					{
						str = str.trim();
						
						if (str.startsWith("<state>"))
						{
							String state = str.substring(7, str.indexOf("<", 7));
							
							int newStatus = JobStatus.UNKNOWN;
							if (state.contains("q"))
								newStatus = JobStatus.QUEUING;
							else if (state.contains("t") || state.contains("r"))
								newStatus = JobStatus.RUNNING;
							else if (state.contains("s") || state.contains("h"))
								newStatus = JobStatus.HOLDING;
							else if (state.contains("E"))
								newStatus = JobStatus.FATAL_ERROR;
							
							if (newStatus > status)
								status = newStatus;
							
							break;
						}
						
						str = in.readLine();
					}
				}
			}
			
			str = in.readLine();
		}

		in.close();
		
		return status;
	}

	
	/* Deletes the job from the SGE queue. Does not attempt to determine if the
	 * request to delete was successful or not. Once the command has been sent,
	 * the job is assumed to have been removed.
	 */
	public void deleteJob(File jobDir)
	{
		try
		{
			getJobId(jobDir);
			
			ProcessBuilder pb = new ProcessBuilder("qdel", "" + sge_job_id);
			pb.redirectErrorStream(true);
		
			Process proc = pb.start();
			proc.waitFor();
		}
		catch (Exception e)
		{
			logger.warning("unable to delete job: " + e);
		}
	}
	
	public static void main(String[] args)
		throws Exception
	{
		SgeClient client = new SgeClient();
		System.out.println("status = " + client.getJobStatus(new File(args[0])));
	}
}

class SGEStreamReader extends Thread
{
	private BufferedReader reader = null;
	StringBuffer buffer = new StringBuffer();
	
	boolean stillReading = true;

	public SGEStreamReader(InputStream in)
	{
		reader = new BufferedReader(new InputStreamReader(in));			
		start();
	}

	public void run()
	{
		try
		{
			String line = reader.readLine();
									
			while (line != null)
			{
				buffer.append(line + "\n");	
				line = reader.readLine();
			}
		}
		catch (Exception e) {}
		
		try { reader.close(); }
		catch (IOException e) {}
		
		stillReading = false;
	}
}