// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.threads;

import java.awt.Frame;

import javax.swing.*;


/**
 * Displays a wait dialog while a DesktopThread is running.
 */
public class DefaultWaitDialog extends WaitDialog
{
	
	public DefaultWaitDialog(Frame owner, String title, String message, final DesktopThread thread) {
		super(owner, title, thread);
		
		String msg = "Please wait...";
		if(message!=null)
			msg = message;
		
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(new JLabel(msg));
		this.getContentPane().add(panel);
		
		pack();
	}
	
	
}
