// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.awt.Cursor;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.gui.TOPALi;
import topali.var.utils.Utils;

public class LinkLabel extends JLabel implements MouseListener
{
	 Logger log = Logger.getLogger(this.getClass());

	String url;

	public LinkLabel(String url) {
		this.url = url;
		setText("<html><a href=\""+url+"\">"+url+"</a></html>");
		addMouseListener(this);
	}

	public void mouseClicked(MouseEvent e)
	{
//		try
//		{
//			boolean success = Utils.openBrowser(url);
//			if(!success) {
//				JOptionPane.showMessageDialog(TOPALi.winMain, "Sorry, the Java Desktop API is not yet supported on your system.", "Error", JOptionPane.ERROR_MESSAGE);
//				log.warn("Java Desktop API not supported on this system.");
//			}
//		} catch (Exception e1)
//		{
//			log.warn("Opening browser for "+url+" failed!", e1);
//		}
	}

	public void mouseEntered(MouseEvent e)
	{
		//setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	public void mouseExited(MouseEvent e)
	{
		//setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

}
