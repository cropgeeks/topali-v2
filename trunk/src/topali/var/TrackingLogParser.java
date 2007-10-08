// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.io.*;
import java.net.*;
import java.util.*;

public class TrackingLogParser
{

	final static String logFileName = "\\\\gruffalo\\tomcat\\topali\\public\\tracking.log";
	final static boolean dnsLookup = true;
	
	HashSet<String> excludedIps = new HashSet<String>();

	Hashtable<String, User> users = new Hashtable<String, User>();

	Hashtable<String, Location> locations = new Hashtable<String, Location>();
	
	Hashtable<String, Job> jobs = new Hashtable<String, Job>();

	LinkedList<Download> downloads = new LinkedList<Download>();
	
	static Hashtable<String, String> lookupHostAdd = new Hashtable<String, String>();

	static Hashtable<String, String> lookupAddHost = new Hashtable<String, String>();

	public TrackingLogParser()
	{
		excludedIps.add("143.234.98.21"); // dominik
		excludedIps.add("143.234.98.74"); // iain
		excludedIps.add("143.234.99.24"); // frank
	}

	public void evaluate() throws Exception
	{
		parse();

		int cancelledJobs = 0;
		int completedJobs = 0;
		Hashtable<String, Integer> job = new Hashtable<String, Integer>();
		for (Job j : jobs.values())
		{
			if (j.status == Job.CANCELLED)
				cancelledJobs++;
			if (j.status == Job.COMPLETETED)
				completedJobs++;

			Integer i = job.get(j.type);
			if (i == null)
				i = 1;
			else
				i++;
			job.put(j.type, i);
		}

		System.out.println();
		System.out.println();
		System.out.println("SUMMARY");
		System.out.println("-------");
		System.out.println();
		System.out.println("Downloads");
		System.out.println("---------");
		System.out.println("Windows: "+countDownloads(Download.WINDOWS));
		System.out.println("Linux: "+countDownloads(Download.LINUX));
		System.out.println("Solaris: "+countDownloads(Download.SOLARIS));
		System.out.println("Mac: "+countDownloads(Download.MAC));
		System.out.println("Total: "+countDownloads(Download.WINDOWS | Download.SOLARIS | Download.LINUX | Download.MAC));
		System.out.println();
		System.out.println("Run with Java WebStart: "+countDownloads(Download.WEBSTART));
		System.out.println();

		System.out.println("Users");
		System.out.println("-----");
		System.out.println("Number of Users: " + users.size());
		System.out.println("Locations:");
		Collection<Location> tmp = locations.values();
		Location[] loc = new Location[tmp.size()];
		loc = tmp.toArray(loc);
		Arrays.sort(loc);
		for (Location l : loc)
		{
			System.out.println(l.location + ": " + l.count + " users");
		}
		System.out.println();
		
		System.out.println("Jobs");
		System.out.println("----");
		System.out.println("Submitted jobs: " + jobs.size());
		System.out.println("Completed jobs: " + completedJobs);
		System.out.println("Cancelled jobs: " + cancelledJobs);
		System.out.println("Details: [TYPE: TOTAL (COMPLETED, CANCELLED)]");
		for (String s : job.keySet())
		{
			System.out.println(s + ": " + job.get(s)+" ("+countJobs(s, Job.COMPLETETED)+", "+countJobs(s, Job.CANCELLED)+")");
		}
		
		System.out.println();
		System.out.println("Jobs per User:");
		Collection<User> tmp2 = this.users.values();
		User[] users = new User[tmp2.size()];
		users = tmp2.toArray(users);
		Arrays.sort(users);
		for(int i=0; i<users.length; i++) {
			System.out.println("User "+users[i].id+": "+users[i].jobs+" ("+users[i].ip+", "+users[i].location+")");
		}
		
	}

