package topali.gui;

import java.io.BufferedInputStream;
import java.net.*;
import topali.data.Prefs;

/**
 * Simple util class to send HTTP get requests back to our server for logging
 * purposes. As we run multiple clusters, it would mean collating all the logs
 * from each of them, but if we get the client to log back to a single server it
 * makes it easier.
 */
public class Tracker
{
    @SuppressWarnings("unused")
	public static void log(final Object...msgs)
	{
		Runnable r = new Runnable()	{
			public void run()
			{
				// Format the URL string to send to the server
				String uid = Prefs.appId;
				String address = "http://www.topali.org/topali/tracking.jsp?id=" + uid;

				for (int i = 0; i < msgs.length; i++)
					address += "&msg" + (i+1) + "=" + msgs[i];

				try
				{
					URL url = new URL(address);
					HttpURLConnection c = (HttpURLConnection) url.openConnection();

					BufferedInputStream in = new BufferedInputStream(c.getInputStream());

					byte[] b = new byte[4096];
					for (int n; (n = in.read(b)) != -1;);

			        in.close();
				}
				catch (Exception e)
				{
					// It's no big deal if the track fails
				}
			}
		};

		new Thread(r).start();
	}
}