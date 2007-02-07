// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import javax.swing.*;
import javax.swing.tree.*;

import topali.data.*;
import topali.gui.*;

// Note: used to be an interface (hence the name), now a superclass
abstract class INode
{	
	// The AlignmentData object associated with this node
	protected AlignmentData data;
	protected SequenceSet ss;
	
	INode(AlignmentData data)
	{
		this.data = data;
		
		if (data.isReferenceList() == false)
			ss = data.getSequenceSet();
	}
	
	public abstract void setMenus();
	
	public abstract JComponent getPanel();
			
	public AlignmentData getAlignmentData()
		{ return data; }
	
	public SequenceSet getSequenceSet()
		{ return ss; }

	public int getTipsKey()
		{ return WinMainTipsPanel.TIPS_NONE; }
	
	public String getHelpKey()
		{ return "intro"; }
	
	// Search methods
	
	// Checks the given node to see if it is the TreePaneNode for the associated
	// AlignmentData datset
	static boolean isTreePaneNodeForData(DefaultMutableTreeNode n, AlignmentData d)
	{
		if (n.getUserObject() instanceof INode == false)
			return false;
		
		INode iNode = (INode) n.getUserObject();
		return (iNode instanceof TreePaneNode && iNode.getAlignmentData() == d);
	}
}