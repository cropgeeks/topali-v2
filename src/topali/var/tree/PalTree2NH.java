// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.tree;

import pal.tree.*;

public class PalTree2NH
{
	Tree tree;
	
	String nw;
	
	/**
	 * Extracts the Newick tree from a PalTree including the bootstrap values
	 * (PalTree.toString forgets the bs values)
	 * @param tree
	 */
	public PalTree2NH(Tree tree) {
		this.tree = tree;	
		nw = createNW(tree.getRoot())+";";
	}
	
	public String getNW() {
		return nw;
	}
	
	private String createNW(Node node) {
		if(node.isLeaf())
			return node.getIdentifier()+":"+node.getBranchLength();
		
		String s = "(";
		for(int i=0; i<node.getChildCount(); i++) {
			s += createNW(node.getChild(i))+",";
		}
		s = s.substring(0, s.length()-1);
		s += ")";
		Object obj = tree.getAttribute(node, "bootstrap");
		if(obj!=null)
			s += obj.toString();
		s+= ":"+node.getBranchLength();
		
		return s;
	}
}
