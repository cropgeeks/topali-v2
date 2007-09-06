// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.*;

public class AlignmentFileStat implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7533048882832132271L;

	public String filename;

	public int size;

	public int length;

	public boolean isDna;

	public long fileSize;

	public AlignmentFileStat()
	{
	}

	public AlignmentFileStat(String filename)
	{
		this.filename = filename;
	}

	@Override
	public String toString()
	{
		return new File(filename).getName();
	}
}