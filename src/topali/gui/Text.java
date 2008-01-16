// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import topali.var.SysPrefs;

public class Text
{
	public static ResourceBundle Analyses = null;

	public static ResourceBundle Gui = null;

	public static ResourceBundle GuiDiag = null;

	public static ResourceBundle GuiNav = null;

	public static ResourceBundle GuiFile = null;

	public static ResourceBundle GuiTree = null;

	static
	{
		Analyses = ResourceBundle.getBundle("res.text.analyses", SysPrefs.locale);
		Gui = ResourceBundle.getBundle("res.text.gui", SysPrefs.locale);
		GuiDiag = ResourceBundle.getBundle("res.text.gui_dialog", SysPrefs.locale);
		GuiNav = ResourceBundle.getBundle("res.text.gui_nav", SysPrefs.locale);
		GuiFile = ResourceBundle.getBundle("res.text.gui_file", SysPrefs.locale);
		GuiTree = ResourceBundle.getBundle("res.text.gui_tree", SysPrefs.locale);
	}

	public static String format(String text, Object... args)
	{
		MessageFormat msg = new MessageFormat(text, SysPrefs.locale);

		return msg.format(args);
	}
}
