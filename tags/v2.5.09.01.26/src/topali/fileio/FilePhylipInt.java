// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import pal.alignment.*;
import topali.data.SequenceSet;

public class FilePhylipInt extends FileGeneric
{
	public FilePhylipInt(SequenceSet s)
	{
		ss = s;
	}

	
	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(file));

		// Create a PAL alignment
		SimpleAlignment alignment = ss.getAlignment(index, start, end,
				useSafeNames);

		// Write it to disk
		AlignmentUtils.printInterleaved(alignment, out);

		out.close();
	}
}
