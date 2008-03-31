// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.analyses;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import pal.tree.*;
import topali.gui.TOPALi;
import topali.var.threads.*;
import topali.var.utils.TreeUtils;

public class TreeRootingThread extends DesktopThread
{
	Logger log = Logger.getLogger(this.getClass());
	
	String tree;
	boolean showDialog;
	
	String mpRootedTree;
	
	public TreeRootingThread(String tree, boolean showDialog) {
		this.tree = tree;
		this.showDialog = showDialog;
	}

	public String getMPRootedTree() {
		if(showDialog) {
			DefaultWaitDialog dlg = new DefaultWaitDialog(TOPALi.winMain, "Rerooting Tree", "Midpoint rooting Tree. Please be patient...", this);
			dlg.setLocationRelativeTo(TOPALi.winMain);
			dlg.setVisible(true);
		}
		else {
			run();
		}
		
		return mpRootedTree;
	}
	
	String allChildren;
	
	public void run()
	{
		try
		{
			PushbackReader pbread = new PushbackReader(new StringReader(tree));
			ReadTree t = new ReadTree(pbread);
			Tree t2 = TreeRooter.getMidpointRooted(t);
			
			//WORKAROUND for PAL tree loosing bootstrap values:
			//bs values are stored in "table" and then written back 
			//to the midpoint rooted tree.
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
			//END WORKAROUND
			
			if(!stop)
			    mpRootedTree = TreeUtils.getNewick(t2);
			else
			    mpRootedTree = null;
		} catch (TreeParseException e)
		{
			log.warn("Midpoint rooting failed.", e);
		}
		finally {
			updateObservers(DesktopThread.THREAD_FINISHED);
		}
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

		
		public int compare(Node o1, Node o2)
		{
			return o1.getIdentifier().getName().compareTo(o2.getIdentifier().getName());
		}
	}
	
}
