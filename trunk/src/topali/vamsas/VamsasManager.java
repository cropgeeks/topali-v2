// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import uk.ac.vamsas.client.IClient;
import uk.ac.vamsas.client.picking.IPickManager;

public class VamsasManager
{
	// Lookup table for TOPALi->VASMAS mappings
	public HashTV hashTV = new HashTV();

	// Lookup table for VASMAS->TOPALi mappings
	public HashVT hashVT = new HashVT();

	// An instance of the pickmanager for dealing with inter-app messages
	public PickHandler msgHandler;

	private IClient vamsasClient;

	public VamsasManager()
	{

	}

	public void createSession()
	{
		// ClientHandle ch = new ClientHandle("TOPALi", "0.00");

		// vamsasClient = SimpleClientFactory.getIClient(ch);

		IPickManager pickManager = vamsasClient.getPickManager();
		msgHandler = new PickHandler(pickManager);
	}
}