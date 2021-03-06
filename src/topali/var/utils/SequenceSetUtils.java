// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.utils;

import java.util.*;

import javax.swing.JOptionPane;

import pal.alignment.SimpleAlignment;
import scri.commons.gui.MsgBox;
//import sun.print.resources.serviceui;
import topali.analyses.ParamEstimateThread;
import topali.data.*;
import topali.data.annotations.Annotation;
import topali.gui.*;
import topali.i18n.Text;

/*
 * Helper class that performs various analyses on a SequenceSet (Alignment)
 * object.
 */
public class SequenceSetUtils
{
	private static Random random = new Random();

	private static Hashtable<String, String> geneticCode;
	static {
		geneticCode = new Hashtable<String, String>();
		geneticCode.put("GCU", "A");
		geneticCode.put("GCC", "A");
		geneticCode.put("GCA", "A");
		geneticCode.put("GCG", "A");

		geneticCode.put("CGU", "R");
		geneticCode.put("CGC", "R");
		geneticCode.put("CGA", "R");
		geneticCode.put("CGG", "R");
		geneticCode.put("AGA", "R");
		geneticCode.put("AGG", "R");

		geneticCode.put("AAU", "N");
		geneticCode.put("AAC", "N");

		geneticCode.put("GAU", "D");
		geneticCode.put("GAC", "D");

		geneticCode.put("UGU", "C");
		geneticCode.put("UGC", "C");

		geneticCode.put("CAA", "Q");
		geneticCode.put("CAG", "Q");

		geneticCode.put("CAA", "Q");
		geneticCode.put("CAG", "Q");

		geneticCode.put("GAA", "E");
		geneticCode.put("GAG", "E");

		geneticCode.put("GGU", "G");
		geneticCode.put("GGC", "G");
		geneticCode.put("GGA", "G");
		geneticCode.put("GGG", "G");

		geneticCode.put("CAU", "H");
		geneticCode.put("CAC", "H");

		geneticCode.put("AUU", "I");
		geneticCode.put("AUC", "I");
		geneticCode.put("AUA", "I");

		geneticCode.put("UUA", "L");
		geneticCode.put("UUG", "L");
		geneticCode.put("CUU", "L");
		geneticCode.put("CUC", "L");
		geneticCode.put("CUA", "L");
		geneticCode.put("CUG", "L");

		geneticCode.put("AAA", "K");
		geneticCode.put("AAG", "K");

		geneticCode.put("AUG", "M");

		geneticCode.put("UUU", "F");
		geneticCode.put("UUC", "F");

		geneticCode.put("CCU", "P");
		geneticCode.put("CCC", "P");
		geneticCode.put("CCA", "P");
		geneticCode.put("CCG", "P");

		geneticCode.put("UCU", "S");
		geneticCode.put("UCC", "S");
		geneticCode.put("UCA", "S");
		geneticCode.put("UCG", "S");
		geneticCode.put("AGU", "S");
		geneticCode.put("AGC", "S");

		geneticCode.put("ACU", "T");
		geneticCode.put("ACC", "T");
		geneticCode.put("ACA", "T");
		geneticCode.put("ACG", "T");

		geneticCode.put("UGG", "W");

		geneticCode.put("UAU", "Y");
		geneticCode.put("UAC", "Y");

		geneticCode.put("GUU", "V");
		geneticCode.put("GUC", "V");
		geneticCode.put("GUA", "V");
		geneticCode.put("GUG", "V");
	}

	/*
	 * Counts how many duplicate sequences were found when running
	 * getUniqueSequences()
	 */
	public static int duplicateCount = 0;

