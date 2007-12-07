// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import static topali.fileio.AlignmentLoadException.*;

import java.awt.Color;
import java.beans.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import pal.alignment.*;
import pal.datatype.*;
import pal.gui.NameColouriser;
import pal.misc.Identifier;
import topali.analyses.SequenceSetUtils;
import topali.fileio.*;

// Class representing a set of sequences (an alignment).
public class SequenceSet extends DataObject
{

	// Actual alignment data
	private Vector<Sequence> sequences = new Vector<Sequence>();

	private int length;

	private String overview;

	// Tracks currently selected sequences
	private int[] selectedSeqs = new int[0];

	// Various parameters related to this alignment that must be computed
	private SequenceSetParams params = null;
	
	// Runtime only objects (ie, not saved to XML)
	private NameColouriser nameColouriser;

	public SequenceSet()
	{
		params = new SequenceSetParams();
	}

	public SequenceSet(SequenceSet ss) {
		this();
		this.length=ss.length;
		this.overview=ss.overview;
		this.selectedSeqs=ss.selectedSeqs.clone();
		this.params = new SequenceSetParams(ss.params);
		for(int i=0; i<ss.sequences.size(); i++) {
			addSequence(new Sequence(ss.getSequence(i)));
		}
	}
	
	public SequenceSet(int id)
	{
		super(id);
	}

	// Creates a new SequenceSet object using a PAL alignment as input
	public SequenceSet(Alignment alignment) throws AlignmentLoadException
	{
		this();
		
		for (int i = 0; i < alignment.getSequenceCount(); i++)
		{
			Sequence seq = new Sequence(alignment.getIdentifier(i).getName());
			seq.setSequence(alignment.getAlignedSequenceString(i));

			addSequence(seq);
		}

		checkValidity();
	}

	// Creates a new SequenceSet object by loading it in from disk
	public SequenceSet(File filename) throws AlignmentLoadException
	{
		this();
		
		AlignmentHandler ah = new AlignmentHandler(this);
		ah.openAlignment(filename);

		checkValidity();
	}

	// Creates a new SequenceSet by loading it from disk and (optionally)
	// ignores checking to see whether the file is aligned or not
	// (allows for non-aligned datasets to be held)
	public SequenceSet(File filename, boolean isAligned)
			throws AlignmentLoadException
	{
		this();
		
		AlignmentHandler ah = new AlignmentHandler(this);
		ah.openAlignment(filename);

		this.params.setAligned(isAligned);
		checkValidity();
	}

	/* Returns the length of this alignment. */
	public int getLength()
	{
		return length;
	}

	public void setLength(int length)
	{
		this.length = length;
	}

	/* Returns the sequences in this alignment. */
	public Vector<Sequence> getSequences()
	{
		return sequences;
	}

	public void setSequences(Vector<Sequence> sequences)
	{
		this.sequences = sequences;
	}

	/* Returns the ClustalX-like overview string. */
	public String getOverview()
	{
		// Additional check because when Castor outputs a string containing all
		// white space then it gets saved as an empty string
		if (overview==null || overview.length() == 0)
			calculateOverview();

		return overview;
	}

	public void setOverview(String overview)
	{
		this.overview = overview;
	}

	/* Returns whether this alignment is DNA (true) or Protein (false). */
	public boolean isDNA()
	{
		return params.isDNA();
	}

	public void setIsDNA(boolean isDNA)
	{
		this.params.setDNA(isDNA);
	}

	/* Returns the array of selected sequences information. */
	public int[] getSelectedSequences()
	{
		return selectedSeqs;
	}

	public void setSelectedSequences(int[] indices)
	{
		selectedSeqs = indices;
	}

	public void addSequence(Sequence sequence)
	{
		sequences.add(sequence);
		length = sequence.getLength();

		// Convert the sequence to all UPPER case
		String seq = sequence.getBuffer().toString().toUpperCase();
		sequence.getBuffer().replace(0, seq.length(), seq);

		// Give the sequence a safe name to work with
		DecimalFormat d = new DecimalFormat("00000");
		sequence.safeName = "SEQ" + d.format(sequences.size());
		
		for(PropertyChangeListener li : changeListeners) {
			li.propertyChange(new PropertyChangeEvent(this, "addSequence", null, sequence));
		}
	}

	/* Returns the sequence at the given index position. */
	public Sequence getSequence(int index)
	{
		return sequences.get(index);
	}

	/* Returns the number of sequences in this alignment. */
	public int getSize()
	{
		return sequences.size();
	}

	/*
	 * Returns an array of Sequence objects that match the sequences at the
	 * given indices.
	 */
	public Sequence[] getSequencesArray(int[] indices)
	{
		Sequence[] seqs = new Sequence[indices.length];
		for (int i = 0; i < indices.length; i++)
			seqs[i] = sequences.get(indices[i]);

		return seqs;
	}

	public String getNameForSafeName(String safeName)
	{
		for (Sequence seq : sequences)
			if (seq.safeName.equals(safeName))
				return seq.getName();

		return "";
	}

