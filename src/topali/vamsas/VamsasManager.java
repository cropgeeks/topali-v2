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

	public static ObjectMapper mapper;
	//public static Hashtable<Object, DataSet> vObjectLocator;
	
	public static ClientHandle client = new ClientHandle("topali", "2.16");
	public static UserHandle user = new UserHandle(System.getProperty("user.name"), "");
	
	private Project project;
	
	private IClient vclient;
	private VamsasEventHandler eventHandler;
	public VamsasMsgHandler msgHandler;
	
	public VamsasManager() {
		VamsasManager.mapper = new ObjectMapper();
		//VamsasManager.vObjectLocator = new Hashtable<Object, DataSet>();
	}
	
	public String[] getAvailableSessions() throws Exception {
		IClientFactory clientfactory = new SimpleClientFactory();
		String[] sessions = clientfactory.getCurrentSessions();
		return sessions;
	}
	
	public void connect(Project project, String session) throws Exception {
		this.project = project;
		initVamsas(session);
	}
	
	public void read() throws IOException {
		IClientDocument cDoc = vclient.getClientDocument();
		//VAMSASUtils.loadProject(project, cDoc);
		VamsasDocumentHandler docHandler = new VamsasDocumentHandler(project, cDoc);
		docHandler.read();
		cDoc.setVamsasRoots(cDoc.getVamsasRoots());
		vclient.updateDocument(cDoc);
		cDoc = null;
	}
	
	public void write() throws IOException {
		IClientDocument cDoc = vclient.getClientDocument();
		//VAMSASUtils.storeProject(project, cDoc);
		VamsasDocumentHandler docHandler = new VamsasDocumentHandler(project, cDoc);
		docHandler.write();
		cDoc.setVamsasRoots(cDoc.getVamsasRoots());
		vclient.updateDocument(cDoc);
		cDoc = null;
	}
	
	private void initVamsas(String session) throws Exception {
		
		IClientFactory clientfactory = new SimpleClientFactory();
		
		// Get an Iclient with session data
		if (session != null)
			vclient = clientfactory.getIClient(VamsasManager.client, VamsasManager.user, session);
		else
			vclient = clientfactory.getIClient(VamsasManager.client, VamsasManager.user);
		
		// Create the Handler
		eventHandler = new VamsasEventHandler();
		eventHandler.connect(this);
		
		//Join the session
		vclient.joinSession();
		
		IPickManager pickManager = vclient.getPickManager();
		if(pickManager==null)
			log.warn("No PickManager available. Inter-Application messaging disabled.");
		else {
			msgHandler = new VamsasMsgHandler();
			msgHandler.connect(pickManager);
		}
	}
	
	protected IClient getVClient() {
		return vclient;
	}
}