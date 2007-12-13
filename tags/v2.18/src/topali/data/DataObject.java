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
	private final static Random random = new Random();
	protected final int id;
	
	public DataObject() {
		id = DataObject.random.nextInt(Integer.MAX_VALUE);
	}
	
	public DataObject(int id) {
		if(id>-1)
			this.id = id;
		else
			this.id = DataObject.random.nextInt(Integer.MAX_VALUE);
	}
	
	public int getID() {
		return id;
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
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final DataObject other = (DataObject) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
}
