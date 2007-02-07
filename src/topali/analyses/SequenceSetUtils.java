// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.util.*;
import javax.swing.*;

import pal.alignment.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;

import doe.*;

/*
 * Helper class that performs various analyses on a SequenceSet (Alignment)
 * object.
 */
public class SequenceSetUtils
{
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
		
		ListIterator itor = ss.getSequences().listIterator(0);
		for (int i = 0; i < indices.length; i++)
		{
			StringBuffer buffer = ((Sequence) itor.next()).getBuffer();

			ListIterator jtor = ss.getSequences().listIterator(0);
			for (int j = 0; j < indices.length; j++)
			{
				StringBuffer toCompare = ((Sequence) jtor.next()).getBuffer();
				
				if (i == j)
					continue;
					
				if (buffer.toString().equals(toCompare.toString()))
				{
					// Ensures only first instance of a duplicate gets added
					if ((keepFirstDuplicate && j < i) || !keepFirstDuplicate)
					{
						count--;
						indices[i] = -1;
					}
					else
						duplicateCount++;
						
					break;
				}
			}
		}
		
		int[] finalIndices = new int[count];
		for (int i = 0, c = 0; i < indices.length; i++)
			if (indices[i] != -1)
				finalIndices[c++] = indices[i];
		
		System.out.println("DUPLICATE=" + duplicateCount);
		
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
			newname = (String) JOptionPane.showInputDialog(MsgBox.frm,
				Text.Analyses.getString("SequenceSetUtils.gui01"),
				Text.Analyses.getString("SequenceSetUtils.gui02"),
				JOptionPane.PLAIN_MESSAGE, null, null, selected.name);

			if (newname == null)
				return false;
		
			if (newname.length() == 0)
				return false;
			
			// Check for invalid characters
			String verified = verifyName(newname, index+1);
			if (verified.equals(newname) == false)
			{
				MsgBox.msg(Text.Analyses.getString("SequenceSetUtils.err01"),
					MsgBox.ERR);
				continue;
			}
		
			// Check for it being unique
			ok = true;
			for (Sequence seq: ss.getSequences())
			{
				if (seq != selected && seq.name.equals(newname))
				{
					String msg = Text.format(
						Text.Analyses.getString("SequenceSetUtils.err02"),
						newname);
					MsgBox.msg(msg, MsgBox.ERR);
						
					ok = false;
					break;
				}
			}		
		}
		
		selected.name = newname;
		return true;
	}
	
	public static void estimateParameters(SequenceSet ss)
	{
		int[] indices = getSequencesForParamEstimation(ss);
		
		SimpleAlignment align = ss.getAlignment(indices, 1, ss.getLength(), true);
		ParamEstDialog dialog = new ParamEstDialog(align);

		SequenceSetParams params = new SequenceSetParams(
			dialog.getRatio(),
			dialog.getAlpha(),
			dialog.getKappa(),
			dialog.getAvgDistance(),
			dialog.getFreqs());
		
		ss.setParams(params);
		WinMainMenuBar.aFileSave.setEnabled(true);
	}
	
	/* Determines which set of sequences should be used when estimating the
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
			String msg = Text.Analyses.getString("SequenceSetUtils.msg01");		
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
		
		for (Sequence seq: ss.getSequences())
		{
			String oldName = seq.name;
			String newName = verifyName(seq.name, ++seqID);
			
			if (oldName.equals(newName) == false)
			{
				ok = false;
				seq.name = newName;
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
			String msg = Text.Analyses.getString("SequenceSetUtils.msg05");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		if (ss.isDNA() == false)
		{
			String msg = Text.Analyses.getString("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		return true;
	}
	
	public static boolean canRunHMM(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length != 4)
		{
			String msg = Text.Analyses.getString("SequenceSetUtils.msg03");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		if (ss.isDNA() == false)
		{
			String msg = Text.Analyses.getString("SequenceSetUtils.msg04");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		return true;
	}
	
	public static boolean canRunDSS(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 3)
		{
			String msg = Text.Analyses.getString("SequenceSetUtils.msg02");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		return true;
	}
	
	public static boolean canRunLRT(SequenceSet ss)
	{
		if (ss.getSelectedSequences().length < 3)
		{
			String msg = Text.Analyses.getString("SequenceSetUtils.msg06");
			MsgBox.msg(msg, MsgBox.ERR);
			
			return false;
		}
		
		return true;
	}
	
	// Creates and returns a new SequenceSet using the existing SequenceSet from
	// the alignment data. This method will *only* use sequences and partitions
	// that have been selected by the user
	// Currently only called by the Sequence->Export code
	public static SequenceSet getConcatenatedSequenceSet(AlignmentData data, RegionAnnotations annotations, int[] seqs, int[] regions)
	{
		SequenceSet ssOld = data.getSequenceSet();
		SequenceSet ssNew = new SequenceSet();		
		
		// For each sequence we want to add...
		for (int seqIndex: seqs)
		{
			// Create (and add) the sequence to the set
			Sequence seqOld = ssOld.getSequence(seqIndex);
			Sequence seqNew = new Sequence(seqOld.name);
						
			// Concatenate and add each partition to the sequence's data
			StringBuffer buffer = seqNew.getBuffer();
			
			// We either want to concatenate together the regions...
			if (regions.length > 0)
			{
				for (int r: regions)
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
}
