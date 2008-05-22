// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.io.*;

import com.install4j.api.launcher.*;
import com.install4j.api.update.*;

import topali.data.Prefs;

/**
 * Utility class that performs install4j updater actions on behalf of Flapjack.
 */
public class Install4j
{
	private static String URL = "http://www.topali.org/installers/updates.xml";

	public static String VERSION = "DEVELOPMENT BUILD";

	/**
	 * install4j update check. This will only work when running under the full
	 * install4j environment, so expect exceptions everywhere else
	 */
	static void doStartUpCheck()
	{
		getVersion();

		if (Prefs.web_check_startup == false)
			return;

		try
		{
			UpdateScheduleRegistry.setUpdateSchedule(UpdateSchedule.ON_EVERY_START);
			if (UpdateScheduleRegistry.checkAndReset() == false)
				return;

			UpdateDescriptor ud = UpdateChecker.getUpdateDescriptor(URL, ApplicationDisplayMode.GUI);

			if (ud.getPossibleUpdateEntry() != null)
				checkForUpdate(true);
		}
		catch (Exception e) {}
	}

	/**
	 * Shows the install4j updater app to check for updates and download/install
	 * any that are found.
	 */
	static void checkForUpdate(boolean block)
	{
		try
		{
			ApplicationLauncher.launchApplication("97", null, block, null);
		}
		catch (IOException e) {}
	}

	private static void getVersion()
	{
		try
		{
			com.install4j.api.ApplicationRegistry.ApplicationInfo info =
				com.install4j.api.ApplicationRegistry.getApplicationInfoByDir(new File("."));

			VERSION = info.getVersion();
		}
		catch (Exception e) {}
		catch (Throwable e) {}
	}
}