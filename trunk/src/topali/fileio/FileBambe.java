// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import topali.data.Sequence;
import topali.data.SequenceSet;

class FileBambe extends FileGeneric
{
	FileBambe(SequenceSet s)
	{
		ss = s;
	}

	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(file));

		// Number of taxa, number of sites
		out.write(index.length + " " + (end - start + 1));
		out.newLine();

		for (int seq = 0; seq < index.length; seq++)
		{
			Sequence sequence = ss.getSequence(index[seq]);
			String str = sequence.getPartition(start, end);

			if (seq > 0)
				out.newLine();
			if (useSafeNames)
				out.write(sequence.safeName);
			else
				out.write(sequence.getName());

			for (int i = 0, j = 0; j < str.length(); i++, j++)
			{
				if (i % 50 == 0)
				{
					out.newLine();
					i = 0;
				}
				out.write(str.charAt(j));
			}
		}

		out.close();
	}
}