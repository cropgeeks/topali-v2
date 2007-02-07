// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import java.awt.*;
import javax.swing.*;
import javax.swing.tree.*;

import topali.gui.*;

class NavPanelRenderer extends JLabel implements TreeCellRenderer
{
	private static Color bColor = UIManager.getColor("Tree.selectionBackground");	
	private boolean selected = false;
	
	public Component getTreeCellRendererComponent(
		JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		Object obj = node.getUserObject();			
		setText(obj.toString());
		
		if (obj instanceof DataSetNodeFolder && expanded)
			setIcon(Icons.TREE_FOLDER_OPEN);
		else if (obj instanceof DataSetNodeFolder && !expanded)
			setIcon(Icons.TREE_FOLDER_CLOSED);
		
		else if (obj instanceof ResultsNodeFolder && expanded)
			setIcon(Icons.TREE_RESULTS_OPEN);
		else if (obj instanceof ResultsNodeFolder && !expanded)
			setIcon(Icons.TREE_RESULTS_CLOSED);
		
		else if (obj instanceof SequenceSetNode)
			setIcon(Icons.TREE_ALIGNMENT);
		
		else if (obj instanceof FileListNode)
			setIcon(Icons.TREE_TABLE);
		
		else if (obj instanceof TreePaneNode)
			setIcon(Icons.TREE_TREE);
		
		else if (obj instanceof PDMResultsNode)
			setIcon(Icons.TREE_PDM);
		
		else if (obj instanceof HMMResultsNode)
			setIcon(Icons.TREE_HMM);
		
		else if (obj instanceof DSSResultsNode)
			setIcon(Icons.TREE_DSS);
		
		else if (obj instanceof LRTResultsNode)
			setIcon(Icons.TREE_LRT);
		
		else if (leaf)
			setIcon(new DefaultTreeCellRenderer().getLeafIcon());
				
		this.selected = selected;
				
		if (selected)
			setForeground((Color) UIManager.get("Tree.selectionForeground"));
		else
			setForeground((Color) UIManager.get("Tree.foreground"));
		
		return this;
	}
	
	public void paintComponent(Graphics g)
	{
		Icon icon = getIcon();
		
		int offset = 0;
		if (icon != null && getText() != null)
			offset = (icon.getIconWidth() + getIconTextGap());
		
		if (selected)
		{
			g.setColor(bColor);
			g.fillRect(offset, 0, 500 - offset, getHeight() - 1);
		}
		
		super.paintComponent(g);
	}
}
	