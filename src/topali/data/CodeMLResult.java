// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

/*
 * Class that stores both the results from running a PAML positive selection
 * analysis (via CODEML) and the settings required to make the run (although 
 * not the data itself).
 */
public class CodeMLResult extends AlignmentResult
{
	// The location of the CodeML binary
	public String codeMLPath;
	
	public CodeMLResult()
	{
	}
}