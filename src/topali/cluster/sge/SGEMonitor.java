package topali.cluster.sge;

import java.io.*;
import java.util.*;

import static topali.cluster.JobStatus.*;

public class SGEMonitor
{
	private int sge_job_id;
	
	public SGEMonitor()
	{
	}
	
	public SGEMonitor(File jobDir)
	{
		loadFile(jobDir);
	}
	
	public boolean loadFile(File jobDir)
	{
		File jobFile = new File(jobDir, ".SGE_ID");
		
		if (jobFile.exists() == false)
			return false;
		
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(jobFile));
			sge_job_id = Integer.parseInt(in.readLine());
			
			System.out.println(sge_job_id);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return false;
		}
		
		return true;
	}
	
	/* Deletes the job from the SGE queue. Does not attempt to determine if the
	 * request to delete was successful or not. Once the command has been sent,
	 * the job is assumed to have been removed.
	 */
	public boolean deleteJob()
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder("qdel", "" + sge_job_id);
			pb.redirectErrorStream(true);
		
			Process proc = pb.start();
			proc.waitFor();
			
			return true;
		}
		catch (Exception e)
		{
			System.out.println(e);
			return false;
		}
	}
	
	public int getJobStatus()
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder("qstat");
			pb.redirectErrorStream(true);
		
			Process proc = pb.start();
			SGEStreamReader sge = new SGEStreamReader(proc.getInputStream());
			proc.waitFor();
			
			return processBuffer(sge.buffer.toString());
		}
		catch (Exception e)
		{
			System.out.println(e);
			return UNKNOWN;
		}
	}
	
	/* Processes the output from a "qstat" command to find the status of the
	 * current job.
	 */
	private int processBuffer(String output)
		throws Exception
	{
		int status = UNKNOWN;
		
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
				
				int newStatus = UNKNOWN;
				
				if (str.contains("q"))
					newStatus = QUEUING;
				// TODO: monitor deletions?
				if (str.contains("t") || str.contains("r"))
					newStatus = RUNNING;
				if (str.contains("s") || str.contains("h"))
					newStatus = HOLDING;
				
//				System.out.println("Code is " + str + " (" + newStatus + ")");
				if (newStatus > status)
					status = newStatus;
			}			
			
			str = in.readLine();
		}
		
		in.close();
		
		return status;
	}
	
	/*
	 * Submits (qsub) command and reads the return from qsub so that a .SGE_ID
	 * file can be placed in the job's directory BEFORE the job begins.
	 */
	public static void submitJob(String cmd, File jobDir)
		throws Exception
	{
		Process p = Runtime.getRuntime().exec(cmd, null, jobDir);
		SGEStreamReader reader = new SGEStreamReader(p.getInputStream());
		
		p.waitFor();
		
		String[] str = reader.buffer.toString().split(" ");
		
		BufferedWriter out = new BufferedWriter(
			new FileWriter(new File(jobDir, ".SGE_ID")));
		
		// "your job [n]" or "your job-array [n].x-y:z"
		if (str[1].equals("job"))
			out.write(str[2]);
		else
			out.write(str[2].substring(0, str[2].indexOf(".")));
		out.close();
	}
	
	public static void main(String[] args)
	{
		if (args.length != 1)
		{
			System.out.println("Usage: java topali.cluster.sge.SGEMonitor <jobdir>");
			System.exit(0);
		}
		
		SGEMonitor monitor = new SGEMonitor();
		
		if (monitor.loadFile(new File(args[0])) == false)
		{
			System.out.println(".SGE_ID file not found");
			System.exit(0);
		}
		
		System.out.println("Status is " + monitor.getJobStatus());
	}
}

class SGEStreamReader extends Thread
{
	private BufferedReader reader = null;
	StringBuffer buffer = new StringBuffer();

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
	}
}