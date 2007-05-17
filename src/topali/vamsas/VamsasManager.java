// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.beans.*;
import java.io.*;

import uk.ac.vamsas.client.*;
import uk.ac.vamsas.client.simpleclient.*;
import uk.ac.vamsas.client.picking.IPickManager;

import doe.*;

public class VamsasManager
{
	// Lookup table for TOPALi->VASMAS mappings
	public HashTV hashTV = new HashTV();

	// Lookup table for VASMAS->TOPALi mappings
	public HashVT hashVT = new HashVT();

	// An instance of the pickmanager for dealing with inter-app messages
	public VamsasMsgHandler msgHandler;

	private IClientFactory clientfactory;
	private IClient vorbaclient;


	public VamsasManager()
	{
	}
	
	public boolean initializeVamsas()
	{
		if (initVamsas() == false)
			return false;
		
		if (addHandlers() == false)
			return false;
		
		if (joinSession() == false)
			return false;
		
		// This can fail - just means pick events won't work
		createPickHandler();
		
		processVamsasDocument();
		
		return true;
	}

	private boolean initVamsas()
	{
		try { clientfactory = new SimpleClientFactory(); }
		catch (IOException e)
		{
			MsgBox.msg("" + e, MsgBox.ERR);
			return false;
		}
		
		// Get an Iclient with session data
		ClientHandle app = new ClientHandle("topali", "2");
		UserHandle user = new UserHandle(System.getProperty("user.name"), "");
		// TODO: session can hold the ID of an existing session that the user
		// wants to connect to
		String session = null;
		
		try
		{
			if (session != null)
				vorbaclient = clientfactory.getIClient(app, user, session);
			else
				vorbaclient = clientfactory.getIClient(app, user);
			
			
		}
		catch (NoDefaultSessionException e)
		{
			MsgBox.msg("There appear to be several sessions to choose from.", MsgBox.ERR);			
			return false;
		}
		
		String[] sessions = clientfactory.getCurrentSessions();
		System.out.println("VAMSAS sessions:");
		for (int s = 0; s < sessions.length; s++)
			System.err.println(sessions[s]);
		
		return true;
	}
	
	private boolean addHandlers()
	{
		vorbaclient.addDocumentUpdateHandler(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleDocumentUpdate(e);
			}
		});
		
		// Register close handler
		vorbaclient.addVorbaEventHandler(Events.DOCUMENT_REQUESTTOCLOSE, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleCloseEvent(e);
			}
		});
		
		// Register client creation handler
		vorbaclient.addVorbaEventHandler(Events.CLIENT_CREATION, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleClientCreationEvent(e);
			}
		});
		
		// Register client finalization handler
		vorbaclient.addVorbaEventHandler(Events.CLIENT_FINALIZATION, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleClientFinalizationEvent(e);
			}
		});
		
		// Register session shutdown handler
		vorbaclient.addVorbaEventHandler(Events.SESSION_SHUTDOWN, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleSessionShutdownEvent(e);
			}
		});
		
		// Register document finalize handler
		vorbaclient.addVorbaEventHandler(Events.DOCUMENT_FINALIZEAPPDATA, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				handleDocumentFinalizeEvent(e);
			}
		});
		
		return true;
	}
	
	private boolean joinSession()
	{
		// Join the session
		try { vorbaclient.joinSession(); }
		catch (Exception e)
		{
			MsgBox.msg("TOPALi was unable to join a VAMSAS session due to the "
				+ "following error:\n " +  e, MsgBox.ERR);
			return false;
		}
		
		return true;
	}
	
	private boolean createPickHandler()
	{
		IPickManager manager = vorbaclient.getPickManager();
		
		if (manager == null)
		{
			MsgBox.msg("TOPALi could not initiate a VAMSAS session pick "
				+ "handler. You will be unable to trasmit events\nbetween "
				+ "VAMSAS applications.", MsgBox.ERR);

			return false;
		}
		
		msgHandler = new VamsasMsgHandler(manager);		
		return true;
	}
	
	private void handleDocumentUpdate(PropertyChangeEvent e)
	{
		System.out.println("Vamsas document update for "
			+ e.getPropertyName() + ": " + e.getOldValue()
			+ " to " + e.getNewValue());
		
		processVamsasDocument();
	}
	
	private void handleCloseEvent(PropertyChangeEvent e)
	{
		System.out.println("handleCloseEvent...");
		// TODO: ask user for a fileto save to then pass it to the vorba object
		// vorbaclient.storeDocument(java.io.File);
	}
	
	private void handleClientCreationEvent(PropertyChangeEvent e)
	{
		// Tell app add new client to its list of clients
		System.out.println("New Vamsas client for "
			+ e.getPropertyName() + ": "
			+ e.getOldValue() + " to "
			+ e.getNewValue());
	}
	
	private void handleClientFinalizationEvent(PropertyChangeEvent e)
	{
		// Tell app to update its list of clients to communicate with
		System.out.println("Vamsas client finalizing for "
			+ e.getPropertyName() + ": "
			+ e.getOldValue() + " to "
			+ e.getNewValue());
        
	}
	
	private void handleSessionShutdownEvent(PropertyChangeEvent e)
	{
		// Tell app to finalize its session data before shutdown
		System.out.println("Session " + e.getPropertyName()
			+ " is shutting down.");
	}
	
	private void handleDocumentFinalizeEvent(PropertyChangeEvent e)
	{
		// Tell app to finalize its session data prior to the storage of the
		// current session as an archive.
		System.out.println("Application received a DOCUMENT_FINALIZEAPPDATA event.");   
	}
	
	private void processVamsasDocument()
	{
		try
		{
			IClientDocument cdoc = vorbaclient.getClientDocument();
			
			// TODO: black magic
			
			
			vorbaclient.updateDocument(cdoc);
		}
		catch (Exception e)
		{
			MsgBox.msg("TOPALi encountered a problem while accessing the "
				+ "document.\n" + e, MsgBox.ERR);
		}
	}
}