// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.mt;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.WinMain;
import topali.var.utils.Utils;

public class MTDialog extends JDialog implements ActionListener
{

	MTDialogPanel panel;
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	ModelTestResult res;
	AlignmentData data;
	SequenceSet ss;

	public MTDialog(WinMain winMain, AlignmentData data, ModelTestResult res) {
		super(winMain, "Model Selection", true);

		this.data = data;
		this.ss = data.getSequenceSet();
		this.res = res;
		init();

		pack();
		setLocationRelativeTo(winMain);
	}

	public ModelTestResult getResult() {
		return this.res;
	}

	private void init() {
		this.setLayout(new BorderLayout());

		panel = new MTDialogPanel(this.res, ss.getProps().isNucleotides());
		add(panel, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "modelselection");
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}


	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource().equals(bRun)) {
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;
			this.res = panel.getResult();
			this.res.isRemote = remote;
			this.res.selectedSeqs = ss.getSelectedSequenceSafeNames();
			
			if(this.res.sampleCrit==ModelTestResult.SAMPLE_SEQLENGTH) {
				this.res.sampleSize = data.getActiveRegionE()-data.getActiveRegionS()+1;
			}
			else if(this.res.sampleCrit==ModelTestResult.SAMPLE_ALGNSIZE) {
				this.res.sampleSize = (data.getActiveRegionE()-data.getActiveRegionS()+1)*this.res.selectedSeqs.length;
			}
			
			int runNum = data.getTracker().getMtRunCount() + 1;
			data.getTracker().setMtRunCount(runNum);
			this.res.guiName = "Model Selection " + runNum;
			this.res.jobName = "Model Selection on " + data.getName() + " ("
					+ ss.getSelectedSequences().length + "/" + ss.getSize()
					+ " sequences)";
			this.setVisible(false);
		}
		else if(e.getSource().equals(bCancel)) {
			this.res = null;
			this.setVisible(false);
		}
		else if(e.getSource().equals(bDefault)) {
			panel.setDefaults();
		}
	}

}