	/* Returns true if this alignment has had its parameters estimated. */
	public boolean hasParametersEstimated()
	{
		return !params.isNeedCalculation();
	}

	public SequenceSetParams getParams()
	{
		return params;
	}

	public void setParams(SequenceSetParams params)
	{
		this.params = params;
	}

	
	public String getCodonUsage()
	{
		return params.getCodonUsage();
	}

	public void setCodonUsage(String codonUsage)
	{
		this.params.setCodonUsage(codonUsage);
	}

	/* Returns the current index of the Sequence with the given name. */
	public int getIndexOf(String name, boolean matchCase)
	{
		int index = 0;
		for (Sequence seq : sequences)
		{
			if ((matchCase && seq.getName().equals(name))
					|| (!matchCase && seq.getName().equalsIgnoreCase(name)))
				return index;
			else
				index++;
		}

		return -1;
	}

	/* Returns an integer array containing the indices off all sequences. */
	public int[] getAllSequences()
	{
		int[] indices = new int[sequences.size()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;

		return indices;
	}

	public void reset()
	{
		sequences = new Vector<Sequence>();
		length = 0;
	}

	/* Performs a number of checks to ensure this sequence set is valid. */
	public void checkValidity() throws AlignmentLoadException
	{
		// 1) Were any sequences even loaded?
		if (sequences.size() == 0)
			throw new AlignmentLoadException(NO_SEQUENCES);

		// 2) Remove any space characters
		for (Sequence seq : sequences)
		{
			StringBuffer buffer = seq.getBuffer();
			for (int j = buffer.length() - 1; j >= 0; j--)
			{
				char c = buffer.charAt(j);
				if (c == ' ')
					buffer.deleteCharAt(j);
			}
		}

		// 3) Is it a proper alignment, (are all sequences of the same length?)
		// (ignore check if we're not expecting an alignment)
		if (params.isAligned())
		{
			int size = sequences.get(0).getLength();
			for (Sequence seq : sequences)
				if (seq.getLength() != size)
				{
					//System.out.println(seq.getName() + " : " + seq.getLength());
					throw new AlignmentLoadException(NOT_ALIGNED);
				}
		}

		// 4) Check for duplicate names
		ListIterator<Sequence> itor1 = sequences.listIterator(0);
		for (int i = 0; itor1.hasNext(); i++)
		{
			String iName = itor1.next().getName();

			ListIterator<Sequence> itor2 = sequences.listIterator(i + 1);
			while (itor2.hasNext())
			{
				String jName = itor2.next().getName();
				if (iName.equals(jName))
				{
					//System.out.println(jName);
					throw new AlignmentLoadException(DUPLICATE_NAMES_FOUND);
				}
			}
		}

		length = (sequences.get(0)).getLength();

		// if (resetSelection)
		params.setDNA(SequenceSetUtils.isSequenceDNA(this)); 

		if (params.isAligned())
			calculateOverview();

		/*
		 * 
		 * nucStart = nStart = 1; nucEnd = nEnd = nMax = seqLength;
		 *  // Reset transition/transvertion ratio and alpha shape parameter
		 * tRatio = alpha = -1; // Reset frequencies freqs = null;
		 */
		// if (resetSelection)
		{
			selectedSeqs = new int[sequences.size()];
			for (int i = 0; i < sequences.size(); i++)
				selectedSeqs[i] = i;
		}
	}

	/*
	 * Rearranges the sequences Vector by moving one or more sequences either up
	 * down, or to the top, of the Vector.
	 */
	public void moveSequences(int[] indices, boolean up, boolean top)
	{
		Sequence[] seqs = new Sequence[indices.length];
		// Find them...
		for (int i = 0; i < seqs.length; i++)
			seqs[i] = sequences.get(indices[i]);

		// Move them...
		if (up)
		{
			for (int i = 0; i < seqs.length; i++)
			{
				sequences.remove(seqs[i]);
				sequences.add(indices[i] - 1, seqs[i]);
			}
		} else
		{
			for (int i = seqs.length - 1; i >= 0; i--)
			{
				sequences.remove(seqs[i]);
				if (top)
					sequences.add(0, seqs[i]);
				else
					sequences.add(indices[i] + 1, seqs[i]);
			}
		}
	}

	/* Returns a PAL alignment version of this alignment */
	public SimpleAlignment getAlignment(boolean useSafeNames)
	{
		return getAlignment(getAllSequences(), 1, length, useSafeNames);
	}

	public SimpleAlignment getAlignment(int start, int end, boolean useSafeNames)
	{
		return getAlignment(getAllSequences(), start, end, useSafeNames);
	}

	/*
	 * Returns a PAL alignment version of this alingment, containing only the
	 * designated sequences, and partition from start to end.
	 */
	public SimpleAlignment getAlignment(int[] indices, int start, int end,
			boolean useSafeNames)
	{
		// Determine names
		Identifier[] ids = new Identifier[indices.length];
		for (int i = 0; i < indices.length; i++)
			if (useSafeNames)
				ids[i] = new Identifier(getSequence(indices[i]).safeName);
			else
				ids[i] = new Identifier(getSequence(indices[i]).getName());

		// Determine data
		String[] seqs = new String[indices.length];
		for (int i = 0; i < indices.length; i++)
			seqs[i] = getSequence(indices[i]).getPartition(start, end);

		// Create a PAL alignment
		if (params.isDNA())
			return new SimpleAlignment(ids, seqs, new Nucleotides());
		else
			return new SimpleAlignment(ids, seqs, new AminoAcids());
	}
	
	// Returns (or computes and returns) a PAL object that can be used to colour
	// sequence labels in trees
	public NameColouriser getNameColouriser(int colorSeed)
	{
		// If it's already been initialized, just return it
		if (nameColouriser != null)
			return nameColouriser;

		// Otherwise, make it...
		nameColouriser = new NameColouriser();

		// ...and define it
		int i = 0;
		for (Sequence seq : sequences)
		{
			Random r = new Random(colorSeed + 1253 + (i++));
			Color c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));

			nameColouriser.addMapping(seq.getName(), c);
		}