	public static int[] getUniqueSequences(SequenceSet ss)
	{
		// keepFirstDuplicate is always passed in as true
		// setting it to false would only pick truly unique seqs
		boolean keepFirstDuplicate = true;

		int[] indices = new int[ss.getSize()];
		for (int i = 0; i < indices.length; i++)
			indices[i] = i;

		int count = ss.getSize();
		duplicateCount = 0;

		ListIterator<Sequence> itor = ss.getSequences().listIterator(0);
		for (int i = 0; i < indices.length; i++)
		{
			StringBuffer buffer = (itor.next()).getBuffer();

			ListIterator<Sequence> jtor = ss.getSequences().listIterator(0);
			for (int j = 0; j < indices.length; j++)
			{
				StringBuffer toCompare = (jtor.next()).getBuffer();

				if (i == j)
					continue;

				if (buffer.toString().equals(toCompare.toString()))
				{
					// Ensures only first instance of a duplicate gets added
					if ((keepFirstDuplicate && j < i) || !keepFirstDuplicate)
					{
						count--;
						indices[i] = -1;
					} else
						duplicateCount++;

					break;
				}
			}
		}

		int[] finalIndices = new int[count];
		for (int i = 0, c = 0; i < indices.length; i++)
			if (indices[i] != -1)
				finalIndices[c++] = indices[i];

		//System.out.println("DUPLICATE=" + duplicateCount);

		return finalIndices;
	}

	/* Prompts to change the name of the currently selected sequence. */
	// TODO: Put this in its own dialog so we can have a Help button
	public static boolean renameSequence(SequenceSet ss)
	{
		int index = ss.getSelectedSequences()[0];
		Sequence selected = ss.getSequence(index);
		String newname = null;

		boolean ok = false;
		while (!ok)
		{
			// Prompt for the new sequence name
			newname = (String) JOptionPane.showInputDialog(TOPALi.winMain,
					Text.get("SequenceSetUtils.gui01"),
					Text.get("SequenceSetUtils.gui02"),
					JOptionPane.PLAIN_MESSAGE, null, null, selected.getName());

			if (newname == null)
				return false;

			if (newname.length() == 0)
				return false;

			// Check for invalid characters
			String verified = verifyName(newname, index + 1);
			if (verified.equals(newname) == false)
			{
				MsgBox.msg(Text.get("SequenceSetUtils.err01"),
						MsgBox.ERR);
				continue;
			}

			// Check for it being unique
			ok = true;
			for (Sequence seq : ss.getSequences())
			{
				if (seq != selected && seq.getName().equals(newname))
				{
					String msg = Text.get("SequenceSetUtils.err02", newname);
					MsgBox.msg(msg, MsgBox.ERR);

					ok = false;
					break;
				}
			}
		}

		selected.setName(newname);
		return true;
	}

	public static void estimateParameters(SequenceSet ss)
	{
		int[] indices = getSequencesForParamEstimation(ss);

		SimpleAlignment alignment = ss.getAlignment(indices, 1, ss.getLength(),
			true);

		ParamEstimateThread pest = new ParamEstimateThread(alignment);
		pest.go();

		SequenceSetProperties params = ss.getProps();

		if(params.isNucleotides()) {
			params.setAlpha(pest.getAlpha());
			params.setKappa(pest.getKappa());
			params.setAvgDist(pest.getAvgDistance());
			params.setFreqs(pest.getFreqs());
			params.setTRatio(pest.getRatio());
		}

		if(!pest.wasCancelled())
		    params.needsCalculation(false);

		//WinMainMenuBar.aFileSave.setEnabled(true);
		//WinMainMenuBar.aVamCommit.setEnabled(true);
		ProjectState.setDataChanged();
	}

