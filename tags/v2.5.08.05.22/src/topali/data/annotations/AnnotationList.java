// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import java.util.*;

public class AnnotationList
{

	ArrayList<Annotation> annotations;
	
	boolean sorted = false;
	
	public AnnotationList()
	{
		annotations = new ArrayList<Annotation>();
	}

	public List<Annotation> getAnnotations(Class<? extends Annotation> type) {
		if(!sorted) {
			Collections.sort(annotations);
			sorted = true;
		}
		
		ArrayList<Annotation> result = new ArrayList<Annotation>();
		for(Annotation anno : annotations) {
			if(anno.getClass().equals(type))
				result.add(anno);
		}
		return result;
	}
	
	public void add(Annotation anno) {
		annotations.add(anno);
		sorted = false;
	}
	
	public void remove(Annotation anno) {
		annotations.remove(anno);
	}
	
	public void removeAll(Collection<Annotation> annos) {
		annotations.removeAll(annos);
	}
	
	public void removeAll(Class<? extends Annotation> type) {
		for(Iterator<Annotation> it = annotations.iterator(); it.hasNext(); ) {
			if(it.next().getClass().equals(type))
				it.remove();
		}
	}

	public ArrayList<Annotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(ArrayList<Annotation> annotations) {
		this.annotations = annotations;
	}
	
}