		return nameColouriser;
	}

	/*
	 * Scans all sequences and produces a ClustalX-like string that shows
	 * changes between the strains.
	 */
	private void calculateOverview()
	{
		StringBuffer buffer = new StringBuffer(length);

		boolean star = true;
		for (int i = 0; i < length; i++, star = true)
		{
			char c = sequences.get(0).getBuffer().charAt(i);
			// (Iterator rather than foreach so we can start at 1)
			ListIterator<Sequence> itor = sequences.listIterator(1);
			while (itor.hasNext())
			{
				if (itor.next().getBuffer().charAt(i) != c)
				{
					star = false;
					break;
				}
			}

			if (star)
				buffer.append("*");
			else
				buffer.append(" ");
		}

		overview = buffer.toString();
	}

	public void save(File file, int format, boolean useSafeNames)
			throws Exception
	{
		save(file, getAllSequences(), 1, length, format, useSafeNames);
	}

	public void save(File file, int[] sequences, int format,
			boolean useSafeNames) throws Exception
	{
		save(file, sequences, 1, length, format, useSafeNames);
	}

	public void save(File file, int[] sequences, int start, int end,
			int format, boolean useSafeNames) throws Exception
	{
		AlignmentHandler ah = new AlignmentHandler(this);
		ah.save(file, sequences, start, end, format, useSafeNames);
	}

	// Returns an array containing the (safe) names of selected sequences
	public String[] getSelectedSequenceSafeNames()
	{
		String[] names = new String[selectedSeqs.length];
		for (int i = 0; i < names.length; i++)
			names[i] = sequences.get(selectedSeqs[i]).safeName;

		return names;
	}

	public String[] getAllSequenceSafeNames()
	{
		String[] names = new String[sequences.size()];
		for (int i = 0; i < names.length; i++)
			names[i] = sequences.get(i).safeName;

		return names;
	}

	// Creates a selectedSeqs index array by matching the safe names from the
	// string array with the actual sequences
	// Used by certain jobs to get a set of selected sequences for a SequenceSet
	// instance without changing the instance's actual selection state.
	// Mainly because the ss's state at the time of processing may have changed
	// since it was submitted
	public int[] getIndicesFromNames(String[] names)
	{
		int[] indices = new int[names.length];

		for (int i = 0; i < names.length; i++)
		{
			for (int j = 0; j < sequences.size(); j++)
				if (sequences.get(j).safeName.equals(names[i]))
				{
					indices[i] = j;
					break;
				}
		}

		return indices;
	}

	public boolean containsStopCodons() {
		for(Sequence s : sequences) {
			if(s.containsStopCodons())
				return true;
		}
		return false;
	}
	
	public boolean isCodons() {
		if(!isDNA())
			return false;
		
		if(sequences.size()>0)
			return sequences.get(0).isCodons();
		
		return false;
	}
	
	public void merge(SequenceSet set) {
		for(Sequence s : set.getSequences()) {
			boolean found = false;
			for(Sequence thisS : getSequences()) {
				if(thisS.getID()==s.getID()) {
					thisS.setName(s.getName());
					thisS.setSequence(s.getSequence());
					found = true;
				}
			}
			if(!found) {
				addSequence(s);
			}
		}
	}
	
	@Override
	public void addChangeListener(PropertyChangeListener listener)
	{
		super.addChangeListener(listener);
		for(Sequence s : sequences)
			s.addChangeListener(listener);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result;
		for(Sequence s : sequences)
			result += s.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SequenceSet other = (SequenceSet) obj;
		if (sequences == null)
		{
			if (other.sequences != null)
				return false;
		} else if (!sequences.equals(other.sequences))
			return false;
		return true;
	}
	
	
}
