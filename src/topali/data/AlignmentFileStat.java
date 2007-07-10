// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.io.*;

public class AlignmentFileStat
{
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
	
	public String toString()
		{ return new File(filename).getName(); }
}