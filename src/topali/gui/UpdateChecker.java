// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

import doe.*;

class UpdateChecker extends Thread
{
	private static int RELEASE = 6;
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
			URL url = new URL("http://www.bioss.ac.uk/~iainm/topali/version2.txt");
			URLConnection uc = url.openConnection();
			
			BufferedReader in = new BufferedReader(
				new InputStreamReader(uc.getInputStream()));
			
			webVersion = Integer.parseInt(in.readLine());
			in.close();			
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
				+ "<b>http://www.bioss.ac.uk/software.html</b> to obtain it.</html>";
			
			MsgBox.msg(msg, MsgBox.INF);
		}
		else if (useGUI)
		{
			MsgBox.msg("You already have the latest version of TOPALi v2.", MsgBox.INF);
		}
	}
	
	static void helpAbout()
	{
		MsgBox.msg("TOPALi V2 - (C) 2006 Iain Milne, Biomathematics & Statistics"
			+ " Scotland\n30th March 2006 - release 2.06", MsgBox.INF);
	}
}