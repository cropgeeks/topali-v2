// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.io.File;
import java.util.Locale;

public class SysPrefs {
	public static Locale locale = Locale.getDefault();

	public static File tmpDir =
		new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name") + "-topaliv2");

	public static boolean isWindows =
		System.getProperty("os.name").toLowerCase().startsWith("windows");

	public static boolean isMacOSX =
		System.getProperty("os.name").toLowerCase().startsWith("mac os");

	public static boolean isLinux =
		System.getProperty("os.name").toLowerCase().startsWith("linux");

}
