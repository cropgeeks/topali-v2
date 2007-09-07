// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.tree;

import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.AnalysisInfoDialog;
import topali.var.Utils;

class TreePanelToolBar extends JToolBar implements ActionListener
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private TreePane treePane;

	private TreePanel panel;

	private TreeResult tResult;
	private SequenceSet ss;
	
	JButton bExport, bCluster;
	JButton bInfo;

	JButton bRoot;
	
	JButton bAncestor;
	
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
		
		bInfo = (JButton)WinMainToolBar.getButton(false, null, "tre11", Icons.ANALYSIS_INFO, null);
		
		bRoot = (JButton)WinMainToolBar.getButton(false, null, "tre10", Icons.MIDPOINT_ROOT, null);
		
		bAncestor = (JButton)WinMainToolBar.getButton(false, null, "tre12", Icons.TREE_ANCESTOR, null);
		
		bExport.addActionListener(this);
		bDrawNormal.addActionListener(this);
		bDrawCircular.addActionListener(this);
		bDrawNewHamp.addActionListener(this);
		bSizedToFit.addActionListener(this);
		bFloat.addActionListener(this);
		bCluster.addActionListener(this);
		bViewCluster.addActionListener(this);
		bInfo.addActionListener(this);
		
		bRoot.addActionListener(this);
		
		bAncestor.addActionListener(this);
		bAncestor.setEnabled(tResult instanceof MBTreeResult || tResult instanceof PhymlResult);
		
		add(new JLabel(" "));
		add(bExport);
		addSeparator();
		add(bDrawNormal);
		add(bDrawCircular);
		add(bDrawNewHamp);
		addSeparator();
		add(bRoot);
		addSeparator();
		add(bCluster);
		add(bViewCluster);
		addSeparator();
		add(bSizedToFit);
		add(bFloat);
		addSeparator();
		add(bAncestor);
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

		else if(e.getSource() == bInfo) {
			AnalysisInfoDialog ad = new AnalysisInfoDialog(tResult, false);
			ad.setText(getTreeInfo());
			ad.setVisible(true);
		}
		
		else if(e.getSource()==bRoot) {
			try
			{
				TreeResult res = new TreeResult(tResult);
				res.x = tResult.x+50;
				res.y = tResult.y+50;
				res.setTreeStr(Utils.midPointRoot(tResult.getTreeStr()));
				res.guiName = tResult.guiName+" (midpoint rooted)";
				treePane.addNewTree(res);
//				InternalTreeFrame tmp = new InternalTreeFrame(treePane, ss, res, 0);
//				tmp.addInternalFrameListener(treePane);
//				tmp.setVisible(true);
//				treePane.add(tmp);
//				tmp.setSelected(true);
			} catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}
		
		else if(e.getSource()==bAncestor) {
			
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;
			
			FastMLResult fastml = new FastMLResult();
			fastml.isRemote = remote;
			if(Prefs.isWindows)
				fastml.fastmlPath = Utils.getLocalPath() + "fastml.exe";
			else
				fastml.fastmlPath = Utils.getLocalPath() + "fastml/fastml";
			
			fastml.selectedSeqs = tResult.selectedSeqs;
			fastml.origTree = tResult.getTreeStr();
			
			
			fastml.guiName = "Ancestor";
			fastml.jobName = "Ancestral sequence creation based on '"+tResult.guiName+"'";
			TOPALi.winMain.anlsRunFastML(fastml);
		}
		
		//WinMainMenuBar.aFileSave.setEnabled(true);
		//WinMainMenuBar.aVamCommit.setEnabled(true);
		ProjectState.setDataChanged();
	}
	
	private String getTreeInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append(tResult.guiName+"\n\n");
		sb.append("Runtime: " + ((tResult.endTime - tResult.startTime) / 1000)+ " seconds\n");
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