// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var;


public class NHTreeUtils
{
	
	public static String removeBranchLengths(String tree) {
		String result = tree.replaceAll(":\\s*\\d+.\\d+", "");
		return result;
	}
	
}
