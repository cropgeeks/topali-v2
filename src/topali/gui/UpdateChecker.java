// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.io.*;
import java.net.*;

import doe.*;

class UpdateChecker extends Thread
{
	private static int RELEASE = 15;
	private int webVersion = 0;
	
	private boolean useGUI = false;
	
	UpdateChecker(boolean useGUI)
	{
		this.useGUI = useGUI;
		
		start();
	}
	
	public void run()
	{
		try
		{
			URL url = new URL("http://www.bioss.ac.uk/knowledge/topali/version.txt");
			URLConnection uc = url.openConnection();
			
			BufferedReader in = new BufferedReader(
				new InputStreamReader(uc.getInputStream()));
			
			webVersion = Integer.parseInt(in.readLine());
			in.close();
			
			System.out.println("Connection to " + url);
			System.out.println("webVersion: " + webVersion + " (current: " + RELEASE + ")");
		}
		catch (Exception e)
		{
			if (useGUI)
				MsgBox.msg("TOPALi was unable to check for a new version due to "
					+ "the following unexpected error:\n  " + e, MsgBox.ERR);
			
			return;
		}
		
		if (webVersion > RELEASE)
		{
			String msg = "<html>A new version of TOPALi v2 is available. Please visit "
				+ "<b>http://www.bioss.ac.uk/knowledge/topali</b> to obtain it.</html>";
			
			MsgBox.msg(msg, MsgBox.INF);
		}
		else if (useGUI)
		{
			MsgBox.msg("You already have the latest version of TOPALi v2.", MsgBox.INF);
		}
	}
	
	static void helpAbout()
	{
		String msg = "<html><b>TOPALi v2</b> (2.15)<br><br>"
					+ "Copyright &copy 2003-2006<br><br>"
					+ "Iain Milne and Frank Wright<br>"
					+ "Biomathematics & Statistics Scotland<br><br>"
					+ "This software is licensed. Please see accompanying "
					+ "license file for details."
					+ "</html>";
				
		doe.MsgBox.msg(msg, doe.MsgBox.INF);
	}
}