// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.io.IOException;

import org.apache.log4j.Logger;

import topali.gui.Project;
import uk.ac.vamsas.client.*;
import uk.ac.vamsas.client.picking.IPickManager;
import uk.ac.vamsas.client.simpleclient.SimpleClientFactory;

public class VamsasManager
{
	 Logger log = Logger.getLogger(this.getClass());
	
	public static final String newSession = "NewSession";
	public static final ClientHandle client = new ClientHandle("topali", "2.16");
	public static final UserHandle user = new UserHandle(System.getProperty("user.name"), "");
	
	private Project project;
	private IClient vclient;
	private VamsasEventHandler eventHandler;
	private IPickManager pickManager;
	public VamsasMsgHandler msgHandler;
	
	public VamsasManager(Project project) {
		this.project = project;
	}
	
	public String[] getAvailableSessions() throws Exception {
		IClientFactory clientfactory = new SimpleClientFactory();
		String[] sessions = clientfactory.getCurrentSessions();
		return sessions;
	}
	
	public void connect(String session) throws Exception {
		log.info("Connecting to vamsas session: "+session);
		initVamsas(session);
	}
	
	public void disconnect() {
		log.info("Disconnecting from vamsas session.");
		//vclient.finalizeClient();
	}
	
	public void read() throws IOException {
		IClientDocument cDoc = vclient.getClientDocument();
		
		Project vProject = VAMSASUtils.loadProject(cDoc);
		if(vProject!=null) {
			this.project.merge(vProject);
		}
		
		VamsasDocumentHandler docHandler = new VamsasDocumentHandler(this.project, cDoc);
		docHandler.read();
		
		cDoc.setVamsasRoots(cDoc.getVamsasRoots());
		vclient.updateDocument(cDoc);
		cDoc = null;
		
		msgHandler = new VamsasMsgHandler(pickManager, this.project.getVamsasMapper());
	}
	
	public void write() throws IOException {
		IClientDocument cDoc = vclient.getClientDocument();
		
		VamsasDocumentHandler docHandler = new VamsasDocumentHandler(this.project, cDoc);
		docHandler.write();
		
		VAMSASUtils.storeProject(this.project, cDoc); 
		
		cDoc.setVamsasRoots(cDoc.getVamsasRoots());
		vclient.updateDocument(cDoc);
		cDoc = null;
	}
	
	private void initVamsas(String session) throws Exception {
		
		IClientFactory clientfactory = new SimpleClientFactory();
		
		// Get an Iclient with session data
		if (session != null) {
			if(session.equals(VamsasManager.newSession)) 
				vclient = clientfactory.getNewSessionIClient(VamsasManager.client, VamsasManager.user);
			else
				vclient = clientfactory.getIClient(VamsasManager.client, VamsasManager.user, session);
		}
		else
			vclient = clientfactory.getIClient(VamsasManager.client, VamsasManager.user);
		
		// Create the Handler
		eventHandler = new VamsasEventHandler();
		eventHandler.connect(this);
		
		//Join the session
		vclient.joinSession();
		
		pickManager = vclient.getPickManager();
		if(pickManager==null)
			log.warn("No PickManager available. Inter-Application messaging disabled.");
	}
	
	protected IClient getVClient() {
		return vclient;
	}
}