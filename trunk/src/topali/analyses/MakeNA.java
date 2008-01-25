// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.io.File;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.fileio.AlignmentLoadException;
import topali.gui.Text;
import topali.var.AssociationMap;
import scri.commons.gui.MsgBox;

public class MakeNA
{
	 Logger log = Logger.getLogger(this.getClass());

	private SequenceSet dna = null, pro = null;
	private String name;
	
	private AlignmentData data;
	
	public MakeNA(File dnaFile, File proFile) throws AlignmentLoadException
	{	
		dna = new SequenceSet(dnaFile, false);
		pro = new SequenceSet(proFile, false);
		name = createName(dnaFile.getName());
	}
	
	public MakeNA(SequenceSet dna, SequenceSet pro, String name) {
		this.dna = dna;
		this.pro = pro;
		this.name = name;
	}
	
	public boolean doConversion() {
		return doConversion(true);
	}
	
	public boolean doConversion(boolean showMessages) {
		return doConversion(showMessages, null);
	}
	
	public boolean doConversion(boolean showMessages, AssociationMap<Object> map)
	{
		//SequenceSet newSS = new SequenceSet();

		// How many sequences are we dealing with
		int dnaCount = dna.getSize();
		int proCount = pro.getSize();
		if (dnaCount != proCount)
		{
			String msg = "The number of sequences in the cDNAs file (" + dnaCount
			+ ") differs from the number in the protein file ("
			+ proCount + ")";
			log.warn(msg);
			if(showMessages)
				MsgBox.msg(msg, MsgBox.ERR);
			return false;
		}

		// Attempt to convert each sequence
		for (int i = 0; i < dnaCount; i++)
		{
			// Find the dna...
			Sequence dnaSeq = dna.getSequence(i);
			StringBuffer dnaBuf = dnaSeq.getBuffer();

			// ... and try to find its matching protein sequence
			Sequence proSeq = getSequenceByName(pro, dnaSeq.getName());
			if (proSeq == null)
			{
				String msg = "The sequence " + dnaSeq.getName() + " was not found in "
				+ "the protein alignment.";
				log.warn(msg);
				if(showMessages)
					MsgBox.msg(msg, MsgBox.ERR);
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
				String msg = "The length of DNA sequence " + dnaSeq.getName()
				+ " is not "
				+ "3x the protein length.\n(DNA length is "
				+ dnaBuf.length() + ", protein length (minus gaps) is "
				+ gaplessCount + ")";
				
				log.warn(msg);
				
				if(showMessages)
					MsgBox.msg(msg, MsgBox.ERR);
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
			//Sequence newSequence = new Sequence(dnaSeq.getName());
			//newSequence.getBuffer().append(seqBuf);
			// And add it to the dataset
			dnaSeq.setSequence(seqBuf.toString());
			//newSS.addSequence(newSequence);
			
			if(map!=null) 
				//map.put(newSequence, proSeq);
				map.put(dnaSeq, proSeq);
		}

		// Perform some final checks on the new alignment before OKing it
		try
		{
			//newSS.checkValidity();
			dna.checkValidity();
		} catch (AlignmentLoadException e)
		{
			String msg = Text.I18N.getString("ImportDataSetDialog.err0"
					+ e.getReason());
			log.warn(msg);
			if(showMessages)
				MsgBox.msg(msg, MsgBox.ERR);
			return false;
		}

		if (SequenceSetUtils.verifySequenceNames(dna) == false) {
			String msg = Text.I18N.getString("ImportDataSetDialog.err05");
			log.warn(msg);
			if(showMessages)
				MsgBox.msg(msg,
					MsgBox.WAR);
		}

		this.data = new AlignmentData(name, dna);
		//TOPALi.winMain.addNewAlignmentData(data);

		return true;
	}

	private Sequence getSequenceByName(SequenceSet pro, String dnaName)
	{
		for (Sequence seq : pro.getSequences())
			if (seq.getName().equals(dnaName))
				return seq;

		return null;
	}

	private String createName(String name)
	{
		if (name.indexOf(".") != -1)
			name = name.substring(0, name.lastIndexOf("."));

		return name;
	}
	
	public AlignmentData getAlignmentData() {
		return this.data;
	}
	
}