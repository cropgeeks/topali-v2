// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.threads;

import java.awt.Frame;
import java.awt.event.*;

import javax.swing.JDialog;

/**
 * Base class for WaitDialogs, which start a DesktopThread when set to visible,
 * and kills the thread when closed.
 */
public abstract class WaitDialog extends JDialog implements DesktopThreadObserver
{
	
	public WaitDialog(Frame owner, String title, final DesktopThread t) {
		super(owner, title, true);
		
		if(t!=null) {
			
			t.addObserver(this);
			
			addWindowListener(new WindowAdapter() {
	
				@Override
				public void windowClosing(WindowEvent e)
				{
					t.kill();
				}
	
				@Override
				public void windowOpened(WindowEvent e)
				{
					t.start();
				}
				
			});
		
		}
	}

	@Override
	public void update(Object obj)
	{	
		if(obj == DesktopThread.THREAD_FINISHED)
			setVisible(false);
	}
	
	
}
