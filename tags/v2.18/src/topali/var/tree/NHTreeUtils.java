// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.tree;


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
		String result = replaceBootstrapValues(tree, "");
		return result;
	}
	
	/**
	 * Replace bootstrap values with a certain replacement
	 * @param tree
	 * @param replacement
	 * @return
	 */
	public static String replaceBootstrapValues(String tree, String replacement) {
		String result = tree.replaceAll("\\)\\d+(\\.\\d+)?", ")"+replacement);
		return result;
	}
	
	/**
	 * Remove bootstrap values which are below a certain threshold
	 * @param tree
	 * @param threshold
	 * @return
	 */
	public static String removeBootstrapValues(String tree, double threshold) {
		int s = 0;
		int e = 0;
		while(true) {
			s = tree.indexOf(')', e);
			e = tree.indexOf(':', s);
			
			if(s==-1 || e==-1)
				break;
			if(e<=(s+1))
				continue;
			
			double bs = Double.parseDouble(tree.substring(s+1, e));
			if(bs<threshold) {
				tree = tree.substring(0, s+1)+tree.substring(e);
			}
			
		}
		
		return tree;
	}
	
	/**
	 * Determines min (int[0]), avg (int[1]) and max (int[2]) of
	 * the bootstrap values in the tree
	 * @param tree
	 * @return
	 */
	public static double[] analyzeBootstrapValues(String tree) {
		double min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, avg = 0;
		
		int s = 0;
		int e = 0;
		int c = 0;
		while(true) {
			s = tree.indexOf(')', e);
			e = tree.indexOf(':', s);
			
			if(s==-1 || e==-1)
				break;
			if(e<=(s+1))
				continue;
			
			double bs = Double.parseDouble(tree.substring(s+1, e));
			if(bs<min) {
				min = bs;
			}
			if(bs>max) {
				max = bs;
			}
			avg += bs;
			c++;
		}
		
		return new double[] {min, avg/(double)c,max};
	}
}
