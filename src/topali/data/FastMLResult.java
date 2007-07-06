// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

/**
 * Just a result wrapper around a alignment data
 * (The result of a fastml job is a new alignment containing the corresponding tree)
 */
public class FastMLResult extends AlignmentResult
{

	public String fastmlPath;
	
	//The tree fastml will be based on
	public String origTree;
	
	//The alignment, which contains the ancestral sequences
	public AlignmentData alignment;
	
	public FastMLResult() {
		alignment = new AlignmentData();
		isRemote = false;
	}
	
	
}
