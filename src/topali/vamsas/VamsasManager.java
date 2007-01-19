package topali.vamsas;

import java.util.*;

import uk.ac.vamsas.client.*;
import uk.ac.vamsas.client.simpleclient.*;

public class VamsasManager
{
	// Lookup table for TOPALi->VASMAS mappings
	public HashTV hashTV = new HashTV();
	// Lookup table for VASMAS->TOPALi mappings
	public HashVT hashVT = new HashVT();
	
	private IClient vamsasClient;
	
	public VamsasManager()
	{
		
	}
	
	public void createSession()
	{
		ClientHandle ch = new ClientHandle("TOPALi", "0.00");
		
//		vamsasClient = SimpleClientFactory.getIClient(ch);
	}
}