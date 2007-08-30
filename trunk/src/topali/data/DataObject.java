// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.PropertyChangeListener;
import java.util.*;

public abstract class DataObject
{
	static final Random random = new Random();
	
	protected LinkedList<PropertyChangeListener> changeListeners = new LinkedList<PropertyChangeListener>();
	
	//private int id;
	
	public DataObject() {
		//id = DataObject.random.nextInt(Integer.MAX_VALUE);
	}
	
	//public DataObject(int id) {	
	//	this.id = id;
	//}
	
	public void addChangeListener(PropertyChangeListener listener) {
		if(!changeListeners.contains(listener))
			this.changeListeners.add(listener);
	}
	
	public void removeChangeListener(PropertyChangeListener listener) {
		if(changeListeners.contains(listener))
			this.changeListeners.remove(listener);
	}

	//public int getId()
	//{
	//	return id;
	//}
	
	/**
	 * Just for castor, you should never ever call this method!
	 * @param id
	 */
	//public void setId(int id) {
	//	this.id = id;
	//}

//	@Override
//	public int hashCode()
//	{
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + (int) (id ^ (id >>> 32));
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		final DataObject other = (DataObject) obj;
//		if (id != other.id)
//			return false;
//		return true;
//	}
	
	
}
