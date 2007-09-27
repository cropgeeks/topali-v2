// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.util.*;

import sbrn.commons.multicore.TokenManager;

/* Maintains a list of all jobs currently running locally. The jobs check this
 * list to see if they're still allowed to run (if not, it means they've been
 * cancelled and they need to stop doing what they do...)
 */
public class LocalJobs
{
	// A single instance of a global TokenManager for all locally run jobs
	public static TokenManager manager = new TokenManager();

	private static Hashtable<String, Boolean> jobs = new Hashtable<String, Boolean>(
			10);

	public static void addJob(String jobId)
	{
		jobs.put(jobId, true);
	}

	public static void delJob(String jobId)
	{
		jobs.remove(jobId);
	}

	public static void cancelJob(String jobId)
	{
		jobs.put(jobId, false);
	}

	public static void cancelAll()
	{
		Enumeration<String> keys = jobs.keys();
		while (keys.hasMoreElements())
			cancelJob(keys.nextElement());
	}

	public static boolean jobsRunning()
	{
		return manager.threadsRunning();
	}

	public static boolean isRunning(String jobId)
	{
		try
		{
			boolean isRunning = jobs.get(jobId);

			return isRunning;
		} catch (NullPointerException e)
		{
			// If it's not found (as will be the case for genuine cluster jobs)
			// return true as we want them to keep running
			return true;
		}
	}
}