// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.PropertyChangeListener;
import java.util.*;

public abstract class DataObject 
{
	protected LinkedList<PropertyChangeListener> changeListeners = new LinkedList<PropertyChangeListener>();
	
	public DataObject() {
	}
	
	public void addChangeListener(PropertyChangeListener listener) {
		if(!changeListeners.contains(listener))
			this.changeListeners.add(listener);
	}
	
	public void removeChangeListener(PropertyChangeListener listener) {
		if(changeListeners.contains(listener))
			this.changeListeners.remove(listener);
	}

	@Override
	public int hashCode()
	{
		return 1;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (getClass() != obj.getClass())
			return false;
		return true;
	}
	
}
