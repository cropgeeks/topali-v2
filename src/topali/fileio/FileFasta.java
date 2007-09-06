// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;
import java.util.StringTokenizer;

import topali.data.*;

class FileFasta extends FileGeneric
{
	FileFasta(SequenceSet s)
	{
		ss = s;
	}

	@Override
	public boolean readFile(File file)
	{
		try
		{
			in = new BufferedReader(new FileReader(file));
			String str = in.readLine();

			Sequence sequence = null;
			StringBuffer buf = null;

			while (str != null)
			{
				if (str.startsWith(">"))
				{
					if (sequence != null)
						ss.addSequence(sequence);

					// Determine name
					// StringTokenizer st = new StringTokenizer(str, ">| ");
					StringTokenizer st = new StringTokenizer(str, "> ");

					sequence = new Sequence(st.nextToken());
					buf = sequence.getBuffer();
				} else if (str.length() > 0)
					if (str.charAt(0) != ';')
						buf.append(str);

				str = in.readLine();
			}

			if (sequence != null)
				ss.addSequence(sequence);
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
	
	@Override
	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(file));

		for (int seq = 0; seq < index.length; seq++)
		{
			Sequence sequence = ss.getSequence(index[seq]);
			String str = sequence.getPartition(start, end);

			if (seq > 0)
				out.newLine();
			if (useSafeNames)
				out.write(">" + sequence.safeName);
			else
				out.write(">" + sequence.getName());

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