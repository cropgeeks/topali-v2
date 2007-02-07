// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.util.*;

import uk.ac.vamsas.objects.core.*;

public class HashTV
{
	// Holds mappings between TOPALi data objects and their associated VAMSAS
	// vorba ID strings.
	private Hashtable<Object, String> ht = new Hashtable<Object, String>();
	
	
	public void addReference(Object obj, String id)
	{
		ht.put(obj, id);
	}
	
	public String getVorbaID(Object obj)
	{
		if (ht.containsKey(obj))
		{
			return ht.get(obj);
		}
		else
		{
			return null;
		}
	}
	
	public AlignmentSequence getAlignmentSequence(Object tRef)
	{
		if (ht.containsKey(tRef))
		{
			return null;
		}
		else
		{
			AlignmentSequence v = new AlignmentSequence();
//			addReference(tRef, BUT CAN'T GET VORBA ID UNTIL ADDED TO DOC!?
			return v;
		}
	}
	
	// TODO: getAlignmentSequence(TOPALiObjectRef)
	//   gets string for vamsas obj and finds it in client doc
	// OR
	//   doesn't find it, so makes a new instance instead, adding the reference
	//   to the hashtable at the same time
}