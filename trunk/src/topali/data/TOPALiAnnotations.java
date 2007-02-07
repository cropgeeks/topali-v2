// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;

// Helper class for AlignmentAnnotations, providing code and methods to deal
// with annotations that would otherwise clog up the AlignmentData class
public class TOPALiAnnotations
{
	// A list (where each element is itself a list) of annotation wrappers
	private Vector<AlignmentAnnotations> annotations;
	
	public TOPALiAnnotations()
	{
		annotations = new Vector<AlignmentAnnotations>();
	}
	
	public TOPALiAnnotations(int alignmentLength)
	{
		annotations = new Vector<AlignmentAnnotations>();
	}
	
	public Vector<AlignmentAnnotations> getAnnotations()
		{ return annotations; }

	public void setAnnotations(Vector<AlignmentAnnotations> annotations)
		{ this.annotations = annotations; }
	
	public AlignmentAnnotations getAnnotations(Class type) {
		if(!AlignmentAnnotations.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Just subclasses of AlignmentAnnotations are allowed!");
		
		AlignmentAnnotations result = null;
		for(AlignmentAnnotations anno : annotations) {
			if(anno.getClass().equals(type)) {
				result = anno;
				break;
			}
		}
		
		if(result==null) {
			try {
				result = (AlignmentAnnotations)type.newInstance();
				annotations.add(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}