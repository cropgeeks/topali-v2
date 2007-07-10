// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.text.*;
import java.util.*;

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
		Analyses = ResourceBundle.getBundle("res.text.analyses", Prefs.locale);
		Gui = ResourceBundle.getBundle("res.text.gui", Prefs.locale);
		GuiDiag = ResourceBundle.getBundle("res.text.gui_dialog", Prefs.locale);
		GuiNav = ResourceBundle.getBundle("res.text.gui_nav", Prefs.locale);
		GuiFile = ResourceBundle.getBundle("res.text.gui_file", Prefs.locale);
		GuiTree = ResourceBundle.getBundle("res.text.gui_tree", Prefs.locale);
	}
	
	public static String format(String text, Object ... args)
	{
		MessageFormat msg = new MessageFormat(text, Prefs.locale);
		
		return msg.format(args);
	}
}
