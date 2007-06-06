// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import javax.swing.SwingUtilities;

import topali.gui.WinMain;
import uk.ac.vamsas.client.picking.*;

public class VamsasMsgHandler implements IMessageHandler
{
	private IPickManager manager;

	public VamsasMsgHandler(IPickManager manager)
	{
		this.manager = manager;
		manager.registerMessageHandler(this);
		
		System.out.println("Successfully created VamsasMsgHandler");
	}

	public void sendMessage(Message message)
	{
		manager.sendMessage(message);
	}

	public void handleMessage(final Message message)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				processMessage(message);
			}
		};

		SwingUtilities.invokeLater(r);
	}

	private void processMessage(Message message)
	{
		if (message instanceof MouseOverMessage)
		{			
			WinMain.vEvents.processAlignmentPanelMouseOverEvent(
				(MouseOverMessage) message);
		}
	}
}