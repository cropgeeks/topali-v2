// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.BorderLayout;
import java.io.*;
import java.net.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.var.LinkLabel;
import doe.MsgBox;

class UpdateChecker extends Thread
{
	 Logger log = Logger.getLogger(this.getClass());

	private static int RELEASE = 17;

	private int webVersion = 0;

	private boolean useGUI = false;

	UpdateChecker(boolean useGUI)
	{
		this.useGUI = useGUI;

		start();
	}

	@Override
	public void run()
	{
		try
		{
			URL url = new URL("http://www.topali.org/topali/version.txt");
			URLConnection uc = url.openConnection();

			BufferedReader in = new BufferedReader(new InputStreamReader(uc
					.getInputStream()));

			String str = null;
			while ((str = in.readLine()) != null)
			{

				if (str.startsWith("<!-- Current = "))
				{
					webVersion = Integer.parseInt(
						str.substring(14, str.indexOf("-->")).trim());
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
			p.add(new JLabel("<html>A new version of TOPALi v2 is available. Please visit our website to obtain it.<br><br>"
				+ "Upgrading sooner rather than later is advised otherwise your version may not<br>communicate with our "
				+ "remote analyses services properly.<br><br></html>"), BorderLayout.CENTER);
			p.add(new LinkLabel("http://www.topali.org"), BorderLayout.SOUTH);
			JOptionPane.showMessageDialog(TOPALi.winMain, p, "Update available", JOptionPane.INFORMATION_MESSAGE);

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
		String msg = "<html><b>TOPALi v2</b> (2.17)<br><br>"
				+ "Copyright &copy 2003-2007 Biomathematics & Statistics Scotland<br><br>"
				+ "Developed by Iain Milne, Dominik Lindner, and Frank Wright<br>"
				+ "with contributions from Dirk Husmeier, Gráinne McGuire, and Adriano Werhli<br><br>"
				+ "This software is licensed. Please see accompanying "
				+ "license file for details.<br><br>"
				+ "My TOPALi ID: " + Prefs.appId + "</html>";

		doe.MsgBox.msg(msg, doe.MsgBox.INF);
	}
}