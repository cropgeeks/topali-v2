// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.beans.*;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.vamsas.client.*;

public class VamsasEventHandler
{
	
	Logger log = Logger.getLogger(this.getClass());
	
	VamsasManager vMan;
	
	public void connect(VamsasManager vMan) {
		
		this.vMan = vMan;
		
		vMan.getVClient().addDocumentUpdateHandler(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleDocumentUpdate(e);
			}
		});
		
		// Register close handler
		vMan.getVClient().addVorbaEventHandler(Events.DOCUMENT_REQUESTTOCLOSE, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleCloseEvent(e);
			}
		});
		
		// Register client creation handler
		vMan.getVClient().addVorbaEventHandler(Events.CLIENT_CREATION, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleClientCreationEvent(e);
			}
		});
		
		// Register client finalization handler
		vMan.getVClient().addVorbaEventHandler(Events.CLIENT_FINALIZATION, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleClientFinalizationEvent(e);
			}
		});
		
		// Register session shutdown handler
		vMan.getVClient().addVorbaEventHandler(Events.SESSION_SHUTDOWN, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleSessionShutdownEvent(e);
			}
		});
		
		// Register document finalize handler
		vMan.getVClient().addVorbaEventHandler(Events.DOCUMENT_FINALIZEAPPDATA, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleDocumentFinalizeEvent(e);
			}
		});
		
	}

	public void handleDocumentUpdate(PropertyChangeEvent e)
	{
		System.out.println("Vamsas document update for "
			+ e.getPropertyName() + ": " + e.getOldValue()
			+ " to " + e.getNewValue());
		
		try
		{
			vMan.read();
		} catch (IOException e1)
		{
			log.warn("Couldn't read vamsas document.", e1);
		}
	}
	
	public void handleCloseEvent(PropertyChangeEvent e)
	{
		System.out.println("handleCloseEvent...\n"+e);
		// TODO: ask user for a fileto save to then pass it to the vorba object
		// vorbaclient.storeDocument(java.io.File);
	}
	
	public void handleClientCreationEvent(PropertyChangeEvent e)
	{
		// Tell app add new client to its list of clients
		System.out.println("New Vamsas client for "
			+ e.getPropertyName() + ": "
			+ e.getOldValue() + " to "
			+ e.getNewValue());
	}
	
	public void handleClientFinalizationEvent(PropertyChangeEvent e)
	{
		// Tell app to update its list of clients to communicate with
		System.out.println("Vamsas client finalizing for "
			+ e.getPropertyName() + ": "
			+ e.getOldValue() + " to "
			+ e.getNewValue());
        
	}
	
	public void handleSessionShutdownEvent(PropertyChangeEvent e)
	{
		// Tell app to finalize its session data before shutdown
		System.out.println("Session " + e.getPropertyName()
			+ " is shutting down.");
	}
	
	public void handleDocumentFinalizeEvent(PropertyChangeEvent e)
	{
		// Tell app to finalize its session data prior to the storage of the
		// current session as an archive.
		System.out.println("Application received a DOCUMENT_FINALIZEAPPDATA event.\n"+e);   
	}
}
