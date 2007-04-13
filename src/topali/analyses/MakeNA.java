// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.io.File;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.fileio.AlignmentLoadException;
import topali.gui.TOPALi;
import topali.gui.Text;
import doe.MsgBox;

public class MakeNA
{
	Logger log = Logger.getLogger(this.getClass());
	
	private File dnaFile, proFile;

	public MakeNA(File dnaFile, File proFile)
	{
		this.dnaFile = dnaFile;
		this.proFile = proFile;
	}

	public boolean doConversion()
	{
		SequenceSet dna, pro;
		SequenceSet newSS = new SequenceSet();

		try
		{
			dna = new SequenceSet(dnaFile, false);
			pro = new SequenceSet(proFile, false);
		} catch (AlignmentLoadException e)
		{
			log.warn(e);
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err0"
					+ e.getReason()), MsgBox.ERR);
			return false;
		}

		// How many sequences are we dealing with
		int dnaCount = dna.getSize();
		int proCount = pro.getSize();
		if (dnaCount != proCount)
		{
			MsgBox.msg("The number of sequences in the cDNAs file (" + dnaCount
					+ ") differs from the number in the protein file ("
					+ proCount + ")", MsgBox.ERR);
			return false;
		}

		// Attempt to convert each sequence
		for (int i = 0; i < dnaCount; i++)
		{
			// Find the dna...
			Sequence dnaSeq = dna.getSequence(i);
			StringBuffer dnaBuf = dnaSeq.getBuffer();

			// ... and try to find its matching protein sequence
			Sequence proSeq = getSequenceByName(pro, dnaSeq.name);
			if (proSeq == null)
			{
				MsgBox.msg("The sequence " + dnaSeq.name + " was not found in "
						+ "the protein alignment.", MsgBox.ERR);
				return false;
			}

			// Then check that the lengths are ok
			int gaplessCount = 0;
			StringBuffer proBuf = proSeq.getBuffer();

			for (int p = 0; p < proBuf.length(); p++)
				if (proBuf.charAt(p) != '-')
					gaplessCount++;

			if ((gaplessCount * 3) != dnaBuf.length())
			{
				MsgBox.msg("The length of DNA sequence " + dnaSeq.name
						+ " is not "
						+ "3x the protein length.\n(DNA length is "
						+ dnaBuf.length() + ", protein length (minus gaps) is "
						+ gaplessCount + ")", MsgBox.ERR);
				return false;
			}

			// Finally, create the new sequence
			StringBuffer seqBuf = new StringBuffer(proBuf.length() * 3);

			for (int p = 0, d = 0; p < proBuf.length(); p++)
			{
				if (proBuf.charAt(p) == '-')
					seqBuf.append("---");

				else
				{
					if (d < dnaBuf.length())
						seqBuf.append(dnaBuf.substring(d, d + 3));
					else
						seqBuf.append(dnaBuf.substring(d));

					d += 3;
				}
			}

			// Create the new sequence
			Sequence newSequence = new Sequence(dnaSeq.name);
			newSequence.getBuffer().append(seqBuf);
			// And add it to the dataset
			newSS.addSequence(newSequence);
		}

		// Perform some final checks on the new alignment before OKing it
		try
		{
			newSS.checkValidity();
		} catch (AlignmentLoadException e)
		{
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err0"
					+ e.getReason()), MsgBox.ERR);
			return false;
		}

		if (SequenceSetUtils.verifySequenceNames(newSS) == false)
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err05"),
					MsgBox.WAR);

		AlignmentData data = new AlignmentData(getNewName(), newSS);
		TOPALi.winMain.addNewAlignmentData(data);

		return true;
	}

	private Sequence getSequenceByName(SequenceSet pro, String dnaName)
	{
		for (Sequence seq : pro.getSequences())
			if (seq.name.equals(dnaName))
				return seq;

		return null;
	}

	private String getNewName()
	{
		String name = dnaFile.getName();
		if (name.indexOf(".") != -1)
			name = name.substring(0, name.lastIndexOf("."));

		return name;
	}
}