// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.beans.*;

// Class representing a single sequence.
public class Sequence extends DataObject
{
	// Sequence's actual name
	public String name;

	// Sequence's "safe" name - the name that can be safely passed to external
	// programs/libraries for processing (and won't break because of its length
	// or use of special characters
	public String safeName;

	// Sequence data
	private StringBuffer sequence;

	public Sequence()
	{
	}

	//public Sequence(int id) {
	//	super(id);
	//}
	
	public Sequence(String name, int length)
	{
		this.name = name;
		sequence = new StringBuffer(length);
	}

	public Sequence(String name)
	{
		this.name = name;
		sequence = new StringBuffer(50);
	}

	public String getSequence()
	{
		return sequence.toString();
	}

	public void setSequence(String sequence)
	{
		String oldValue = (this.sequence!=null) ? this.sequence.toString() : null;
		this.sequence = new StringBuffer(sequence);
		
		for(PropertyChangeListener l : changeListeners)
			l.propertyChange(new PropertyChangeEvent(this, "sequence", oldValue, sequence));
	}

	public String toString()
	{
		return name;
	}

	public int getLength()
	{
		return sequence.length();
	}

	// Returns a formatted version of the name that will fit within the given
	// number of characters. If the name is shorter than this width, then it
	// will be padded out with spaces
	/*
	 * public String getName(int width) { if (name.length() > width) return
	 * name.substring(0, width);
	 * 
	 * else if (name.length() == width) return name;
	 * 
	 * else { String str = name; for (int i = name.length(); i < width; i++) str += " ";
	 * return str; } }
	 */
	public StringBuffer getBuffer()
	{
		return sequence;
	}

	// "Pure" return of the data - this should be the only place where we do the
	// start-1 now, as all other methods are dealing with sequences that begin
	// at position 1 (rather than buffers beginning at 0)
	public String getPartition(int start, int end)
	{
		return sequence.substring(start - 1, end);
	}

	public boolean isEqualTo(Sequence seq)
	{
		if (sequence.toString().equals(seq.getBuffer().toString()))
			return true;
		return false;
	}

	public String formatName(int width, boolean leadingSpaces)
	{
		if (width <= name.length())
			return name.substring(0, width);

		if (leadingSpaces)
			return getSpacesForWidth(width) + name;
		else
			return name + getSpacesForWidth(width);
	}

	// Returns a String containing enough spaces to pad out this sequence's
	// name so that its name+spaces will equal the given width
	private String getSpacesForWidth(int width)
	{
		String str = "";
		for (int i = name.length(); i <= width; i++)
			str += " ";
		return str;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		String oldName = this.name;
		this.name = name;
		
		for(PropertyChangeListener l : changeListeners)
			l.propertyChange(new PropertyChangeEvent(this, "name", oldName, name));
	}
	
	public boolean matches(Sequence seq, boolean ignoreGaps) {
		String seq1 = ignoreGaps ? sequence.toString().replaceAll("-", "") : sequence.toString();
		String seq2 = ignoreGaps ? seq.getSequence().replaceAll("-", "") : seq.getSequence();
		boolean equalSeq = seq1.equals(seq2);
		boolean equalName = name.equals(seq.name);
		return equalSeq && equalName;
	}

//	@Override
//	public int hashCode()
//	{
//		final int PRIME = 31;
//		int result = 1;
//		result = PRIME * result + ((name == null) ? 0 : name.hashCode());
//		result = PRIME * result + ((safeName == null) ? 0 : safeName.hashCode());
//		result = PRIME * result + ((sequence == null) ? 0 : sequence.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj)
//	{
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		final Sequence other = (Sequence) obj;
//		if (name == null)
//		{
//			if (other.name != null)
//				return false;
//		} else if (!name.equals(other.name))
//			return false;
//		if (safeName == null)
//		{
//			if (other.safeName != null)
//				return false;
//		} else if (!safeName.equals(other.safeName))
//			return false;
//		if (sequence == null)
//		{
//			if (other.sequence != null)
//				return false;
//		} else if (!sequence.equals(other.sequence))
//			return false;
//		return true;
//	}

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Sequence))
			return false;
		
		Sequence seq = (Sequence)obj;
		try
		{
			return (seq.name.equals(name) && seq.sequence.toString().equals(sequence.toString()));
		} catch (RuntimeException e)
		{
			return false;
		}
	}
	
	
	
}
