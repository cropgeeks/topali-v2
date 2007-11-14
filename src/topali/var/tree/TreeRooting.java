// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.var.tree;

import java.io.*;
import java.util.*;

import pal.tree.*;

public class TreeRooting
{

	String tree;
	
	String allChildren;
	
	public TreeRooting(String tree) {
		this.tree = tree;
	}
	
	public String mpRoot() throws Exception {
		PushbackReader pbread = new PushbackReader(new StringReader(tree));
		ReadTree t = new ReadTree(pbread);
		
		//very dirty workaround for PAL tree loosing bootstrap values:
		//bs values are stored in "table" and then after mp rooting written
		//back to the tree.
		int c = t.getExternalNodeCount();
		List<Node> list = new LinkedList<Node>();
		for(int i=0; i<c; i++) {
			list.add(t.getExternalNode(i));
		}
		allChildren = list2String(list);
		
		Hashtable<String, String> table = new Hashtable<String, String>();
		c = t.getInternalNodeCount();
		for(int i=0; i<c; i++) {
			Node node = t.getInternalNode(i);
			List<Node> children = new LinkedList<Node>();
			children = getChildren(node, children);
			table.put(list2String(children), node.getIdentifier().getName());
		}
		
		Tree t2 = TreeRooter.getMidpointRooted(t);
		c = t2.getInternalNodeCount();
		for(int i=0; i<c; i++) {
			Node node = t2.getInternalNode(i);
			List<Node> children = new LinkedList<Node>();
			children = getChildren(node, children);
			String tmp = table.get(list2String(children));
			if(tmp==null)
				tmp = table.get(reverse(children));
			if(tmp!=null)
				t2.setAttribute(node, "bootstrap", tmp);
		}
		
		return (new PalTree2NH(t2)).getNW();
	}
	
	private List<Node> getChildren(Node node, List<Node> children) {
		int c = node.getChildCount();
		for(int i=0; i<c; i++) {
			Node child = node.getChild(i);
			if(child.isLeaf())
				children.add(child);
			else
				getChildren(child, children);
		}
		return children;
	}
	
	private String list2String(List<Node> list) {
		Collections.sort(list, new NodeListComparator());
		StringBuffer sb = new StringBuffer();
		for(Node n : list) {
			sb.append(n.getIdentifier().getName());
			sb.append(',');
		}
		return sb.toString();
	}
	
	private String reverse(List<Node> list) {
		String result = new String(allChildren);
		for(Node n : list) {
			result = result.replace(n.getIdentifier().getName()+",", "");
		}
		return result;
	}
	
	class NodeListComparator implements Comparator<Node> {

		@Override
		public int compare(Node o1, Node o2)
		{
			return o1.getIdentifier().getName().compareTo(o2.getIdentifier().getName());
		}

		
	}
}
