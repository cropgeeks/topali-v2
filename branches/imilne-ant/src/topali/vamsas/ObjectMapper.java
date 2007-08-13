package topali.vamsas;

import java.util.*;

import org.apache.log4j.Logger;

import uk.ac.vamsas.client.*;

public class ObjectMapper
{
	Logger log = Logger.getLogger(this.getClass());
	
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
		if (hashTV.containsKey(topaliObject))
		{
			log.info("Found corresponding VAMSAS object for "+topaliObject+", it's "+hashTV.get(topaliObject));
			return cdoc.getObject(hashTV.get(topaliObject));
		}
		
		log.info("Didn't find corresponding VAMSAS object for "+topaliObject);
		return null;
	}
	
	/**
	 * Uses a vamsas data object and attempts to return the matching TOPALi
	 * object by matching the vamsas ID in the VT hashtable.
	 */
	Object getTopaliObject(Vobject vamsasObject)
	{
		// Does this vamsas object have a key (it won't if it's never been part
		// of the document, ie, only just been created)
		VorbaId id = vamsasObject.getVorbaId();
		if (id == null)
		{
			log.info("VAMSAS object "+vamsasObject+" is not registered. Registering now...");
			// Register the object for use within the session document
			cdoc.registerObject(vamsasObject);
			return null;
		}
		
		// If it does exist...do we have a mapping for it?
		return hashVT.get(id);
	}
	
	Object getTopaliObject(String vorbaId) {
		return hashVT.get(vorbaId);
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
			id = cdoc.registerObject(vamsasObject);
			log.info(vamsasObject+" is now registered, id is "+id);
		}
		else
			log.info(vamsasObject+" was already registered, id is "+id);
		
		hashTV.put(topaliObject, id);		
		hashVT.put(id, topaliObject);
		log.info("Updated hashtables for "+topaliObject);
	}
	
	public String getVorbaID(Object topaliObject) {
		Vobject vObj = getVamsasObject(topaliObject);
		if(vObj!=null)
			return vObj.getVorbaId().getId();
		else
			return null;
	}
}