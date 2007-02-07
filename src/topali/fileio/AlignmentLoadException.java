// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.fileio;

public class AlignmentLoadException extends Exception
{
	public static final int NO_SEQUENCES = 1;
	public static final int NOT_ALIGNED = 2;
	public static final int DUPLICATE_NAMES_FOUND = 3;
	public static final int UNKNOWN_FORMAT = 4;
	
	private int code;
	
	public AlignmentLoadException(int code)
	{
		this.code = code;
	}
	
	public int getReason()
		{ return code; }
}