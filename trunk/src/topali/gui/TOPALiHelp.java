// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;
import javax.help.*;

public class TOPALiHelp
{
	public static HelpSet hs;
	public static HelpBroker hb;
	
	static
	{
		try
		{
//			URL url = HelpSet.findHelpSet(null, "/res/help/help.hs");
			URL url = TOPALiHelp.class.getResource("/res/help/help.hs");
			hs = new HelpSet(null, url);
			
			hb = hs.createHelpBroker("TOPALi");
			hb.setFont(new Font("SansSerif", Font.PLAIN, 12));
			
	//		((DefaultHelpBroker)hb).setActivationWindow(doe.MsgBox.frm);
		}
		catch (Exception e)
		{
			System.out.println("Error loading help");
			System.out.println(e);
		}
	}
	
	// Called by a dialog class to enable F1 help to the given helpPage
	public static void enableHelpKey(JRootPane rootPane, String helpPage)
	{
		hb.enableHelpKey(rootPane, helpPage, hs);
	}
	
	// Associates the given control with the a given online help topic
	public static void enableHelpOnButton(Component comp, String helpPage)
	{
		hb.enableHelpOnButton(comp, helpPage, hs); 
	}
	
	// Associates the given menuitem with the a given online help topic
	public static void enableHelpOnButton(JMenuItem comp, String helpPage)
	{
		hb.enableHelpOnButton(comp, helpPage, hs); 
	}
	
	public static JButton getHelpButton(String helpPage)
	{
		JButton bHelp = new JButton(Text.Gui.getString("help"));
		bHelp.setMnemonic(KeyEvent.VK_H);
		enableHelpOnButton(bHelp, helpPage);
		
		return bHelp;
	}
}