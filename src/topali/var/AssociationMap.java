// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * A generic class to associate objects with eachother.
 * (As it just loops through lists, it's not very efficient on bigger datasets)
 * 
 * @param <T>
 */
public class AssociationMap<T> implements Serializable
{

	LinkedList<T> list1;
	LinkedList<T> list2;
	
	public AssociationMap() {
		list1 = new LinkedList<T>();
		list2 = new LinkedList<T>(); 
	}
	
	/**
	 * Removes all data
	 */
	public void clear() {
		list1.clear();
		list2.clear();
	}
	
	/**
	 * Add a new object association
	 * @param o1
	 * @param o2
	 */
	public void put(T o1, T o2) {
		boolean ok = true;
		LinkedList<T> tmp = get(o1);
		for(T t : tmp) {
			if(t.equals(o2)) {
				ok = false;
				break;
			}
		}
		if(ok) {
			tmp = get(o2);
			for(T t : tmp) {
				if(t.equals(o1)) {
					ok = false;
					break;
				}
			}
		}
		
		if(ok) {
			list1.add(o1);
			list2.add(o2);
		}
	}
	
	/**
	 * Remove all associations with object o
	 * @param o
	 */
	public void remove(T o) {
		for(int i=0; i<list1.size(); i++) {
			if(list1.get(i).equals(o) || list2.get(i).equals(o)) {
				list1.remove(i);
				list2.remove(i);
				i--;
			}
		}
	}
	
	/**
	 * Remove a certain association
	 * @param o1
	 * @param o2
	 */
	public void remove(T o1, T o2) {
		for(int i=0; i<list1.size(); i++) {
			if((list1.get(i).equals(o1) && list2.get(i).equals(o2)) || (list1.get(i).equals(o2) && list2.get(i).equals(o1))) {
				list1.remove(i);
				list2.remove(i);
				i--;
			}
		}
	}
	
	/**
	 * Get all direct associations with object o
	 * @param o
	 * @return
	 */
	public LinkedList<T> get(T o) {
		LinkedList<T> result = new LinkedList<T>();
		for(int i=0; i<list1.size(); i++) {
			if(list1.get(i).equals(o)) {
				result.add(list2.get(i));
			}
			else if(list2.get(i).equals(o)) {
				result.add(list1.get(i));
			}
		}
		return result;
	}
	
	/**
	 * Get all associations with object o,
	 * e. g. A -> B and B -> C calling 
	 * getAll(A) will return {B, C}.
	 * @param o
	 * @return
	 */
	public LinkedList<T> getAll(T o) {
		LinkedList<T> result = get(o);
		for(int i=0; i<result.size(); i++) {
			LinkedList<T> tmp = get(result.get(i));
			for(T e : tmp) {
				if(!result.contains(e) && !e.equals(o))
					result.add(e);
			}
		}
		return result;
	}
	
	/**
	 * Returns true if there are any associations with object o
	 * @param o
	 * @return
	 */
	public boolean contains(T o) {
		if(list1.contains(o) || list2.contains(o))
			return true;
		else
			return false;
	}

	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<list1.size(); i++) {
			sb.append(list1.get(i));
			sb.append(" <-> ");
			sb.append(list2.get(i));
			sb.append('\n');
		}
		return sb.toString();
	}
	
	
}
