// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.var.LinkLabel;

import doe.MsgBox;

class UpdateChecker extends Thread
{
	Logger log = Logger.getLogger(this.getClass());
	
	private static int RELEASE = 16;

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
			URL url = new URL(
					"http://gruffalo.scri.ac.uk/topali/version.jsp?client=topali&id="+Prefs.ident);
			URLConnection uc = url.openConnection();

			BufferedReader in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));
				
			String str = null;
			while ((str = in.readLine()) != null)
			{
				
				if (str.startsWith("Current = "))
				{
					webVersion = Integer.parseInt(str.substring(10));
					break;
				}
			}

//			webVersion = Integer.parseInt(in.readLine());
			in.close();

			log.info("Connection to " + url);
			log.info("webVersion: " + webVersion + " (current: "+ RELEASE + ")");
		} catch (Exception e)
		{
			if (useGUI)
				MsgBox.msg(
						"TOPALi was unable to check for a new version due to "
								+ "the following unexpected error:\n  " + e,
						MsgBox.ERR);
			
			log.warn("Unable to check for updates.",e);

			return;
		}

		if (webVersion > RELEASE)
		{
			JPanel p = new JPanel(new BorderLayout());
			p.add(new JLabel("<html>A new version of TOPALi v2 is available.<br>Please visit our website to obtain it.</html>"), BorderLayout.CENTER);
			p.add(new LinkLabel("http://www.bioss.ac.uk/knowledge/topali"), BorderLayout.SOUTH);
			JOptionPane.showMessageDialog(null, p, "Update available", JOptionPane.INFORMATION_MESSAGE);
			
//			String msg = "<html>A new version of TOPALi v2 is available. Please visit "
//					+ "<b>http://www.bioss.ac.uk/knowledge/topali</b> to obtain it.</html>";
//
//			MsgBox.msg(msg, MsgBox.INF);
		} else if (useGUI)
		{
			MsgBox.msg("You already have the latest version of TOPALi v2.",
					MsgBox.INF);
		}
	}

	static void helpAbout()
	{
		String msg = "<html><b>TOPALi v2</b> (2.16)<br><br>"
				+ "Copyright &copy 2003-2007 Biomathematics & Statistics Scotland<br><br>"
				+ "Developed by Iain Milne, Dominik Lindner, and Frank Wright<br>"
				+ "with contributions from Dirk Husmeier, Gráinne McGuire, and Adriano Werhli<br><br>"
				+ "This software is licensed. Please see accompanying "
				+ "license file for details." + "</html>";

		doe.MsgBox.msg(msg, doe.MsgBox.INF);
	}
}