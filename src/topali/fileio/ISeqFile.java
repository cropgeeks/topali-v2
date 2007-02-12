// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

import java.io.File;
import java.io.IOException;

interface ISeqFile
{
	public boolean readFile(File file);

	public void writeFile(File file, int[] index, int start, int end,
			boolean useSafeNames) throws IOException;
}
