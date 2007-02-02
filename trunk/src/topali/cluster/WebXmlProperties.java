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
