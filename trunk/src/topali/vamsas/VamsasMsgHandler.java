// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import topali.data.Sequence;
import topali.gui.*;
import uk.ac.vamsas.client.picking.*;

public class VamsasMsgHandler implements IMessageHandler
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private IPickManager manager;
	ObjectMapper mapper;
	
	public VamsasMsgHandler(IPickManager manager, VamsasManager vamman)
	{
		this.manager = manager;
		this.mapper = vamman.mapper;
		manager.registerMessageHandler(this);
	}
	
	public void sendMessage(Message message)
	{
		if(TOPALi.debugClient)
			log.info("Send new message: "+message.getRawMessage());
		manager.sendMessage(message);
	}

	public void handleMessage(final Message message)
	{
		if(TOPALi.debugClient)
			log.info("Received new message: "+message.getRawMessage());
		
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
			MouseOverMessage msg = (MouseOverMessage)message;
			String id = msg.getVorbaID();
			Object tmp = mapper.getTopaliObject(id);
			if(tmp!=null && tmp instanceof Sequence) {
				Sequence seq = (Sequence)tmp;
				int pos = msg.getPosition();
				WinMain.vEvents.processAlignmentPanelMouseOverEvent(seq, pos+1);
			}
		}
	}
}