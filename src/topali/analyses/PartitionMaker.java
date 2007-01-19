// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import topali.data.*;

public class PartitionMaker
{
	private PartitionAnnotations pAnnotations;
	private int alignmentLength;
	
	private boolean discard = false;
	private int minLength = 75;
	
	public PartitionMaker(AlignmentData data)
	{
		pAnnotations = (PartitionAnnotations)data.getTopaliAnnotations().getAnnotations(PartitionAnnotations.class);
		pAnnotations.deleteAll();

		alignmentLength = data.getSequenceSet().getLength();
	}
	
	public void setDiscard(boolean discard)
		{ this.discard = discard; }
	public void setMinLength(int minLength)
		{ this.minLength = minLength; }
	
	// Partitions the graph by selecting midpoints between the peaks, so a
	// partition will go from start-midpoint1-midpoint2-end and so on
	public void autoPartition(float[][] data, float threshold)
	{
		boolean significant = data[0][1] > threshold;
		int start = 1, end = (int) data[0][0];
		
		int highStart = 1, highEnd = 1;
		
		// Zip along the windows...determining which ones are significant
		for (int i = 1; i < data.length; i++)
		{
			// Is this point above the threshold?
			boolean currentSig = (data[i][1] > threshold);			
			
			// If true, then the threshold has been crossed
			if (currentSig != significant || i == (data.length-1))
			{
				if (i == (data.length-1))
				{
					end = alignmentLength;
					addPartition(start, end);
				}
				
				// Has the graph just gone above the theshold?
				else if (currentSig == true)
				{
					highStart = (int) data[i][0];
				}
					
				// Or has it just gone back below, in which case, work out the
				// midpoint, and create a new partition up to that point
				else
				{
					highEnd = (int) data[i][0];
					end = highEnd - (int) ((highEnd-highStart) / 2f);
					
					addPartition(start, end);
						
					start = end + 1;
				}
			}
			
			significant = currentSig;
		}
	}
	
	private void addPartition(int start, int end)
	{
		int length = (end-start+1);
		if (discard && length < minLength)
		{
			// Do nothing
		}
		else
			//partitions.add(new Partition(start, end));
			pAnnotations.addRegion(start, end);
	}
	
	public void autoPartitionHMM(float[][] data1, float[][] data2, float[][] data3, float threshold)
	{	
//		int start = (int) data1[0][0], end = (int) data1[0][0];
		int start = 1, end = 0;
		
		int break1 = 1, break2 = -1;
		
		// What (initial) state are we in (0 indeterminate, or 1, 2, or 3)
		int state = 0;
		if (data1[0][1] >= threshold)
			state = 1;
		else if (data2[0][1] >= threshold)
			state = 2;
		else if (data3[0][1] >= threshold)
			state = 3;
		
		System.out.println("INITIAL STATE=" + state);
		
		for (int i = 1; i < data1.length; i++)
		{
			int newState = 0;
			
			if (data1[i][1] >= threshold)
				newState = 1;
			else if (data2[i][1] >= threshold)
				newState = 2;
			else if (data3[i][1] >= threshold)
				newState = 3;
			
			// Change...
			if (newState != state)
			{
				System.out.println("CHANGE AT " + data1[i][0] + " : " + newState);
				
				// Is this the start of a partition?
				if (break1 == -1)
				{
					break1 = (int) data1[i][0];
				}
				// Or the end of a partition?
				else
				{
					// Set the start = to 1 nt past the last partition
					start = end + 1;
					
					break2 = (int) data1[i][0];
					end = break2;// - (int) ((break2-break1) / 2f);
					
					System.out.println("ADDING PARTITION");
					addPartition(start, end);
					
					
					break1 = break2 = -1;
				}
			}

			state = newState;
		}
		
		// Add the final partition
		end = (int) data1[data1.length-1][0];
		addPartition(start, end);
	}
}