// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import topali.analyses.*;
import topali.cluster.*;
import topali.data.*;
import topali.gui.*;

import doe.*;

public class AutoPartitionDialog extends JDialog implements ActionListener
{
	private WinMain winMain;
	private AlignmentData data;
	
	private JButton bOK, bCancel;
	private JComboBox resultsCombo;
	private JCheckBox checkDiscard;
	private SpinnerNumberModel discardModel;
	private JSpinner discardSpin;

	public AutoPartitionDialog(WinMain winMain, AlignmentData data, AnalysisResult result)
	{
		super(winMain, "Auto-Partition Alignment", true);
		
		this.winMain = winMain;
		this.data = data;
		
		setLayout(new BorderLayout(5, 5));
		add(getButtonPanel(), BorderLayout.SOUTH);
		add(getControls(), BorderLayout.CENTER);
		Utils.addCloseHandler(this, bCancel);
		
		if (data.getResults().size() == 0)
		{
			MsgBox.msg("Auto-partitioning the alignment is only possible once "
				+ "a PDM, HMM, or DSS analysis result set has been generated.",
				MsgBox.ERR);
			return;
		}
		else if (result != null)
			resultsCombo.setSelectedItem(result);
				
		pack();		
		setResizable(false);
		setLocationRelativeTo(winMain);
		setVisible(true);
	}
	
	private JPanel getButtonPanel()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		
		return Utils.getButtonPanel(this, bOK, bCancel, "auto_partition");
	}
	
	private JPanel getControls()
	{
		// Top half (results selection)
		resultsCombo = new JComboBox();
		for (AnalysisResult r: data.getResults())
			if (r.status == JobStatus.COMPLETED)
				resultsCombo.addItem(r);
		
		JLabel resultsLabel = new JLabel("Create partitions for the alignment "
			+ "based on the following results:");
		resultsLabel.setLabelFor(resultsCombo);
		resultsLabel.setDisplayedMnemonic(KeyEvent.VK_P);		
		
		discardModel = new SpinnerNumberModel(Prefs.gui_auto_min, 1, 250, 1);
		discardSpin = new JSpinner(discardModel);
		discardSpin.setEnabled(Prefs.gui_auto_discard);
		((JSpinner.NumberEditor)discardSpin.getEditor()).getTextField()
			.setToolTipText("Specifies the minimum size a partition must be "
			+ "to be included");
		
		checkDiscard = new JCheckBox("Discard partitions less than",
			Prefs.gui_auto_discard);
		checkDiscard.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				discardSpin.setEnabled(checkDiscard.isSelected());
			}
		});
		checkDiscard.setMnemonic(KeyEvent.VK_D);

		JLabel label2 = new JLabel("  nucleotides in length");
		
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p1.add(checkDiscard);
		p1.add(discardSpin);
		p1.add(label2);
		
		
		DoeLayout layout1 = new DoeLayout();		
		layout1.add(resultsLabel, 0, 0, 1, 1, new Insets(10, 10, 5, 10));
		layout1.add(resultsCombo, 0, 1, 1, 1, new Insets(2, 15, 5, 10));
		layout1.add(p1, 0, 2, 1, 1, new Insets(5, 5, 0, 10));
		
	
		return layout1.getPanel();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bOK)
			partition();
	}
	
	
	private void partition()
	{
		setVisible(false);
		
		Prefs.gui_auto_discard = checkDiscard.isSelected();
		Prefs.gui_auto_min = discardModel.getNumber().intValue();
		
		PartitionMaker pm = new PartitionMaker(data);
		pm.setDiscard(Prefs.gui_auto_discard);
		pm.setMinLength(Prefs.gui_auto_min);

		AnalysisResult result = (AnalysisResult) resultsCombo.getSelectedItem();
		
		if (result instanceof PDMResult)
		{
			PDMResult pdm = (PDMResult) result;
			pm.autoPartition(pdm.locData, pdm.calculateThreshold());
		}
		
		else if (result instanceof HMMResult)
		{
			HMMResult hmm = (HMMResult) result;
			pm.autoPartitionHMM(hmm.data1, hmm.data2, hmm.data3, hmm.thresholdCutoff);
		}
		
		else if (result instanceof DSSResult)
		{
			DSSResult dss = (DSSResult) result;
			pm.autoPartition(dss.data,
				AnalysisUtils.getArrayValue(dss.thresholds, dss.thresholdCutoff));
		}
		
		else if (result instanceof LRTResult)
		{
			LRTResult lrt = (LRTResult) result;
			pm.autoPartition(lrt.data,
				AnalysisUtils.getArrayValue(lrt.thresholds, lrt.thresholdCutoff));
		}
		
		cleanup();
	}
	
	private void cleanup()
	{
		winMain.pDialog.refreshPartitionList();
		WinMainMenuBar.aFileSave.setEnabled(true);
	
		int num = data.getTopaliAnnotations().getPartitionAnnotations().countRegions();
		
		if (num == 1)
			MsgBox.msg(Text.GuiDiag.getString("AutoPartitionDialog.msg01"),
				MsgBox.INF);
		else
			MsgBox.msg(Text.format(Text.GuiDiag.getString(
				"AutoPartitionDialog.msg02"), num), MsgBox.INF);
	}
}
