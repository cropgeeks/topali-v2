// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.io.IOException;
import topali.data.AlignmentData;
import uk.ac.vamsas.client.*;
import uk.ac.vamsas.client.simpleclient.SimpleClientFactory;
import uk.ac.vamsas.objects.core.*;

public class VManager
{
	SimpleClientFactory fact;
	IClient client;
	ClientHandle cHandle = new ClientHandle("TOPALi", "16");
	PickHandler pick;
	
	public VManager() throws IOException {
		fact = new SimpleClientFactory();
	}
	
	public String[] getAvailableSessions() {
		return fact.getCurrentSessions();
	}
	
	public void connectSession(String session) {
		client = fact.getIClient(cHandle, session);
		//pick = new PickHandler(client.getPickManager());
		System.out.println("Vamsas connect to: "+client.getSessionUrn());
	}
	
	public void exportData(AlignmentData aData) throws IOException {
		IClientDocument doc = client.getClientDocument();
		uk.ac.vamsas.objects.core.VAMSAS[] vamsasses = doc.getVamsasRoots();
		
		uk.ac.vamsas.objects.core.VAMSAS vamsas = null;
		if(vamsasses.length>0)
			vamsas = vamsasses[0];
		else
			vamsas = new uk.ac.vamsas.objects.core.VAMSAS();
		
		DataSet vData = VamsasMapper.createVamsasDataSet(aData);
		vamsas.addDataSet(vData);
	}
	
	public AlignmentData importData() {
		return null;
	}
}
