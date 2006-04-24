// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

// Tracks a list of partitions, and also a separate "current" partition which
// can be the highlighted region on a graph *without* actually being part of the
// partitions list that the user sees.
public class PartitionAnnotations extends RegionAnnotations
{
	// The alignment positions of the currently highlighted partition
	private int currentStart, currentEnd;
	
	private int alignmentLength;
	
	public PartitionAnnotations()
	{
		// Empty constructor for Castor XML
	}
	
	public PartitionAnnotations(int alignmentLength)
	{
		this.alignmentLength = alignmentLength;
		label = "TOPALi Partition Annotations";
		
		resetCurrentPartition();
	}
	
	public int getCurrentStart()
		{ return currentStart; }
	public void setCurrentStart(int currentStart)
		{ this.currentStart = currentStart; }
	
	public int getCurrentEnd()
		{ return currentEnd; }
	public void setCurrentEnd(int currentEnd)
		{ this.currentEnd = currentEnd; }
	
	public int getAlignmentLength()
		{ return alignmentLength; }
	public void setAlignmentLength(int alignmentLength)
		{ this.alignmentLength = alignmentLength; }	
	
	public void setCurrentPartition(int currentStart, int currentEnd)
	{
		this.currentStart = currentStart;
		this.currentEnd = currentEnd;
	}
	
	public void resetCurrentPartition()
	{
		currentStart = 1;
		currentEnd   = alignmentLength;
	}
	
	protected AnnotationElement create(int position)
	{
		return new AnnotationElement(AnnotationElement.PARTITION, position);
	}
}