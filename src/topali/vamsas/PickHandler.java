// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import javax.swing.*;

import topali.gui.*;

import uk.ac.vamsas.client.picking.*;

public class PickHandler implements IMessageHandler
{
	private IPickManager manager;
		
	public PickHandler(IPickManager manager)
	{
		this.manager = manager;
		manager.registerMessageHandler(this);
	}
	
	public void sendMessage(Message message)
	{
		manager.sendMessage(message);
	}
	
	public void handleMessage(final Message message)
	{
		Runnable r = new Runnable() {
			public void run() {
				processMessage(message);
			}
		};
		
		SwingUtilities.invokeLater(r);
	}
	
	private void processMessage(Message message)
	{
		if (message instanceof MouseOverMessage)
		{
			MouseOverMessage mom = (MouseOverMessage) message;
			
			String seqID = mom.getVorbaID();
			int position = mom.getPosition();
			
			TOPALi.winMain.vamsasMouseOver(seqID, position);
		}
	}
}