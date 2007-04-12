// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import topali.gui.TOPALi;

public class LinkLabel extends JLabel implements MouseListener
{
	String url;
	
	public LinkLabel(String url) {
		this.url = url;
		setText("<html><a href=\""+url+"\">"+url+"</a></html>");
		addMouseListener(this);
	}

	public void mouseClicked(MouseEvent e)
	{
		try
		{
			boolean success = Utils.openBrowser(url);
			if(!success) {
				JOptionPane.showMessageDialog(this, "Sorry, the Java Desktop API is not yet supported on your system.", "Error", JOptionPane.ERROR_MESSAGE);
				TOPALi.log.warning("Java Desktop API not supported on this system.");
			}
		} catch (Exception e1)
		{
			TOPALi.log.warning("Opening browser for "+url+" failed!");
		} 
	}

	public void mouseEntered(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	public void mouseExited(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void mousePressed(MouseEvent e)
	{		
	}

	public void mouseReleased(MouseEvent e)
	{
	}
	
}
