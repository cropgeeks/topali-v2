// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.*;
/*
public class PartitionInfo
{
	// Reference to the currently selected partition within the alignment
	// Note that the "current" partition may be dynamically created - it does
	// not have to reside in the list below
	public Partition current;
	
	// A list of all partitions associated with the alignment
	public Vector<Partition> partitions = new Vector<Partition>();
	// Index of the selected partition from the PartitionDialog within this list
	public int selected = -1;
	
	// Should preview trees be drawn using these partitions?
	public boolean drawPreview = true;
	// Has the "too slow" warning been generated yet?
	public boolean slowWarningShown = false;
	
	public PartitionInfo()
	{
	}
	
	public void setCurrent(Partition current)
		{ this.current = current; }
	
	public Vector<Partition> getPartitions()
		{ return partitions; }
	
	public void setPartitions(Vector<Partition> partitions)
		{ this.partitions = partitions; }
	
	// Adds the Partition to the partitions list, ensuring that it is added at
	// the correct position so as to maintain a sorted list. This position is
	// then returned.
	public int addAndSortPartition(Partition newPartition)
	{
		for (int i = 0; i < partitions.size(); i++)
		{
			Partition p = partitions.get(i);
			
			if (p.s >= newPartition.s && p.e > newPartition.e)
			{
				partitions.add(i, newPartition);
				return i;
			}
		}
		
		// Otherwise add it at the end
		partitions.add(newPartition);
		return partitions.size()-1;
	}
	
	public boolean exists(Partition p2)
	{
		for (Partition p1: partitions)
			if (p1.s == p2.s && p1.e == p2.e)
				return true;
		
		return false;
	}
	
	public void createDefaultPartition(AlignmentData data)
	{
		int length = data.getSequenceSet().getLength();
		
		current = new Partition(1, length);
	}
	
	public void resetDefaultPartition(AlignmentData data)
	{
		System.out.println("reset");
		
		current.s = 1;
		current.e = data.getSequenceSet().getLength();
	}
	
	public void clear()
	{
		partitions.clear();
	}
}
*/