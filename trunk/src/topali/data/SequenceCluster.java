// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.util.LinkedList;

// Simple class to represent a group of sequences that are clustered together.
public class SequenceCluster 
{
	private LinkedList<String> sequences = new LinkedList<String>();

	public SequenceCluster()
	{
	}

	public LinkedList<String> getSequences()
	{
		return sequences;
	}

	public void setSequences(LinkedList<String> sequences)
	{
		this.sequences = sequences;
	}

	public void addSequence(String sequence)
	{
		sequences.add(sequence);
	}

	// Does this cluster contain a sequence with the given name?
	public boolean contains(String name)
	{
		for (String seq : sequences)
			if (seq.equals(name))
				return true;

		return false;
	}

	public String getFirstSequence()
	{
		return sequences.get(0);
	}

	
	public String toString()
	{
		String eol = System.getProperty("line.separator");
		StringBuffer str = new StringBuffer(1000);

		for (String seq : sequences)
			str.append("  " + seq + eol);

		return str.toString();
	}
}