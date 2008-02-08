// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import static topali.gui.WinMainMenuBar.aFileExportDataSet;

import java.awt.print.Printable;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import topali.data.AlignmentData;
import topali.gui.*;
import topali.gui.tree.TreePane;
import topali.i18n.Text;

public class TreePaneNode extends INode implements IPrintable
{
	private TreePane treePane;

	TreePaneNode(AlignmentData data)
	{
		super(data);

		treePane = new TreePane(data);
	}

	@Override
	public int getTipsKey()
	{
		return WinMainTipsPanel.TIPS_TRE;
	}

	@Override
	public String toString()
	{
		int count = treePane.getFrameCount();

		return Text.format(Text.I18N.getString("TreePaneNode.gui01"), count);
	}

	@Override
	public JComponent getPanel()
	{
		return treePane;
	}

	public TreePane getTreePane()
	{
		return treePane;
	}

	@Override
	public void setMenus()
	{
		aFileExportDataSet.setEnabled(true);

		//aAnlsCreateTree.setEnabled(true);

	}
	
	void setTreeNode(DefaultMutableTreeNode node)
	{
		treePane.setTreeNode(node);
	}
	
	public boolean isPrintable()
	{
		return treePane.isPrintable();
	}
	
	public Printable[] getPrintables()
	{
		return treePane.getPrintables();
	}
}