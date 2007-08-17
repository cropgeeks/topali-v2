// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.PropertyChangeListener;
import java.util.LinkedList;

public abstract class ViewableDataObject
{

	protected LinkedList<PropertyChangeListener> changeListeners = new LinkedList<PropertyChangeListener>();
	
	private String id;
	
	public ViewableDataObject() {
		
	}
	
	public void addChangeListener(PropertyChangeListener listener) {
		if(!changeListeners.contains(listener))
			this.changeListeners.add(listener);
	}
	
	public void removeChangeListener(PropertyChangeListener listener) {
		if(changeListeners.contains(listener))
			this.changeListeners.remove(listener);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}
	
	
}
