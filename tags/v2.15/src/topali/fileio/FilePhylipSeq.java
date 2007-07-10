// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import pal.alignment.*;

import topali.data.*;

public class FilePhylipSeq extends FileGeneric
{
	public FilePhylipSeq(SequenceSet s)
	{
		ss = s;
	}
	
	public void writeFile(File file, int[] index, int start, int end, boolean useSafeNames)
		throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(file));
		
		// Create a PAL alignment
		SimpleAlignment alignment = ss.getAlignment(index, start, end, useSafeNames);
		
		// Write it to disk
		AlignmentUtils.printSequential(alignment, out);
		
		out.close();
	}
}