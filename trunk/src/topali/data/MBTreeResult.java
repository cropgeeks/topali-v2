// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

/*
 * Class that stores both the results from running MrBayes and the
 * settings required to make the run (although not the data itself).
 */
public class MBTreeResult extends TreeResult
{
	// The location of the MrBayes binary
	public String mbPath;

	// Sequences to be analysed
	public String[] selectedSeqs;

	public MBTreeResult()
	{
	}
}