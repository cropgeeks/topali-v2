// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import static topali.fileio.AlignmentLoadException.UNKNOWN_FORMAT;
import static topali.mod.Filters.*;

import java.io.File;

import topali.data.SequenceSet;

/*
 * Handler class for the file types that the program supports
 */
public class AlignmentHandler
{
	private SequenceSet ss;

	public AlignmentHandler(SequenceSet ss)
	{
		this.ss = ss;
	}

	public void openAlignment(File filename) throws AlignmentLoadException
	{
		// Can we load it?
		if (readFile(filename) == false)
			throw new AlignmentLoadException(UNKNOWN_FORMAT);
	}

	// Attempts to read the given file by sequentially loading it with each of
	// the supported file type classes until it is successfully read or fails
	private boolean readFile(File file)
	{
		ISeqFile seqFile = null;

		// Putting this last for now because I don't trust ReadSeq on phylip
		// (PAL is better for it)
		// Moved it to first again: 30/05/2008
		seqFile = new FileReadSeq(ss);
		if (seqFile.readFile(file))
			return true;

		// Try possible file formats until successfull
		seqFile = new FileNexus(ss);
		if (seqFile.readFile(file))
			return true;

		seqFile = new FileMSF(ss);
		if (seqFile.readFile(file))
			return true;

		seqFile = new FileClustal(ss);
		if (seqFile.readFile(file))
			return true;

		seqFile = new FileFasta(ss);
		if (seqFile.readFile(file))
			return true;

		seqFile = new FileGeneric(ss);
		if (seqFile.readFile(file))
			return true;



		return false;
	}

	public void save(File file, int[] sequences, int start, int end,
			int format, boolean useSafeNames) throws Exception
	{
		ISeqFile iFile = null;

		switch (format)
		{
		case FAS:
			iFile = new FileFasta(ss);
			break;
		case ALN:
			iFile = new FileClustal(ss);
			break;
		case MSF:
			iFile = new FileMSF(ss);
			break;
		case NEX:
			iFile = new FileNexus(ss);
			break;
		case NEX_B:
			iFile = new FileNexusMB(ss);
			break;
		case BAM:
			iFile = new FileBambe(ss);
			break;
		case PHY_S:
			iFile = new FilePhylipSeq(ss);
			break;
		case PHY_I:
			iFile = new FilePhylipInt(ss);
			break;
		}

		iFile.writeFile(file, sequences, start, end, useSafeNames);
	}

}
