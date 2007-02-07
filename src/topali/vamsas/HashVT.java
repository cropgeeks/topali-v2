// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.vamsas;

import java.util.*;

public class HashVT
{
	// Holds mappings between VAMSAS vorba ID strings and their associated
	// TOPALi data objects.
	private Hashtable<String, Object> ht = new Hashtable<String, Object>();
	
	public void addReference(String id, Object obj)
	{
		ht.put(id, obj);
	}
}