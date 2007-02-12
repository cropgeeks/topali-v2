// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;
import java.util.LinkedList;
import java.util.StringTokenizer;

import pal.alignment.AlignmentUtils;
import pal.alignment.SimpleAlignment;
import topali.data.Sequence;
import topali.data.SequenceSet;

class FileClustal extends FileGeneric
{
	// Sequences found
	private LinkedList<Sequence> seqs = new LinkedList<Sequence>();

	FileClustal(SequenceSet s)
	{
		ss = s;
	}

	public boolean readFile(File file)
	{
		try
		{
			in = new BufferedReader(new FileReader(file));
			String str = in.readLine();

			// Is this file even in clustal/muscle format?
			if (str.toUpperCase().startsWith("CLUSTAL"))
			{
			} else if (str.toUpperCase().startsWith("MUSCLE"))
			{
			} else
				throw new Exception();

			str = in.readLine();

			while (str != null)
			{
				// Looking for the start of a new block of data...
				int seqNum = -1;
				while (seqNum == -1 && str != null)
				{
					if (str.length() > 0)
					{
						seqNum = 0;
						break;
					}

					str = in.readLine();
				}

				while (str != null)
				{
					// Blocks between sequence data (maybe with *s)
					if ((str.startsWith(" ") || str.length() == 0)
							&& seqNum > 0)
					{
						str = in.readLine();
						break;
					}

					// Get the sequence's name
					StringTokenizer st = new StringTokenizer(str);
					String name = st.nextToken();

					// Find it (or create it)
					Sequence sequence = getSequence(seqNum);
					if (sequence == null)
					{
						sequence = new Sequence(name);
						seqs.addLast(sequence);
					}

					// Add the data to it
					sequence.getBuffer().append(st.nextToken());

					seqNum++;
					str = in.readLine();
				}
			}

			// Add all the sequences to the SequenceSet
			for (int i = 0; i < seqs.size(); i++)
				ss.addSequence((Sequence) seqs.get(i));
		} catch (Exception e)
		{
			success = false;
			ss.reset();
		}

		try
		{
			in.close();
		} catch (Exception e)
		{
		}

		return success;
	}

	private Sequence getSequence(int index)
	{
		if (index >= seqs.size())
			return null;
		else
			return (Sequence) seqs.get(index);
	}

	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(file));

		// Create a PAL alignment
		SimpleAlignment alignment = ss.getAlignment(index, start, end,
				useSafeNames);

		// Write it to disk
		AlignmentUtils.printCLUSTALW(alignment, out);

		out.close();
	}

	/*
	 * public boolean writeFile(File file, int[] index, int start, int end) {
	 * try { PrintWriter out = new PrintWriter(new FileWriter(file));
	 *  // Create a PAL alignment SimpleAlignment alignment =
	 * ss.getAlignment(index, start, end);
	 *  // Write it to disk AlignmentUtils.printCLUSTALW(alignment, out);
	 * 
	 * out.close(); return true; } catch (Exception e) { MsgBox.msg("Unable to
	 * create file " + file + ":\n" + e, MsgBox.ERR); return false; } }
	 */
}