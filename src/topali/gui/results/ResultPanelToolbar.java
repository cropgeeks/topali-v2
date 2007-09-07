// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.event.ActionEvent;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.*;
import doe.MsgBox;

public class ResultPanelToolbar extends JToolBar
{
	// Always enabled:
	JButton bInfo, bReselect;
	Action aInfo, aReselect;
	// Just enabled if ResultPanel contains a graph
	JButton bThres, bAddPart, bAutoPart, bToolTips;
	Action aThres, aAddPart, aAutoPart, aToolTips;

	final ResultPanel resPanel;
	final AlignmentData data;
	final AlignmentResult result;
	
	public ResultPanelToolbar(ResultPanel resPanel, AlignmentData data, AlignmentResult result) {	
		this.resPanel = resPanel;
		this.data = data;
		this.result =result;
		
		addStandardActions();
		add(new JToolBar.Separator());
		addGraphActions();
		
		enableGraphButtons(false);
	}
	
	public void addStandardActions() {
		aInfo = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				AnalysisInfoDialog dialog = new AnalysisInfoDialog(result);
				dialog.setText(resPanel.getAnalysisInfo());
				dialog.setVisible(true);
			}
		};
		
		aReselect = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				String msg = "This will reselect the sequences used at the time of this "
					+ "analysis in the main alignment view window. Continue?";

			if (MsgBox.yesno(msg, 0) == JOptionPane.YES_OPTION)
				TOPALi.winMain.menuAnlsReselectSequences(result.selectedSeqs);
			}
		};
		
		
		bInfo = (JButton) WinMainToolBar.getButton(false, null, "dss06",
				Icons.ANALYSIS_INFO, aInfo);
		bReselect = (JButton) WinMainToolBar.getButton(false, null,
				"dss05", Icons.RESELECT, aReselect);
		
		add(bInfo);
		add(bReselect);
	}
	
	public void addGraphActions() {
		aThres = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				ThresholdDialog diag = new ThresholdDialog(resPanel, result.threshold);
				diag.setVisible(true);
			}
		};
		
		aAddPart = new AbstractAction() {
			public void actionPerformed(ActionEvent e)
			{
				WinMain.rDialog.addCurrentRegion(PartitionAnnotations.class);
				//WinMainMenuBar.aFileSave.setEnabled(true);
				//WinMainMenuBar.aVamCommit.setEnabled(true);
				ProjectState.setDataChanged();
			}
		};
		
		aAutoPart = new AbstractAction() {
			public void actionPerformed(ActionEvent e)
			{
				new AutoPartitionDialog(null, data, result);
			}
		};
		
		aToolTips = new AbstractAction() {
			public void actionPerformed(ActionEvent e)
			{
				TreeToolTipDialog dialog = new TreeToolTipDialog(
						result.useTreeToolTips, result.treeToolTipWindow, data.getSequenceSet().getLength());

				result.useTreeToolTips = dialog.isOptionChecked();
				result.treeToolTipWindow = dialog.getWindowSize();

				//WinMainMenuBar.aFileSave.setEnabled(true);
				//WinMainMenuBar.aVamCommit.setEnabled(true);
				ProjectState.setDataChanged();
			}
		}; 
		
		bThres = (JButton) WinMainToolBar.getButton(false, null, "dss04",
				Icons.ADJUST_THRESHOLD, aThres);
		bAddPart = (JButton) WinMainToolBar.getButton(false, null, "dss03",
				Icons.ADD_PARTITION, aAddPart);
		bAutoPart = (JButton) WinMainToolBar.getButton(false, null, "dss02",
				Icons.AUTO_PARTITION, aAutoPart);
		bToolTips = (JButton) WinMainToolBar.getButton(false, null, "dss07",
				Icons.TREE_TOOLTIPS, aToolTips);
		
		add(bThres);
		add(bAddPart);
		add(bAutoPart);
		add(bToolTips);
	}

	public void enableGraphButtons(boolean b) {
		bThres.setEnabled(b);
		bAddPart.setEnabled(b);
		bAutoPart.setEnabled(b);
		bToolTips.setEnabled(b);
	}
}
