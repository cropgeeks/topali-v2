// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

// Tracks a list of partitions, and also a separate "current" partition which
// can be the highlighted region on a graph *without* actually being part of the
// partitions list that the user sees.
public class PartitionAnnotations extends RegionAnnotations
{
	
	private int alignmentLength;

	public PartitionAnnotations()
	{
		// Empty constructor for Castor XML
		label = "TOPALi Partitions";
	}
	
	public PartitionAnnotations(int alignmentLength)
	{
		super();
		this.alignmentLength = alignmentLength;
	}
	
	
	public int getAlignmentLength()
		{ return alignmentLength; }
	public void setAlignmentLength(int alignmentLength)
		{ this.alignmentLength = alignmentLength; }	
	
	protected AnnotationElement create(int position)
	{
		return new AnnotationElement(AnnotationElement.PARTITION, position);
	}
}