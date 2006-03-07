// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;
import java.util.*;

import topali.data.*;

class FileMSF extends FileGeneric
{
	// Sequences found
	private LinkedList<Sequence> seqs = new LinkedList<Sequence>();

	FileMSF(SequenceSet s)
	{
		ss = s;
	}
	
	public boolean readFile(File file)
	{	
		try
		{
			in = new BufferedReader(new FileReader(file));		
			String str = in.readLine();
			
			// Is this file even in msf format?
			if (str.toUpperCase().startsWith("!!NA_MULTIPLE_ALIGNMENT") == false
				&& str.toUpperCase().startsWith("PILEUP") == false)
				throw new Exception();
			
			// Step 1: determine header information
			while (str != null)
			{			
				if (str.startsWith("//"))
					break;
				
				str = in.readLine();
			}
			
			// Step 2: read sequence data
			readData(in.readLine(), in);
						
			// Step 3: reformat based on gaps, missing chars, etc
			for (int i = 0; i < seqs.size(); i++)
			{
				Sequence s = (Sequence) seqs.get(i);
				String data = s.getBuffer().toString();
				data = data.replace('.', '-');
				data = data.replace('~', '?');
				
				s.setSequence(data);
			}			
			
			// Step 4: finally pass each sequence to the SequenceSet collection
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
	
	private void readData(String str, BufferedReader in)
		throws Exception
	{
		StringTokenizer st = null;
		Sequence sequence = null;
		StringBuffer buf  = null;
	
		int index = 0;
		
		while (str != null)
		{
			// Break between interleaved blocks
			if (str.length() == 0)
				index = 0;
				
			else
			{
				// Name of this sequence
				st = new StringTokenizer(str);
				String name = st.nextToken();
				
				try
				{
					// We want to ignore lines that mark numbers (1...50 etc)
					Integer.parseInt(name);
				}
				catch (NumberFormatException e)
				{				
					// Has a Sequence object been created for it already?
					sequence = getSequence(index);
					// If not, then add a new one
					if (sequence == null)
					{
						sequence = new Sequence(name);
						seqs.addLast(sequence);
					}
					
					// Now append the actual data to it
					String line = str.substring(str.indexOf(st.nextToken()));
					sequence.getBuffer().append(line);
					
					index++;
				}
			}
			
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
		String sep = System.getProperty("line.separator");
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
			
		// Header information
		out.write("PileUp" + sep);
		out.newLine();
		
		// Header
		out.write(" MSF: " + ss.getLength() + " Type: ");
		if (ss.isDNA())
			out.write("N ");
		else
			out.write("P ");
		out.write("Check: 0 .." + sep);
		out.newLine();
		
		for (int seq = 0; seq < index.length; seq++)
		{
			if (useSafeNames)
				out.write(" Name: " + ss.getSequence(index[seq]).safeName);
			else
				out.write(" Name: " + ss.getSequence(index[seq]).name);
			out.write(" Len: " + ss.getLength());
			out.write(" Check: 0");
			out.write(" Weight: 1.00");
			out.newLine();
		}
		out.write(sep + "//" + sep);
		
		// Actual data
		int nameLen = getLongestNameLength(index) + 1;
		for (int i = start; i < end; i+=50)
		{
			out.newLine();
			
			for (int seq = 0; seq < index.length; seq++)
			{
				Sequence sequence = ss.getSequence(index[seq]);
				
				if (useSafeNames)
					out.write(sequence.safeName);
				else
					out.write(sequence.formatName(nameLen, true));
				
				for (int nStart = i; nStart < i+50; nStart += 10)
				{
					int nEnd = nStart + 9;
					if (nEnd > end) nEnd = end;
					if (nStart > nEnd)
						break;
						
					String toWrite = sequence.getPartition(nStart, nEnd);
					toWrite = toWrite.replace('-', '.');
					toWrite = toWrite.replace('?', '~');
					
					out.write(" " + toWrite);
				}
				
				out.newLine();
			}
		}
		
		out.close();
	}
}