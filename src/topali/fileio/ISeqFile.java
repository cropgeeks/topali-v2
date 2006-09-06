// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.*;

interface ISeqFile
{
	public boolean readFile(File file);
	
	public void writeFile(File file, int[] index, int start, int end, boolean useSafeNames)
		throws IOException;
}
