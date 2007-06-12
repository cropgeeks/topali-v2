// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import pal.tree.TreeParseException;
import topali.data.*;
import topali.gui.*;
import topali.gui.atv.ATV;
import topali.gui.dialog.AnalysisInfoDialog;

class TreePanelToolBar extends JToolBar implements ActionListener
{
	private TreePane treePane;

	private TreePanel panel;

	private TreeResult tResult;
	private SequenceSet ss;
	
	JButton bExport, bCluster;
	JButton bATV, bInfo;

	JToggleButton bDrawNormal, bDrawCircular, bDrawNewHamp, bSizedToFit,
			bFloat, bViewCluster;

	TreePanelToolBar(TreePane treePane, TreePanel panel, TreeResult tree, SequenceSet ss)
	{
		this.treePane = treePane;
		this.panel = panel;
		this.tResult = tree;
		this.ss = ss;
		
		setFloatable(false);
		setBorderPainted(false);
		setVisible(Prefs.gui_toolbar_visible);

		bExport = (JButton) WinMainToolBar.getButton(false, "tre05", "tre06",
				Icons.EXPORT, null);
		bDrawNormal = (JToggleButton) WinMainToolBar.getButton(true, null,
				"tre02", Icons.TREE_NORMAL, null);
		bDrawNormal.setSelected(tree.viewMode == TreeResult.NORMAL);
		bDrawCircular = (JToggleButton) WinMainToolBar.getButton(true, null,
				"tre03", Icons.TREE_CIRCULAR, null);
		bDrawCircular.setSelected(tree.viewMode == TreeResult.CIRCULAR);
		bDrawNewHamp = (JToggleButton) WinMainToolBar.getButton(true, null,
				"tre04", Icons.TREE_NEWHAMP, null);
		bDrawNewHamp.setSelected(tree.viewMode == TreeResult.TEXTUAL);
		bSizedToFit = (JToggleButton) WinMainToolBar.getButton(true, null,
				"tre01", Icons.SIZED_TO_FIT, null);
		bSizedToFit.setSelected(tree.isSizedToFit);
		bCluster = (JButton) WinMainToolBar.getButton(false, null, "tre08",
				Icons.CLUSTER, null);
		
		bFloat = (JToggleButton) WinMainToolBar.getButton(true, null, "tre07",
				Icons.FLOAT, null);
		bViewCluster = (JToggleButton) WinMainToolBar.getButton(true, null,
				"tre09", Icons.TREE_NEWHAMP, null);

		bATV = (JButton)WinMainToolBar.getButton(false, null, "tre10", Icons.UP16, null);
		bInfo = (JButton)WinMainToolBar.getButton(false, null, "tre11", Icons.ANALYSIS_INFO, null);
		
		bExport.addActionListener(this);
		bDrawNormal.addActionListener(this);
		bDrawCircular.addActionListener(this);
		bDrawNewHamp.addActionListener(this);
		bSizedToFit.addActionListener(this);
		bFloat.addActionListener(this);
		bCluster.addActionListener(this);
		bViewCluster.addActionListener(this);
		bATV.addActionListener(this);
		bInfo.addActionListener(this);
		
		add(new JLabel(" "));
		add(bExport);
		addSeparator();
		add(bDrawNormal);
		add(bDrawCircular);
		add(bDrawNewHamp);
		addSeparator();
		add(bCluster);
		add(bViewCluster);
		addSeparator();
		add(bSizedToFit);
		add(bFloat);
		add(bATV);
		addSeparator();
		add(bInfo);
		add(new JLabel(" "));

		// Add the toggle buttons into a group so that only one can be selected
		ButtonGroup group = new ButtonGroup();
		group.add(bDrawNormal);
		group.add(bDrawCircular);
		group.add(bDrawNewHamp);
		group.add(bViewCluster);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bExport)
			TreePanelUtils.exportTree(panel);

		if (e.getSource() == bDrawNormal)
			panel.setViewMode(TreeResult.NORMAL);

		else if (e.getSource() == bDrawCircular)
			panel.setViewMode(TreeResult.CIRCULAR);

		else if (e.getSource() == bDrawNewHamp)
			panel.setViewMode(TreeResult.TEXTUAL);

		else if (e.getSource() == bSizedToFit)
			panel.setSizedToFit(bSizedToFit.isSelected());

		else if (e.getSource() == bFloat)
			treePane.toggleFloating(panel, bFloat.isSelected());

		else if (e.getSource() == bCluster)
			TreePanelUtils.cluster(panel);

		else if (e.getSource() == bViewCluster)
			panel.setViewMode(TreeResult.CLUSTER);

		else if(e.getSource() == bATV) {
			try
			{
				ATV atv = new ATV(tResult.getTreeStrActual(ss), tResult.getTitle(), null, null);
				SwingUtilities.invokeLater(atv);
			} catch (TreeParseException e1)
			{
				e1.printStackTrace();
			}
		}
		else if(e.getSource() == bInfo) {
			AnalysisInfoDialog ad = new AnalysisInfoDialog(tResult);
			ad.setText(getTreeInfo());
			ad.setVisible(true);
		}
		WinMainMenuBar.aFileSave.setEnabled(true);
	}
	
	private String getTreeInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append("\nSelected sequences:\n");
		for (String seq : tResult.selectedSeqs)
			sb.append("\n" + ss.getNameForSafeName(seq));
		sb.append("\n\nSelected Partition:\n"+tResult.getPartitionStart()+"-"+tResult.getPartitionEnd());
		
		sb.append("\n\nAdditional parameters:\n");
		sb.append(tResult.info);
		
		sb.append('\n');
		
		return sb.toString();
	}
}