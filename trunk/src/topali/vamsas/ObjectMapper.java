package topali.vamsas;

import java.util.*;

import uk.ac.vamsas.client.*;
import uk.ac.vamsas.objects.core.*;

class ObjectMapper
{
	// Resolves vamsas IDs to TOPALi data objects
	private Hashtable<VorbaId, Object> hashVT = new Hashtable<VorbaId, Object>();
	
	// Resolves TOPALi objects to vamsas IDs
	private IdentityHashMap<Object, VorbaId> hashTV = new IdentityHashMap<Object, VorbaId>();
	
	IClientDocument cdoc = null;
	
	ObjectMapper()
	{
	}
	
	// This needs to be called each time we want to use this class for a fresh
	// update or store of data
	void registerClientDocument(IClientDocument cdoc)
		{ this.cdoc = cdoc;	}
	
	/**
	 * Uses a TOPALi data object and attempts to return the matching vamsas
	 * object by matching the TOPALi obj reference in the TV hashtable.
	 */
	Vobject getVamsasObject(Object topaliObject)
	{
		System.out.print("Looking for key for " + topaliObject);
		
		if (hashTV.containsKey(topaliObject))
		{
			System.out.println("...found it " + hashTV.get(topaliObject));
			return cdoc.getObject(hashTV.get(topaliObject));
		}
		
		System.out.println("...didn't find it");
		return null;
	}
	
	/**
	 * Uses a vamsas data object and attempts to return the matching TOPALi
	 * object by matching the vamsas ID in the VT hashtable.
	 */
	Object getTopaliObject(Vobject vamsasObject)
	{
		System.out.print("Looking for key for " + vamsasObject);
		
		// Does this vamsas object have a key (it won't if it's never been part
		// of the document, ie, only just been created)
		VorbaId id = vamsasObject.getVorbaId();
		if (id == null)
		{
			System.out.println("id==null; registering new vamsas object.");
			// Register the object for use within the session document
			cdoc.registerObject(vamsasObject);
			return null;
		}
		
		// If it does exist...do we have a mapping for it?
		return hashVT.get(id);
	}
	
	/**
	 * Registers linked TOPALi/Vamsas objects in the hash tables.
	 */
	void registerObjects(Object topaliObject, Vobject vamsasObject)
	{
		VorbaId id = vamsasObject.getVorbaId();
		System.out.print("Checking vorba id...");
		
		if (id == null)
		{
			System.out.println("is null");
			id = cdoc.registerObject(vamsasObject);
			System.out.println("id is now " + id);
		}
		else
			System.out.println("is " + id);
		
		hashTV.put(topaliObject, id);		
		hashVT.put(id, topaliObject);
		
		System.out.println("Added hashtable links for " + topaliObject);
	}
}