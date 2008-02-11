// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.i18n;

import java.text.MessageFormat;
import java.util.*;

import topali.var.SysPrefs;

public class Text
{

	public static ResourceBundle I18N = null;
	
	static
	{
//		Analyses = ResourceBundle.getBundle("res.text.analyses", SysPrefs.locale);
//		Gui = ResourceBundle.getBundle("res.text.gui", SysPrefs.locale);
//		GuiDiag = ResourceBundle.getBundle("res.text.gui_dialog", SysPrefs.locale);
//		GuiNav = ResourceBundle.getBundle("res.text.gui_nav", SysPrefs.locale);
//		GuiFile = ResourceBundle.getBundle("res.text.gui_file", SysPrefs.locale);
//		GuiTree = ResourceBundle.getBundle("res.text.gui_tree", SysPrefs.locale);
		//I18N = ResourceBundle.getBundle("res.text.i18n", Locale.getDefault());
	    I18N = ResourceBundle.getBundle("res.text.i18n", Locale.getDefault());
	}

	public static String format(String text, Object... args)
	{
		MessageFormat msg = new MessageFormat(text, Locale.getDefault());

		return msg.format(args);
	}
	
	public static String getString(String key)
	{
	    return I18N.getString(key);
	}
}
