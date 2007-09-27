// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;


/**
 * Utilities to deal with New Hampshire trees
 */
public class NHTreeUtils
{
	/**
	 * Removes the branch lengths of a tree
	 * @param tree
	 * @return
	 */
	public static String removeBranchLengths(String tree) {
		String result = tree.replaceAll(":\\s*\\d+.\\d+", "");
		return result;
	}
	
	/**
	 * Removes the bootstrap or probability (MrBayes) values of a tree
	 * @param tree
	 * @return
	 */
	public static String removeBootstrapValues(String tree) {
		String result = tree.replaceAll("\\)\\d+(\\.\\d+)?", ")");
		return result;
	}
}
