// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data.annotations;

import java.util.*;

public class AnnotationList extends ArrayList<Annotation>
{

	public AnnotationList()
	{
		super();
	}

	public AnnotationList(int initialCapacity)
	{
		super(initialCapacity);
	}

	public List<Annotation> getAnnotations(Class<? extends Annotation> type) {
		ArrayList<Annotation> result = new ArrayList<Annotation>();
		for(Annotation anno : this) {
			if(anno.getClass().equals(type))
				result.add(anno);
		}
		return result;
	}
	
}
