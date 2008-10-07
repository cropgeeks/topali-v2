// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import java.io.*;
import java.util.*;

class WebXmlProperties
{
	private static Properties props;

	WebXmlProperties(File filename)
	{
		if (props == null)
			loadProperties(filename);
	}

	private static void loadProperties(File filename)
	{
		try
		{
			props = new Properties();
			props.load(new FileInputStream(filename));

			// We need to process the properties to replace environment variables
			// eg, so tmp-dir = $TMP/wibble
			// becomes tmp-dir = /tmp/wibble

			// Get all the system environment variables
			Map<String,String> envmap = System.getenv();

			// For each property, run it via the shell to process any env vars
			Enumeration propKeys = props.keys();
			while (propKeys.hasMoreElements())
			{
				String key   = (String) propKeys.nextElement();
				String value = (String) props.get(key);

				// Regex match each env variable against the property
				for (String env: envmap.keySet())
					value = value.replaceAll("\\$"+env, envmap.get(env));

				props.put(key, value);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	static String getParameter(String key)
	{
		return props.getProperty(key);
	}
}
