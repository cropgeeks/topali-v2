// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.mrbayes;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.utils.Utils;

public class MrBayesDialog extends JDialog implements ActionListener
{
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	AdvancedMrBayes advanced;

	WinMain winMain;
	AlignmentData data;
	MBTreeResult result;

	public MrBayesDialog(WinMain winMain, AlignmentData data, TreeResult result) {
		super(winMain, "MrBayes (Standard DNA) Settings", true);

		this.winMain = winMain;
		this.data = data;
		this.result = (MBTreeResult)result;

		init();

		if (result != null)
		    advanced.initPrevResult((MBTreeResult)result);

		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private void init()
	{
		setLayout(new BorderLayout());

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "mrbayes");
		add(bp, BorderLayout.SOUTH);

		advanced = new AdvancedMrBayes(data);
		add(advanced, BorderLayout.CENTER);
	}

	private void ok(boolean remote)
	{
		this.result = advanced.onOK();
		Prefs.mb_type = 0;

		result.isRemote = remote;
		result.setPartitionStart(data.getActiveRegionS());
		result.setPartitionEnd(data.getActiveRegionE());
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.guiName = "#"+runNum+" Tree (MrBayes)";
		result.jobName = "MrBayes Tree Estimation";

		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();

		if (SysPrefs.isWindows)
			result.mbPath = Utils.getLocalPath() + "\\mb.exe";
		else
			result.mbPath = Utils.getLocalPath() + "/mrbayes/mb";

		setVisible(false);
	}

	public MBTreeResult getResult() {
		return this.result;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
		{
			this.result = null;
			setVisible(false);
		}

		else if (e.getSource() == bRun)
			ok((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);

		else if(e.getSource() == bDefault)
			advanced.setDefaults();
	}
}