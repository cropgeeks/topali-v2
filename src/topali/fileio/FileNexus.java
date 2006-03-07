// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;
import java.util.*;

import topali.data.*;

class FileNexus extends FileGeneric
{
	// Header information
	private boolean inComment = false;
	private boolean interleaved = false;
	private	int length = 0;
	private	char gap   = '-';
	private	char match = '.';
	private	char miss  = '?';
	
	// Sequences found
	private LinkedList<Sequence> seqs = new LinkedList<Sequence>();

	FileNexus(SequenceSet s)
	{
		ss = s;
	}
	
	public boolean readFile(File file)
	{	
		try
		{
			in = new BufferedReader(new FileReader(file));		
			String str = in.readLine();
			
			// Is this file even in nexus format?
			if (str.toUpperCase().indexOf("#NEXUS") == -1)
				throw new Exception();
			
			// Step 1: determine header information
			while (str != null)
			{			
				str = str.trim();
				str = str.toLowerCase();
				
				if (str.startsWith("[")) inComment = true;
				
				if (!inComment)
				{
					// Will the data be interleaved or not
					if (str.indexOf("interleave") != -1)
						interleaved = true;
					
					// Determine number of sites
					int index = str.indexOf("nchar=");
					if (index != -1)
						length = Integer.parseInt(str.substring(index+6,
							str.indexOf(";", index+6)));
					
					// What gap character is being used?
					index = str.indexOf("gap=");
					if (index != -1)
						gap = str.substring(index+4, index+5).charAt(0);
					
					// What match character is being used?
					index = str.indexOf("matchchar=");
					if (index != -1)
						match = str.substring(index+10, index+11).charAt(0);
					
					// What missing character is being used?
					index = str.indexOf("missing=");
					if (index != -1)
						miss = str.substring(index+8, index+9).charAt(0);
					
					// Is the header finished?
					if (str.indexOf("matrix") != -1)
						break;
				}
				
				if (str.indexOf("]") != -1) inComment = false;
				
				str = in.readLine();
			}
			
			str = in.readLine();
			
						
			// Step 2: read sequence data
			readData(str, in);
						
			
			// Step 3: reformat based on gaps, missing chars, etc
			for (int i = 0; i < seqs.size(); i++)
			{
				Sequence s = (Sequence) seqs.get(i);
				String data = s.getBuffer().toString();
				if (gap != '-')
					data = data.replace(gap, '-');
				if (miss != '?')
					data = data.replace(miss, '?');
				
				s.setSequence(data);
			}
			
			
			// Step 4: fill in match chars
			try
			{
				StringBuffer seq0 = ((Sequence)seqs.get(0)).getBuffer();
				for (int i = 1; i < seqs.size(); i++)
				{
					StringBuffer buf = ((Sequence) seqs.get(i)).getBuffer();
					// For each '.' character in the sequence, replace it with
					// the corresponding value from the first sequence
					for (int j = 0; j < buf.length(); j++)
						if (buf.charAt(j) == match)
							buf.setCharAt(j, seq0.charAt(j));
				}
			}
			catch (Exception e) {}			
			
			
			// Step 5: finally pass each sequence to the SequenceSet collection
			for (int i = 0; i < seqs.size(); i++)
				ss.addSequence((Sequence)seqs.get(i));
		}
		catch (Exception e)
		{
			success = false;
			ss.reset();
		}
		
		try { in.close(); }
		catch (Exception e) {}
		
		return success;
	}
	
	private void readData(String str, BufferedReader in) throws Exception
	{
		StringTokenizer st = null;
		Sequence sequence = null;
		StringBuffer buf  = null;
	
		int index = 0;
		
		while (str != null)
		{
			if (str.startsWith("[")) inComment = true;
		
			if (!inComment)
			{
				// Break between interleaved blocks
				if (str.length() == 0)
					index = 0;
					
				// End of data has been reached
				else if (str.indexOf(";") != -1)
					break;
					
				else
				{
					// Name of this sequence
					st = new StringTokenizer(str);
					String name = st.nextToken();
					
					// Has a Sequence object been created for it already?
					sequence = getSequence(index);
					// If not, then add a new one
					if (sequence == null)
					{
						sequence = new Sequence(name, length);
						seqs.addLast(sequence);
					}
					
					// Now append the actual data to it
					String line = str.substring(str.indexOf(st.nextToken()));
					sequence.getBuffer().append(
						line.replaceAll("\\[.+\\]", " "));
						// Reg exp removes any [####] inline comments
					
					index++;
				}
			}
			
			if (str.indexOf("]") != -1) inComment = false;
			str = in.readLine();
		}
	}
	
	private Sequence getSequence(int index)
	{
		if (index >= seqs.size())
			return null;
		else	
			return (Sequence) seqs.get(index);
	}

	public void writeFile(File file, int[] index, int start, int end, boolean useSafeNames)
		throws IOException
	{
		String nl = System.getProperty("line.separator");		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		
		// Header information
		out.write("#NEXUS" + nl);
		out.newLine();
		
		// Comments
/*		out.write("[NEXUS alignment file generated by TOPALi - www.bioss.sari.ac.uk/~iainm/topali" + Prefs.sepL);
		out.write("  Alignment: " + ss.getName() + ", partition from "
			+ (start+1) + " to " + end + nl + nl);
		
		out.write(""
			+ "This file can be input into SPLITSTREE to produce a graphical check for" + nl
			+ "evidence of recombination among the sequences." + nl + nl
			+ "This file can also be input into PAUP* for further phylogenetic analysis." + nl);
		
		// Are we saving the entire alignment - if so, then include summary
		if (start == 0 && end == ss.getLength() && index.length == ss.getSize())
			out.write(nl + SummaryDialog.getSummaryInformation(nl) + nl);
		
		out.write(nl + "]" + nl + nl);
*/
		
		// begin taxa
		out.write("begin taxa;" + nl + "  dimensions ntax="
			+ index.length + ";" + nl);
		out.write("  taxlabels" + nl);
		for (int seq = 0; seq < index.length; seq++)
		{
			if (useSafeNames)
				out.write("    " + ss.getSequence(index[seq]).safeName + nl);
			else
				out.write("    " + ss.getSequence(index[seq]).name + nl);
		}
		out.write("  ;" + nl + "end;" + nl);
		out.newLine();
		
		// begin characters
		out.write("begin characters;" + nl);
		out.write("  dimensions nchar=" + (end-start+1) + ";" + nl);
		out.write("  format" + nl + "    interleave" + nl);
		if (ss.isDNA())
		{
			out.write("    datatype=DNA" + nl);
			out.write("    symbols=\"A C G T U\"" + nl);
		}
		else
			out.write("    datatype=PROTEIN" + nl);
		out.write("    missing=?" + nl);
		out.write("    gap=-" + nl);
		out.write("    matchchar=." + nl);
		out.write("    labels" + nl);
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