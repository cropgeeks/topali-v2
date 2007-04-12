// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import com.sun.security.sasl.ClientFactoryImpl;

import uk.ac.vamsas.client.*;
import uk.ac.vamsas.client.picking.IPickManager;
import uk.ac.vamsas.client.simpleclient.SimpleClientFactory;

public class Vamsas
{

	public Vamsas() throws Exception {
		SimpleClientFactory fac = new SimpleClientFactory();
		ClientHandle app = new ClientHandle("TOPALi", "2.2");
		UserHandle user = new UserHandle("bla", "blup");
		IClient client = fac.getIClient(app, user); 
		
	}
	

}
