// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import topali.data.*;

class FileNexusMB extends FileNexus
{
	FileNexusMB(SequenceSet ss)
	{
		super(ss);
	}
	
	public void writeFile(File file, int[] index, int start, int end, boolean useSafeNames)
		throws IOException
	{
		String nl = System.getProperty("line.separator");		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		// Header information
		out.write("#NEXUS" + nl);
		out.newLine();
		
		// begin data
		out.write("begin data;" + nl);
		out.write("  dimensions ntax=" + index.length + " nchar=" + (end-start+1) + ";" + nl);
		out.write("  format" + nl + "    interleave" + nl);
		if (ss.isDNA())
		{
			out.write("    datatype=DNA" + nl);
		}
		else
			out.write("    datatype=PROTEIN" + nl);
		out.write("    missing=?" + nl);
		out.write("    gap=-" + nl);
		out.write("    matchchar=." + nl);
		out.write("  ;" + nl);
		out.newLine();
		
		// Actual data
		out.write("  matrix" + nl);
		
		int nameLen = getLongestNameLength(index) + 1;
		for (int i = start; i < end; i+=50)
		{			
			for (int seq = 0; seq < index.length; seq++)
			{
				Sequence sequence = ss.getSequence(index[seq]);
				
				if (useSafeNames)
					out.write(sequence.safeName + " ");
				else
					out.write(sequence.formatName(nameLen, false));
								
				int nStart = i;
				int nEnd = i+49;					
				if (nEnd > end) nEnd = end;					
				
				out.write(sequence.getPartition(nStart, nEnd));
				out.newLine();
			}
			out.newLine();
		}
		
		out.write("  ;" + nl + "end;");			
		out.close();
	}
}