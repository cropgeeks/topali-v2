// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.control;

import java.io.*;
import java.util.logging.*;

import topali.cluster.*;

public class JobCleanup
{
	// Age (in days) that a failed job can remain for before being deleted
	private static int FAIL_AGE = 3;
	// Age (in days) that a completed job can remain for before being deleted
	private static int OK_AGE = 30;
	
	public static void main(String[] args)
	{
		// Disable logging (and SGE feedback)
		Logger.getLogger("topali.cluster").setLevel(Level.OFF);
		
		if (args.length == 0)
		{
			System.out.println("Usage: topali.cluster.control.JobCleanup <job_dir> <fail_age> <ok_age>");
			System.out.println("  <job_dir>  = path to directory containing cluster jobs");
			System.out.println("  <fail_age> = age in days that a failed job can remain");
			System.out.println("  <ok_age>   = age in days that a completed job can remain");
			System.exit(0);
		}
		
		if (args.length > 1)
			FAIL_AGE = Integer.parseInt(args[1]);
		if (args.length > 2)
			OK_AGE = Integer.parseInt(args[2]);
		
		File dir = new File(args[0]);
		for (File f: dir.listFiles())
		{
			if (f.isDirectory())
			{
				System.out.println(f);
				
				if (shouldDelete(f))
				{
					System.out.println("  deleting...");
					deleteDirectory(f);
				}
			}
			
			System.out.println();
		}
	}
	
	// Returns true if this job's directory should be deleted
	private static boolean shouldDelete(File jobDir)
	{
		long age = jobDir.lastModified();
		System.out.println("  last modified: " + age);
		
		// Has the job completed successfully (result.xml exists if so)
		boolean jobCompleted = new File(jobDir, "result.xml").exists();
		System.out.println("  job completed: " + jobCompleted);
		if (jobCompleted)
			return isOlderThan(age, OK_AGE);
		
		// If it hasn't finished, perhaps it's still running?
		boolean jobStillRunning = isJobStillRunning(jobDir);
		if (jobStillRunning == false)
			return isOlderThan(age, FAIL_AGE);
		
		// Return false for all other cases
		return false;
	}
	
	// Returns true if the job stored within jobDir is still on the cluster q
	private static boolean isJobStillRunning(File jobDir)
	{
		SgeClient client = new SgeClient();
		if (client.getJobStatus(jobDir) == JobStatus.UNKNOWN)
		{
			System.out.println("  job does not appear to be on cluster");
			return false;
		}
		else
		{
			System.out.println("  job is still on cluster");
			return true;
		}
	}
	
	// Returns true if the given time is older than the given number of days ago
	private static boolean isOlderThan(long fileAge, int daysAgo)
	{
		// Get this time as a time-in-ms
		long t = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000);
		
		System.out.println("  " + daysAgo + " day(s) ago is " + t);
		
		if (fileAge < t)
		{
			System.out.println("  job is old enough to delete");
			return true;
		}
		else
		{
			System.out.println("  job is NOT old enough to delete");
			return false;
		}
	}
	
	// Recursivly deletes all files and subdirectories in the given directory
	private static void deleteDirectory(File directory)
	{
		File[] files = directory.listFiles();
		
		for (File f: files)
		{
			if (f.isDirectory())
				deleteDirectory(f);
			
			f.delete();
		}
		
		directory.delete();
	}
}