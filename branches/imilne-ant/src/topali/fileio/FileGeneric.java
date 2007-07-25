// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import pal.alignment.ReadAlignment;
import topali.data.*;

class FileGeneric implements ISeqFile
{
	protected BufferedReader in = null;

	protected SequenceSet ss;

	protected boolean success = true;

	FileGeneric()
	{
	}

	FileGeneric(SequenceSet s)
	{
		ss = s;
	}

	public boolean readFile(File file)
	{
		try
		{
			ReadAlignment alignment = new ReadAlignment("" + file);

			for (int i = 0; i < alignment.getSequenceCount(); i++)
			{
				Sequence seq = new Sequence("" + alignment.getIdentifier(i));
				seq.getBuffer().append(alignment.getAlignedSequenceString(i));

				ss.addSequence(seq);
			}

			return true;
		} catch (Exception e)
		{
			ss.reset();
			return false;
		}
	}

	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{

	}

	protected int getLongestNameLength(int[] index)
	{
		int longest = 0;

		for (int i = 0; i < index.length; i++)
		{
			Sequence s = ss.getSequence(index[i]);
			if (s.getName().length() > longest)
				longest = s.getName().length();
		}

		return longest;
	}
}