// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.threads;

import java.awt.*;

import javax.swing.*;

/**
 * Displays a WaitDialog with progress bar, while a DesktopThread is running
 */
public class ProgressBarWaitDialog extends WaitDialog implements DesktopThreadObserver
{

	JProgressBar pb;
	
	public ProgressBarWaitDialog(Frame owner, String title, String message, final DesktopThread thread) {
		super(owner, title, thread);
		
		thread.addObserver(this);
		
		String msg = "Please wait...";
		if(message!=null)
			msg = message;
		
		JPanel panel = new JPanel(new BorderLayout(5,5));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		panel.add(new JLabel(msg), BorderLayout.NORTH);
		pb = new JProgressBar();
		panel.add(pb, BorderLayout.SOUTH);
		
		this.getContentPane().add(panel);
		
		pack();
	}

	@Override
	public void update(Object obj)
	{
		if(obj instanceof Integer) {
			pb.setValue((Integer)obj);
		}
		
		super.update(obj);
	}
	
}
