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

	ModelTestResult[] results;
	AlignmentData data;
	SequenceSet ss;

	public MTDialog(WinMain winMain, AlignmentData data, ModelTestResult res) {
		super(winMain, "Model Selection", true);

		this.data = data;
		this.ss = data.getSequenceSet();
		init();

		pack();
		setLocationRelativeTo(winMain);
	}

	public ModelTestResult[] getResult() {
		return results;
	}

	private void init() {
		this.setLayout(new BorderLayout());

		panel = new MTDialogPanel(data, null, ss.getProps().isNucleotides());
		add(panel, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "modelselection");
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}

	public void actionPerformed(ActionEvent e)
	{
		// If it's possible to split the alignment into three codon interlaced
		// regions (eg, 1, 4, 7 in the first region... 2, 5, 8, in the 2nd etc)
		// then make three identical result objects ready for running three
		// separate ModelSelection jobs

		// (otherwise just run MS once)

		if (e.getSource().equals(bRun))
		{
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;

			// If we need to run MS three times...
			if (panel.checkProteinCoding.isSelected() && panel.checkProteinCoding.isEnabled())
			{
				results = new ModelTestResult[3];

				for (int i = 0; i < results.length; i++)
				{
					results[i] = createResult(i, true);
					results[i].isRemote = remote;
					results[i].splitType = i+1;
				}
			}
			// Or just once...
			else
			{
				results = new ModelTestResult[1];
				results[0] = createResult(0, false);
				results[0].isRemote = remote;
				results[0].splitType = ModelTestResult.SINGLE_MODEL_RUN;
			}


			setVisible(false);
		}

		else if(e.getSource().equals(bCancel))
			this.setVisible(false);

		else if(e.getSource().equals(bDefault))
			panel.setDefaults();
	}

	// Creates a ModelTestResult object ready to be sent to the analysis
	private ModelTestResult createResult(int msRunNum, boolean usePNames)
	{
		ModelTestResult	res = panel.getResult();

		res.selectedSeqs = ss.getSelectedSequenceSafeNames();

		if(res.sampleCrit==ModelTestResult.SAMPLE_SEQLENGTH) {
//				this.res.sampleSize = data.getActiveRegionE()-data.getActiveRegionS()+1;
			res.sampleSize = data.getSequenceSet().getLength();
		}
		else if(res.sampleCrit==ModelTestResult.SAMPLE_ALGNSIZE) {
//				this.res.sampleSize = (data.getActiveRegionE()-data.getActiveRegionS()+1)*this.res.selectedSeqs.length;
			res.sampleSize = data.getSequenceSet().getLength() * res.selectedSeqs.length;
		}


		int runNum = data.getTracker().getMtRunCount() + 1;

		// Determine how to name the job...
		if (usePNames)
		{
			res.guiName = "Model Selection " + runNum + " (CP" + (msRunNum+1) + ")";
			res.jobName = "Model Selection " + "(CP" + (msRunNum+1) + ") on "
				+ data.getName() + " (" + ss.getSelectedSequences().length
				+ "/" + ss.getSize() + " sequences)";

			if (msRunNum == 2)
				data.getTracker().setMtRunCount(runNum);
		}
		else
		{
			res.guiName = "Model Selection " + runNum;
			res.jobName = "Model Selection on "
				+ data.getName() + " (" + ss.getSelectedSequences().length
				+ "/" + ss.getSize() + " sequences)";

			data.getTracker().setMtRunCount(runNum);
		}

		return res;
	}
}