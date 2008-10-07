// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.raxml;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.utils.Utils;

public class RaxmlCDNADialog extends JDialog implements ActionListener
{
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	WinMain winMain;
	AlignmentData data;
	RaxmlResult result;

	JTabbedPane tabs;
	RaxmlCDNAAdvancedPanel cdnaadvanced;

	public RaxmlCDNADialog(WinMain winMain, AlignmentData data, TreeResult result) {
		super(winMain, "RaxML Settings", true);
		this.winMain = winMain;
		this.data = data;

		init();

		if(result!=null)
			cdnaadvanced.initPrevResult((RaxmlResult)result);

		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private void init()
	{
		setLayout(new BorderLayout());

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "raxml");
		add(bp, BorderLayout.SOUTH);

		cdnaadvanced = new RaxmlCDNAAdvancedPanel(data);
		add(cdnaadvanced, BorderLayout.CENTER);
	}

	private void ok(boolean remote)
	{
		this.result = cdnaadvanced.onOK();
		Prefs.rax_type = 1;

		result.isRemote = remote;
		result.setPartitionStart(data.getActiveRegionS());
		result.setPartitionEnd(data.getActiveRegionE());
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.guiName = "#"+runNum+" Tree (RaxML)";
		result.jobName = "RaxML Tree Estimation";

		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();

		if (SysPrefs.isWindows)
			result.raxmlPath = Utils.getLocalPath() + "\\raxml.exe";
		else
			result.raxmlPath = Utils.getLocalPath() + "/raxml/raxmlHPC";

		setVisible(false);
	}

	public RaxmlResult getResult() {
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
			cdnaadvanced.setDefaults();
	}
}