	private void parse() throws Exception
	{
		//2007-09-20 15:49:37 - 143.234.98.21 - 143.234.98.21 - 55150467556878101066028884146549 - OPEN
		//          0                1                 2                      3                      4
		
		System.out.print("Parsing log file...");

		BufferedReader r = new BufferedReader(new FileReader(new File(
				logFileName)));
		String line = null;
		while ((line = r.readLine()) != null)
		{
			String[] tmp = line.split("\\s+-\\s+");

			if (tmp.length < 4 || excludedIps.contains(tmp[1]))
				continue;

			if(tmp[3].startsWith("/")) {
				if(tmp[3].endsWith(".exe")) 
					downloads.add(new Download(Download.WINDOWS));
				else if(tmp[3].endsWith("Linux.sh"))
					downloads.add(new Download(Download.LINUX));
				else if(tmp[3].endsWith("Solaris.sh"))
					downloads.add(new Download(Download.SOLARIS));
				else if(tmp[3].endsWith(".dmg"))
					downloads.add(new Download(Download.MAC));
				else if(tmp[3].endsWith(".jnlp"))
					downloads.add(new Download(Download.WEBSTART));
			}
			
			if (tmp.length < 5)
				continue;
			
			else if (tmp[4].equals("OPEN"))
			{
				if (!users.containsKey(tmp[3]))
				{
					User user = new User(tmp[3], tmp[1]);
					users.put(tmp[3], user);
				}
			}

			else if (tmp[4].equals("SUBMITTED"))
			{
				Job job = new Job(tmp[6], tmp[5]);
				jobs.put(tmp[6], job);
				User user = users.get(tmp[3]);
				if(user!=null)
					user.jobs++;
			}

			else if (tmp[4].equals("COMPLETED"))
			{
				Job job = jobs.get(tmp[5]);
				if (job != null)
					job.status = Job.COMPLETETED;
			}

			else if (tmp[4].equals("CANCELLED"))
			{
				Job job = jobs.get(tmp[5]);
				if (job != null)
					job.status = Job.CANCELLED;
			}
		}
		System.out.println("ok.");
	}

	private int countJobs(String type, int status) {
		int i = 0;
		for(Job job : jobs.values()) {
			if(job.type.equals(type) && job.status==status)
				i++;
		}
		return i;
	}
	
	private int countDownloads(int type) {
		int i = 0;
		for(Download d : downloads) {
			if((d.type & type)>0)
				i++;
		}
		return i;
	}
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		TrackingLogParser t = new TrackingLogParser();
		t.evaluate();
	}

	private static String lookup(String s) throws UnknownHostException
	{
		boolean isIP = s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");

		if (isIP)
		{
			if (lookupAddHost.contains(s))
			{
				return lookupAddHost.get(s);
			}
		} else
		{
			if (lookupHostAdd.contains(s))
			{
				return lookupHostAdd.get(s);
			}
		}

		InetAddress tmp = InetAddress.getByName(s);
		String host = tmp.getHostName().toLowerCase();
		String add = tmp.getHostAddress().toLowerCase();
		lookupHostAdd.put(host, add);
		lookupAddHost.put(add, host);

		if (isIP)
			return host;
		else
			return add;
	}

	class User implements Comparable<User>
	{

		public String id;

		public String ip;

		public String location = "unknown";
		
		public int jobs = 0;

		public User(String id, String ip)
		{
			this.id = id;
			this.ip = ip;

			if(dnsLookup) {
				try
				{
					String host = lookup(ip);
					if (!host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
					{
						String[] tmp = host.split("\\.");
						location = tmp[tmp.length - 1];
						
						Location l = locations.get(location);
						if(l==null) {
							l = new Location();
							l.location = location;
							locations.put(location, l);
						}
						l.count += 1;
					}
				} catch (UnknownHostException e)
				{
				}
			}
		}

		@Override
		public int compareTo(User o)
		{
			if(o.jobs>this.jobs)
				return 1;
			else if(o.jobs<this.jobs) 
				return -1;
			else
				return this.id.compareTo(o.id);
		}
		
	}

	class Location implements Comparable<Location> {
		public String location = "unknown";
		public int count = 0;
		
		public Location() {
			
		}

		@Override
		public int compareTo(Location o)
		{
			if(o.count>this.count)
				return 1;
			else if(o.count<this.count) 
				return -1;
			else {
				return location.compareTo(o.location);
			}
		}
		
	}
	
	class Job
	{
		public static final int UNKNOWN = 0;

		public static final int COMPLETETED = 1;

		public static final int CANCELLED = 2;

		public String id;

		public String type;

		public int status;

		public Job(String id, String type)
		{
			this.id = id;
			this.type = type;
			this.status = UNKNOWN;
		}

	}
	
	class Download {
		public static final int WINDOWS = 1;
		public static final int LINUX = 2;
		public static final int MAC = 4;
		public static final int SOLARIS = 8;
		public static final int WEBSTART = 16;
		
		public int type;

		public Download(int type)
		{
			this.type = type;
		}
	}
}
