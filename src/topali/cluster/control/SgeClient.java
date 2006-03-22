package topali.cluster.control;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import topali.cluster.*;

public class SgeClient implements ICluster
{
	private static Logger logger = Logger.getLogger("topali.cluster.control");
	
	private int sge_job_id;
	
	// Initilizes the SgeClient to read the job's ID from a .SGE_ID file
	public void getJobId(File jobDir)
	{
		// Bugger all we can do about it if it doesn't exist (yet)...
		File jobFile = new File(jobDir, ".SGE_ID");		
		if (jobFile.exists() == false)
			return;
		
		try
		{
			String s = ClusterUtils.readFile(jobFile);
			
			// [n].x-y:z
			if (s.contains("."))
				sge_job_id = Integer.parseInt(s.substring(0, s.indexOf(".")));
			// [n]
			else
				sge_job_id = Integer.parseInt(s);
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
	
	/*
	 * Submits (qsub) command and reads the return from qsub so that a .SGE_ID
	 * file can be placed in the job's directory BEFORE the job begins.
	 */
	public void submitJob(File jobDir, String scriptName)
		throws Exception
	{
		String cmd = "qsub " + scriptName;
		
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
	}
	
	public int getJobStatus(File jobDir)
	{
		getJobId(jobDir);
		
		try
		{
			ProcessBuilder pb = new ProcessBuilder("qstat");
			pb.redirectErrorStream(true);
		
			Process p = pb.start();
			SGEStreamReader reader = new SGEStreamReader(p.getInputStream());
			
			// Run the job
			p.waitFor();
			// And give the buffer time to be read properly		
			while (reader.stillReading)
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
			
			return processBuffer(reader.buffer.toString());
		}
		catch (Exception e)
		{
			logger.info("unable to determine job status: " + e);
			return JobStatus.UNKNOWN;
		}
	}
	
	/* Processes the output from a "qstat" command to find the status of the
	 * current job.
	 */
	private int processBuffer(String output)
		throws Exception
	{
		int status = JobStatus.UNKNOWN;
		
		BufferedReader in = new BufferedReader(new StringReader(output));
		String str = in.readLine();
		
		while (str != null)
		{
			StringTokenizer st = new StringTokenizer(str);
			System.out.println(str);
			
			// What job_id (if any) does this line relate to
			int id = -1;
			try { id = Integer.parseInt(st.nextToken()); }
			catch (Exception e) {}
			
			if (id == sge_job_id)
			{
				// Strip out the status code
				str = str.substring(38, 44).toLowerCase();
				System.out.println("  " + str);
				
				int newStatus = JobStatus.UNKNOWN;
				
				if (str.contains("q"))
					newStatus = JobStatus.QUEUING;
				// TODO: monitor deletions?
				if (str.contains("t") || str.contains("r"))
					newStatus = JobStatus.RUNNING;
				if (str.contains("s") || str.contains("h"))
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
	
	/* Deletes the job from the SGE queue. Does not attempt to determine if the
	 * request to delete was successful or not. Once the command has been sent,
	 * the job is assumed to have been removed.
	 */
	public void deleteJob(File jobDir)
	{
		getJobId(jobDir);
		
		try
		{
			ProcessBuilder pb = new ProcessBuilder("qdel", "" + sge_job_id);
			pb.redirectErrorStream(true);
		
			Process proc = pb.start();
			proc.waitFor();
		}
		catch (Exception e)
		{
			logger.info("unable to delete job: " + e);
		}
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