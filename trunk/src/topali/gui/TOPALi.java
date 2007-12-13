// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.applet.Applet;
import java.awt.Frame;
import java.awt.event.*;
import java.io.File;
import java.net.*;
import java.util.*;

import javax.swing.*;

import org.apache.log4j.*;

import sbrn.commons.multicore.TokenManager;
import topali.cluster.LocalJobs;
import topali.logging.GracefulShutdownHandler;
import topali.mod.*;
import topali.var.*;

public class TOPALi extends Applet implements Application
{
	//enables/disables extensive logging
	public static final boolean debugClient = true;
	public static final boolean debugJobs = false;
	
	public static final String VERSION = "2.19";

	private final String prefsFile = ".topali2-new.xml";

	 static Logger root;
	 static Logger log;

	static {
		PropertyConfigurator.configure(TOPALi.class.getResource("/res/client.log4j.properties"));
		root = Logger.getRootLogger();
		log = Logger.getLogger(TOPALi.class);
	}

	private boolean isApplet = false;

	private JWindow splash = null;

	private Prefs prefs = new Prefs();

	public static WinMain winMain;

	public static void main(String[] args)
	{

		root.info("TOPALi V. "+VERSION);
		root.info("Locale is " + Locale.getDefault());
		root.info("Running Java " + System.getProperty("java.version"));

		// Let axis know where its config file is
		System.setProperty("axis.ClientConfigFile", "res/client-config.wsdd");

		Utils.createScratch();

		// These don't work (here) - stick em in as -D options on startup
		// System.setProperty("sun.java2d.translaccel", "true");
		// System.setProperty("sun.java2d.ddoffscreen", "false");
		// System.setProperty("sun.java2d.noddraw", "true");
		// System.setProperty("sun.java2d.opengl","True");

		File initialProject = null;
		if (args.length == 1 && args[0] != null)
			initialProject = new File(args[0]);

		Icons.loadIcons();

		new TOPALi(initialProject);


	}

	@Override
	public void init()
	{
		root.info("Applet init()");
		isApplet = true;
		// new TOPALi();
	}

	@Override
	public void destroy()
	{
		shutdown();
	}

	private TOPALi(final File initialProject)
	{
		//If there is a GracefulShutdownHandler, tell it about TOPALi
		//and also use it as UncaughtExceptionHandler
		Enumeration en = root.getAllAppenders();
		while(en.hasMoreElements()) {
			Object o = en.nextElement();
			if(o instanceof GracefulShutdownHandler) {
				GracefulShutdownHandler sh = (GracefulShutdownHandler)o;
				sh.setApplication(this);
				Thread.setDefaultUncaughtExceptionHandler(sh);
			}
		}
		
		if(debugClient) {
			//MemoryMonitor memmon = new MemoryMonitor(MemoryMonitor.UNIT_MEGABYTES, 10);
			//memmon.start();
		}

		showSplash();

		// Load the preferences
		prefs.loadPreferences(new File(System.getProperty("user.home"),
				prefsFile));
		doEncryption(true);

		setProxy();
		LocalJobs.manager = new TokenManager(Prefs.gui_max_cpus);

		try
		{
			if (Prefs.isWindows)
			{
				// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				UIManager.setLookAndFeel("org.fife.plaf.Office2003.Office2003LookAndFeel");
				// UIManager.setLookAndFeel("org.fife.plaf.OfficeXP.OfficeXPLookAndFeel");
				// UIManager.setLookAndFeel("org.fife.plaf.VisualStudio2005.VisualStudio2005LookAndFeel");

				UIManager.put("OptionPane.errorIcon", Icons.WIN_ERROR);
				UIManager.put("OptionPane.informationIcon", Icons.WIN_INFORM);
				UIManager.put("OptionPane.warningIcon", Icons.WIN_WARN);
				UIManager.put("OptionPane.questionIcon", Icons.WIN_QUESTION);

			} else if (Prefs.isMacOSX)
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			else
				UIManager
						.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
		} catch (Exception e)
		{
			log.warn("Cannot set Look and Feel", e);
		}

		// Create and initialise the main window
		winMain = new WinMain();

		winMain.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				shutdown();
			}

			@Override
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
			// System.setProperty("http.auth.ntlm.domain", "SIMS");

			Authenticator.setDefault(new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(Prefs.web_proxy_username,
							Prefs.web_proxy_password.toCharArray());
				}
			});
		} else
		{
			System.setProperty("http.proxyHost", "");
			System.setProperty("http.proxyPort", "");
		}
	}

	public void shutdown()
	{
		log.info("Shutting down TOPALi");

		// Check it's ok to exit
		if (!winMain.okToContinue())
			return;
		winMain.setVisible(false);

		//check if there is a vamsas session
		if(winMain.vamsas!=null)
			winMain.vamsasDisconnect();

		if (winMain.getExtendedState() == Frame.MAXIMIZED_BOTH)
		{
			Prefs.gui_maximized = true;
			winMain.setExtendedState(Frame.NORMAL);
		} else
			Prefs.gui_maximized = false;

		Prefs.gui_win_width = winMain.getWidth();
		Prefs.gui_win_height = winMain.getHeight();
		Prefs.gui_splits_loc = WinMain.splits.getDividerLocation();
		WinMain.ovDialog.exit();
		WinMain.rDialog.exit();

		// Save the preferences
		doEncryption(false);
		prefs.savePreferences(new File(System.getProperty("user.home"),
				prefsFile));
		// Remove tmp files
		// Utils.emptyScratch();

		// And exit
		if (isApplet == false)
			System.exit(0);
		else
			winMain.dispose();
	}

	private void showSplash()
	{
		JLabel label = new JLabel(new ImageIcon(getClass().getResource(
				"/res/icons/splash.png")));

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
		if(Prefs.web_proxy_password==null || Prefs.web_proxy_password.equals(""))
			return;

		// About as secure as a chocolate teapot is functional...
		String key = "287e283d5737552c5a72277561745452";
		String scheme = StringEncrypter.DESEDE_ENCRYPTION_SCHEME;

		try
		{
			StringEncrypter s = new StringEncrypter(scheme, key);

			if (decrypt)
			{
				try
				{
					Prefs.web_proxy_password = s
							.decrypt(Prefs.web_proxy_password);
				} catch (EncryptionException e)
				{
					log.warn("Cannot decrypt.\n", e);
				}
			} else
			{
				try
				{
					Prefs.web_proxy_password = s
							.encrypt(Prefs.web_proxy_password);
				} catch (EncryptionException e)
				{
					log.warn("Cannot encrypt.", e);
				}
			}
		} catch (Exception e)
		{
			log.warn("En/Decryption failed", e);
		}
	}
}