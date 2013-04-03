// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.io.*;
import java.util.*;

public class CountLogs
{
	private Hashtable<String, User> hashtable = new Hashtable<String, User>(500);

	public static void main(String[] args)
	{
		new CountLogs(args[0]);
	}

	public CountLogs(String logfile)
	{
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(new File(
					logfile)));

			String str = in.readLine();
			while (str != null)
			{
				if (str.startsWith("INFO:"))
					processLine(str);

				str = in.readLine();
			}

			in.close();
		} catch (Exception e)
		{
			System.out.println(e);
		}

		System.out.println("Unique IPs: " + hashtable.size());
		System.out.println();

		Enumeration<String> keys = hashtable.keys();
		while (keys.hasMoreElements())
		{
			User user = hashtable.get(keys.nextElement());
			System.out.print(user.ip + ", ");
			System.out.print(user.PDM + ", ");
			System.out.print(user.HMM + ", ");
			System.out.print(user.DSS + ", ");
			System.out.print(user.LRT + ", ");
			System.out.print(user.MBT);
			System.out.println();
		}
	}

	private void processLine(String str)
	{
		String ip = str.substring(str.lastIndexOf("-") + 1);

		User user = getUser(ip);

		if (str.contains("PDM"))
			user.PDM++;
		else if (str.contains("HMM"))
			user.HMM++;
		else if (str.contains("DSS"))
			user.DSS++;
		else if (str.contains("LRT"))
			user.LRT++;
		else if (str.contains("MBT"))
			user.MBT++;
	}

	private User getUser(String ip)
	{
		User user = hashtable.get(ip);

		if (user == null)
		{
			user = new User(ip);
			hashtable.put(user.ip, user);
		}

		return user;
	}

	private static class User
	{
		String ip;

		int PDM, HMM, DSS, LRT, MBT;

		User(String ip)
		{
			this.ip = ip;
		}
	}
}