	/*
	 * Determines which set of sequences should be used when estimating the
	 * parameter values (alpha, tRatio, etc) for the alignment. If less than 50,
	 * then all sequences are used, otherwise the user can choose to still use
	 * all the sequences, or a randomly selected group of 50.
	 */
	public static int[] getSequencesForParamEstimation(SequenceSet ss)
	{
		int[] indices = ss.getAllSequences();

		if (ss.getSize() > 50)
		{
			// Ask the user
			String msg = Text.get("SequenceSetUtils.msg01");
			int option = MsgBox.yesno(msg, 0);

			// And return all the indices...
			if (option != JOptionPane.YES_OPTION)
				return indices;

			// Or just a random 50
			indices = new int[50];
			for (int i = 0; i < 50; i++)
			{
				while (true)
				{
					boolean ok = true;
					int seq = (int) (Math.random() * ss.getSize());
					for (int j = 0; j < 50; j++)
						if (indices[j] == seq)
						{
							ok = false;
							break;
						}

					if (ok)
					{
						indices[i] = seq;
						break;
					}
				}
			}
		}

		return indices;
	}

	// Scans through every sequence in the alignment and checks each sequence's
	// name to ensure it does not contain any invalid characters.
	public static boolean verifySequenceNames(SequenceSet ss)
	{
		int seqID = 0;
		boolean ok = true;

		for (Sequence seq : ss.getSequences())
		{
			String oldName = seq.getName();
			String newName = verifyName(seq.getName(), ++seqID);

			if (oldName.equals(newName) == false)
			{
				ok = false;
				seq.setName(newName);
			}
		}

		return ok;
	}

	// Checks a sequence name to ensure it does not contain illegal characters.
	// Returns the name, potentially modified to remove the illegal characters.
	private static String verifyName(String name, int seqID)
	{
		name = name.replace(":", "#" + seqID);
		name = name.replace("(", "#" + seqID);
		name = name.replace(")", "#" + seqID);
		name = name.replace("[", "#" + seqID);
		name = name.replace("]", "#" + seqID);
		name = name.replace("{", "#" + seqID);
		name = name.replace("}", "#" + seqID);
		name = name.replace(";", "#" + seqID);
		name = name.replace(" ", "#" + seqID);

		return name;
	}

