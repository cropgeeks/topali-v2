// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.cml;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.WinMain;
import topali.var.utils.Utils;

public class CMLSiteDialog extends JDialog implements ActionListener
{

	WinMain winmain;
	AlignmentData data;
	CodeMLResult res;

	CMLSitePanel panel;

	public JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	public CMLSiteDialog(WinMain winMain, AlignmentData data, CodeMLResult res) {
		super(winMain, "Positive Selection - Site Models", true);

		this.winmain = winMain;
		this.data = data;
		this.res = res;
		init();

		pack();
		setLocationRelativeTo(winmain);
		setResizable(false);

		getRootPane().setDefaultButton(bRun);
		bRun.requestFocus();
	}

	private void init() {
		panel = new CMLSitePanel(res, this);

		this.setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "cmlsite_settings");
		getContentPane().add(bp, BorderLayout.SOUTH);

		Utils.addCloseHandler(this, bCancel);
	}


	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bRun)
		{
			setVisible(false);

			SequenceSet ss = data.getSequenceSet();

			res = panel.getResult();

			if (SysPrefs.isWindows)
				res.codemlPath = Utils.getLocalPath() + "codeml.exe";
			else
				res.codemlPath = Utils.getLocalPath() + "codeml/codeml";

			res.selectedSeqs = ss.getSelectedSequenceSafeNames();
			res.isRemote = ((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);

			int runNum = data.getTracker().getCodeMLRunCount() + 1;
			data.getTracker().setCodeMLRunCount(runNum);
			res.guiName = "PAML Sites " + runNum;
			res.jobName = "PAML/CodeML Analysis " + runNum + " on " + data.getName()
					+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
					+ " sequences)";

			winmain.submitJob(data, res);
		}

		else if (e.getSource() == bDefault) {
			panel.setDefaults();
		}
	}


}
