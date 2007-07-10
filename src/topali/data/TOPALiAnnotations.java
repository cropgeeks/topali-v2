// (C) 2003-2006 Iain Milne
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
		annotations = new Vector<AlignmentAnnotations>(2);
	}
	
	public TOPALiAnnotations(int alignmentLength)
	{
		annotations = new Vector<AlignmentAnnotations>(2);
		
		// Create a set of partition annotations
		PartitionAnnotations pAnnotations = new PartitionAnnotations(alignmentLength);
		// And add them as the first element of the list
		annotations.add(pAnnotations);
	}
	
	public Vector<AlignmentAnnotations> getAnnotations()
		{ return annotations; }

	public void setAnnotations(Vector<AlignmentAnnotations> annotations)
		{ this.annotations = annotations; }
	
	// Returns the PartitionAnnotations (element 0 in the list)
	public PartitionAnnotations getPartitionAnnotations()
	{
		return (PartitionAnnotations) annotations.get(0);
	}
}