	public static boolean canRunPDM(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 4)
		{
			String msg = Text.get("SequenceSetUtils.msg05");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	public static boolean canRunHMM(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length != 4)
		{
			String msg = Text.get("SequenceSetUtils.msg03");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	public static boolean canRunDSS(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 3)
		{
			String msg = Text.get("SequenceSetUtils.msg02");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	public static boolean canRunLRT(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 3)
		{
			String msg = Text.get("SequenceSetUtils.msg06");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	public static boolean canRunCodeML(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 3)
		{
			String msg = Text.get("SequenceSetUtils.msg08");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	public static boolean canRunCodonW(SequenceSet ss)
	{
		if (ss.getProps().isNucleotides() == false)
		{
			String msg = Text.get("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);

			return false;
		}

		return true;
	}

	// Creates and returns a new SequenceSet using the existing SequenceSet from
	// the alignment data. This method will *only* use sequences and partitions
	// that have been selected by the user
	// Currently only called by the Sequence->Export code
	public static SequenceSet getConcatenatedSequenceSet(AlignmentData data,
			RegionAnnotations annotations, int[] seqs, int[] regions)
	{
		SequenceSet ssOld = data.getSequenceSet();
		SequenceSet ssNew = new SequenceSet();

		// For each sequence we want to add...
		for (int seqIndex : seqs)
		{
			// Create (and add) the sequence to the set
			Sequence seqOld = ssOld.getSequence(seqIndex);
			Sequence seqNew = new Sequence(seqOld.getName());

			// Concatenate and add each partition to the sequence's data
			StringBuffer buffer = seqNew.getBuffer();

			// We either want to concatenate together the regions...
			if (regions.length > 0)
			{
				for (int r : regions)
				{
					RegionAnnotations.Region reg = annotations.get(r);
					buffer.append(seqOld.getPartition(reg.getS(), reg.getE()));
				}
			}
			// Or just make a new alignment that contains the same as before
			else
				buffer.append(seqOld.getSequence());

			ssNew.addSequence(seqNew);
		}

		return ssNew;
	}

	public static SequenceSet getConcatenatedSequenceSet(AlignmentData data, int[] seqs, List<Annotation> annos)
	{
		SequenceSet ssOld = data.getSequenceSet();
		SequenceSet ssNew = new SequenceSet();

		// For each sequence we want to add...
		for (int seqIndex : seqs)
		{
			// Create (and add) the sequence to the set
			Sequence seqOld = ssOld.getSequence(seqIndex);
			Sequence seqNew = new Sequence(seqOld.getName());

			// Concatenate and add each partition to the sequence's data
			StringBuffer buffer = seqNew.getBuffer();

			// We either want to concatenate together the regions...
			if (annos!=null && annos.size() > 0)
			{
				for(Annotation anno: annos) {
					buffer.append(seqOld.getPartition(anno.getStart(), anno.getEnd()));
				}
			}
			// Or just make a new alignment that contains the same as before
			else
				buffer.append(seqOld.getSequence());

			ssNew.addSequence(seqNew);
		}

		return ssNew;
	}

	public static SequenceSet getCodonPosSequenceSet(AlignmentData data, int codonPos, int[] seqs) {
		SequenceSet ssOld = data.getSequenceSet();
		SequenceSet ssNew = new SequenceSet();

		// For each sequence we want to add...
		for (int seqIndex : seqs)
		{
			// Create (and add) the sequence to the set
			Sequence seqOld = ssOld.getSequence(seqIndex);
			Sequence seqNew = new Sequence(seqOld.getName());
			StringBuffer buffer = seqNew.getBuffer();
			for(int i=codonPos-1; i<seqOld.getLength(); i+=3) {
				buffer.append(seqOld.getBuffer().charAt(i));
			}
			ssNew.addSequence(seqNew);
		}

		return ssNew;
	}

	public static int determineSeqType(SequenceSet ss) {
		return determineSeqType(ss, 0, ss.getLength()-1);
	}

	public static int determineSeqType(SequenceSet ss, int start, int end) {
		int a=0, c=0, g=0, t=0, u=0, n=0, aa=0;
		int count = 0;

		Vector<Sequence> sequences = ss.getSequences();
		for (Sequence seq : sequences)
		{
			StringBuffer buffer = seq.getBuffer();

			for (int ch = start; ch <= end && ch < buffer.length(); ch++)
			{
				count++;

				switch (buffer.charAt(ch))
				{
				case 'A':
					a++;
					break;
				case 'C':
					c++;
					break;
				case 'G':
					g++;
					break;
				case 'T':
					t++;
					break;
				case 'U':
					u++;
					break;
				case 'N':
					n++;
					break;

				case 'R':
				case 'D':
				case 'E':
				case 'Q':
				case 'H':
				case 'I':
				case 'L':
				case 'K':
				case 'M':
				case 'F':
				case 'P':
				case 'S':
				case 'W':
				case 'Y':
				case 'V':
					aa++;
					break;

				default:
					count--;
					break;
				}
			}
		}

		boolean rna = (u>0) && (((double)(a+c+g+t+u+n)/(double)count)>0.9);
		boolean dna = (u==0) && (((double)(a+c+g+t+u+n)/(double)count)>0.9);
		boolean protein = ((double)(aa+n)/(double)count)>0.5;

		if(rna && !dna && !protein)
			return SequenceSetProperties.TYPE_RNA;
		if(dna && !rna && !protein)
			return SequenceSetProperties.TYPE_DNA;
		if(protein && !rna && !dna)
			return SequenceSetProperties.TYPE_PROTEIN;

		else
			return SequenceSetProperties.TYPE_UNKNOWN;
	}

	/*
	 * Simple algorithm to decide if the alignment is DNA or protein. Basically,
	 * if 85% of the data (not including '-' or '?') is ACGT U or N then the
	 * alignment is assumed to be DNA.
	 */
	public static boolean isSequenceDNA(SequenceSet ss)
	{
		int a = 0, c = 0, g = 0, t = 0, u = 0, n = 0;
		int count = 0;
		Vector<Sequence> sequences = ss.getSequences();
		for (Sequence seq : sequences)
		{
			StringBuffer buffer = seq.getBuffer();

			for (int ch = 0; ch < buffer.length(); ch++)
			{
				count++;

				switch (buffer.charAt(ch))
				{
				case 'A':
					a++;
					break;
				case 'C':
					c++;
					break;
				case 'G':
					g++;
					break;
				case 'T':
					t++;
					break;
				case 'U':
					u++;
					break;
				case 'N':
					n++;
					break;

				case '-':
					count--;
					break;
				case '?':
					count--;
					break;
				}
			}
		}

		int total = a + c + g + t + u + n;

		if ((((float) total / (float) count) * 100) > 85)
			return true;
		else
			return false;
	}

	public static SequenceSet getBootstrappedSequenceSet(SequenceSet ss, int blockSize, boolean shuffleSeqOrder) {
		int nSeqs = ss.getSize();
		int nNucs = ss.getLength();
		int nBlocks = nNucs/blockSize;

		char[][] bs = new char[nSeqs][nNucs];

		for(int i=0; i<nBlocks; i++) {
			int rand = SequenceSetUtils.random.nextInt(nBlocks);

			for(int j=0; j<blockSize; j++) {
				int nucPos = i*blockSize+j;
				int randNucPos = rand*blockSize+j;

				for(int seqPos=0; seqPos<nSeqs; seqPos++) {
					Sequence seq = ss.getSequence(seqPos);
					bs[seqPos][nucPos] = seq.getSequence().charAt(randNucPos);
				}

			}
		}

		SequenceSet result = new SequenceSet(ss);
		result.reset();
		for(int i=0; i<nSeqs; i++) {
			Sequence orgSeq = ss.getSequence(i);
			Sequence newSeq = new Sequence();
			newSeq.setName(orgSeq.getName());
			newSeq.safeName = orgSeq.safeName;
			newSeq.setSequence(new String(bs[i]));
			result.addSequence(newSeq, false);
		}

		if(shuffleSeqOrder) {
			SequenceSet shuffled = new SequenceSet(ss);
			shuffled.reset();

			while(result.getSequences().size()>0) {
				int size = result.getSequences().size();
				int rand = SequenceSetUtils.random.nextInt(size);
				Sequence seq = result.getSequence(rand);
				result.getSequences().remove(rand);
				shuffled.addSequence(seq, false);
			}

			return shuffled;
		}

		return result;
	}

	public static Sequence translate(Sequence seq, int start, int end)
	{
		Sequence trans = new Sequence(seq.getName());
		StringBuffer transBuffer = trans.getBuffer();
		String seqString = seq.getPartition(start, end);

		for(int i=0; i<seqString.length(); i+=3)
		{
			String codon = seqString.substring(i, (i+3));
			codon = codon.replaceAll("T", "U");

			if(codon.equals("---"))
				transBuffer.append('-');
			else
			{
				String aa = geneticCode.get(codon);

				if(aa!=null)
					transBuffer.append(aa);
				else
					transBuffer.append("?");
			}
		}

		return trans;
	}

	public static SequenceSet translate(SequenceSet ss, int start, int end, boolean justSel) {
		SequenceSet trans = new SequenceSet();
		if(justSel) {
			int[] selSeqs = ss.getSelectedSequences();
			for(int i=0; i<selSeqs.length; i++) {
				Sequence transSeq = translate(ss.getSequence(selSeqs[i]), start, end);
				if(transSeq!=null)
					trans.addSequence(transSeq);
			}
		}
		else {
			for(Sequence seq : ss.getSequences()) {
				Sequence transSeq = translate(seq, start, end);
				if(transSeq!=null)
					trans.addSequence(transSeq);
			}
		}
		return trans;
	}
}
