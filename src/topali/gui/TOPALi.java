// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.*;
import java.util.logging.*;

import topali.mod.*;

public class TOPALi extends Applet
{
	private boolean isApplet = false;
	private JWindow splash = null;
	
	private Prefs prefs = new Prefs();
	private Icons icons = new Icons();
	
	public static WinMain winMain;
	
	public static void main(String[] args)
	{ 
		Logger.getLogger("topali.gui.TOPALi").info("Locale: " + Locale.getDefault());

		// Let axis know where its config file is
		System.setProperty("axis.ClientConfigFile", "res/client-config.wsdd");
			
		Utils.createScratch();
		
		// These don't work (here) - stick em in as -D options on startup
//		System.setProperty("sun.java2d.translaccel", "true");
//		System.setProperty("sun.java2d.ddoffscreen", "false");
//		System.setProperty("sun.java2d.noddraw", "true");
//		System.setProperty("sun.java2d.opengl","True");


		File initialProject = null;
		if (args.length == 1 && args[0] != null)
			initialProject = new File(args[0]);

		new TOPALi(initialProject);
	}
	
	public void init()
	{ 
		System.out.println("Applet init()");
		isApplet = true;
//		new TOPALi();
	}
	
	public void destroy()
	{
		exit();
	}
	
	public TOPALi(final File initialProject)
	{		
		showSplash();
		
		// Load the preferences
		prefs.loadPreferences(new File(System.getProperty("user.home"), ".TOPALiV2.xml"));
		doEncryption(true);
		
		setProxy();
				
		try
		{
			if (Prefs.isWindows)
			{
//				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				UIManager.setLookAndFeel("org.fife.plaf.Office2003.Office2003LookAndFeel");
		//		UIManager.setLookAndFeel("org.fife.plaf.OfficeXP.OfficeXPLookAndFeel");
		//		UIManager.setLookAndFeel("org.fife.plaf.VisualStudio2005.VisualStudio2005LookAndFeel");
		
				UIManager.put("OptionPane.errorIcon", Icons.WIN_ERROR);
				UIManager.put("OptionPane.informationIcon", Icons.WIN_INFORM);
				UIManager.put("OptionPane.warningIcon", Icons.WIN_WARN);
				UIManager.put("OptionPane.questionIcon", Icons.WIN_QUESTION);
			}
			else if (Prefs.isMacOSX)
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			else
				UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
		}
		catch (Exception e) { System.out.println(e); }
		
		// Create and initialise the main window
		winMain = new WinMain();
		winMain.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e)
			{				
				exit();
			}
			
			public void windowOpened(WindowEvent e)
			{
				hideSplash();
				
				// If this is the first time TOPALi has been loaded, show help
				if (Prefs.gui_first_run)
				{
					JButton b = new JButton();
			    	TOPALiHelp.enableHelpOnButton(b, "intro");
			    	b.doClick();
			    	Prefs.gui_first_run = false;
				}
				
				// Do we want to open an initial project?
				if (initialProject != null)
					winMain.menuFileOpenProject(initialProject.getPath());
				
				// Fire off an update check thread
				if (Prefs.web_check_startup)
					winMain.menuHelpUpdate(false);
			}
		});
		
		new doe.MsgBox(winMain, Text.Gui.getString("WinMain.gui01"));		
		winMain.setVisible(true);
	}
	
	public static void setProxy()
	{		
		if (Prefs.web_proxy_enable)
		{
			System.setProperty("http.proxyHost", Prefs.web_proxy_server);
			System.setProperty("http.proxyPort", "" + Prefs.web_proxy_port);
//			System.setProperty("http.auth.ntlm.domain", "SIMS");
			
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(
						Prefs.web_proxy_username,
						Prefs.web_proxy_password.toCharArray());
				}
			});
		}
		else
		{
			System.setProperty("http.proxyHost", "");
			System.setProperty("http.proxyPort", "");
		}
	}
	
	private void exit()
	{
		// Check it's ok to exit
		if (!winMain.okToContinue())
			return;
		winMain.setVisible(false);
				
		if (winMain.getExtendedState() == JFrame.MAXIMIZED_BOTH)
		{
			Prefs.gui_maximized = true;
			winMain.setExtendedState(JFrame.NORMAL);
		}
		else
			Prefs.gui_maximized = false;
				
		Prefs.gui_win_width  = winMain.getWidth();
		Prefs.gui_win_height = winMain.getHeight();
		Prefs.gui_splits_loc = winMain.splits.getDividerLocation();
		winMain.ovDialog.exit();
		winMain.pDialog.exit();
	
		// Save the preferences
		doEncryption(false);
		prefs.savePreferences(new File(System.getProperty("user.home"),	".TOPALiV2.xml"));
		// Remove tmp files
//		Utils.emptyScratch();
		
		
		// And exit
		if (isApplet == false)
			System.exit(0);
		else
			winMain.dispose();
	}
	
	private void showSplash()
	{
		JLabel label = new JLabel(new ImageIcon(
			getClass().getResource("/res/icons/splash.png")));
		
		splash = new JWindow();
		splash.add(label);
		splash.pack();
		splash.setLocationRelativeTo(null);
		splash.setVisible(true);
	}
	
	private void hideSplash()
	{
		splash.setVisible(false);
		splash.dispose();
	}
	
	// Decrypts/encrypts passwords that have been written to disk by TOPALi
	private void doEncryption(boolean decrypt)
	{
		// About as secure as a chocolate teapot is functional...
		String key    = "287e283d5737552c5a72277561745452";
		String scheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;
		
		try
		{
			StringEncrypter s = new StringEncrypter(scheme, key);
		
			if (decrypt)
			{
				try { Prefs.web_proxy_password = s.decrypt(Prefs.web_proxy_password); }
				catch (EncryptionException e) {}
			}
			else
			{
				try { Prefs.web_proxy_password = s.encrypt(Prefs.web_proxy_password); }
				catch (EncryptionException e) {}
			}
		}
		catch (Exception e) {}
	}
}