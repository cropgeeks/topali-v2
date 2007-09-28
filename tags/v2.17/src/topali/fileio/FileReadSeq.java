// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

import org.apache.log4j.Logger;

import sbrn.commons.bioinf.ReadSeq;
import topali.data.SequenceSet;
import topali.gui.Prefs;
import doe.MsgBox;

// Unlike the other classes, this one just pretends to be a file-format handler,
// delegating the actual work to the ReadSeq util class (web/cgi service)
class FileReadSeq extends FileGeneric
{
	 Logger log = Logger.getLogger(this.getClass());
	
	FileReadSeq(SequenceSet s)
	{
		ss = s;
	}

	@Override
	public boolean readFile(File file)
	{
		File outFile = new File(Prefs.tmpDir, "tmpAlignment");

		// Use ReadSeq to convert the file into a FASTA formatted file
		System.out.println("Attempting ReadSeq conversion...");
		ReadSeq readSeq = new ReadSeq(file, outFile);
		try
		{
			readSeq.convertFile();
		} catch (Exception e)
		{
			log.warn(e);
			success = false;
			ss.reset();

			MsgBox.msg("TOPALi was unable to convert the file using ReadSeq:\n"
					+ e, MsgBox.ERR);
		}

		// Then use TOPALi's FASTA handler to load the file
		FileFasta ff = new FileFasta(ss);
		success = ff.readFile(outFile);

		return success;
	}

	@Override
	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException
	{

	}
}