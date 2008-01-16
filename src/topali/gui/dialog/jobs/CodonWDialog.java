// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.*;
import topali.var.utils.Utils;

public class CodonWDialog extends JDialog implements ActionListener
{

	AlignmentData data;
	SequenceSet ss;
	//JButton bOK, bCancel;
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	JComboBox cbCode;
	CodonWResult result;

	public CodonWDialog(AlignmentData data, CodonWResult res) {
		super(TOPALi.winMain, "Check Codon Usage", true);
		this.data = data;
		this.ss = data.getSequenceSet();
		init();

		if(res!=null) {
			setSelectedCode(res.geneticCode);
		}

		pack();
		setLocationRelativeTo(TOPALi.winMain);
	}

	private void init() {
		JPanel p1 = new JPanel();
		cbCode = new JComboBox(SequenceSetParams.availCodes);
		String code = ss.getParams().getGeneticCode();
		setSelectedCode(code);

		p1.add(new JLabel("Genetic Code: "));
		p1.add(cbCode);

		this.add(p1, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "codonw");
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}

	private void setSelectedCode(String code) {
		for(int i=0; i<SequenceSetParams.availCodes.length; i++)
			if(SequenceSetParams.availCodes[i].equals(code)) {
				cbCode.setSelectedIndex(i);
				break;
			}
	}

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource().equals(bRun)) {
			ss.getParams().setGeneticCode((String)cbCode.getSelectedItem());
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;

			result = new CodonWResult();

			if(SysPrefs.isWindows)
				result.codonwPath = Utils.getLocalPath() + "CodonW.exe";
			else
				result.codonwPath = Utils.getLocalPath() + "codonW/codonw";

			result.isRemote = remote;
			result.geneticCode = (String)cbCode.getSelectedItem();
			result.selectedSeqs = ss.getSelectedSequenceSafeNames();

			int runNum = data.getTracker().getCwRunCount() + 1;
			data.getTracker().setCwRunCount(runNum);
			result.guiName = "CodonW " + runNum;
			result.jobName = "CodonW on " + data.name
					+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
					+ " sequences)";

			this.setVisible(false);
		}
		else if(e.getSource().equals(bCancel)) {
			result = null;
			this.setVisible(false);
		}
		else if(e.getSource().equals(bDefault)) {
			setSelectedCode(SequenceSetParams.GENETICCODE_UNIVERSAL);
		}
	}

	public CodonWResult getResult() {
		return result;
	}
}
