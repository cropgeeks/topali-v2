// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.i18n.Text;
import topali.var.utils.Utils;
import scri.commons.gui.MsgBox;

public class AnalysisInfoDialog extends JDialog implements ActionListener
{
	private AlignmentResult aResult;

	private JButton bClose, bClipboard, bSubmit, bHelp;

	private JTextArea text;
	
	public AnalysisInfoDialog(AlignmentResult aResult) {
		super(TOPALi.winMain, "Analysis Information", true);
		this.aResult = aResult;
		
		add(createControls());
		getRootPane().setDefaultButton(bClose);
		Utils.addCloseHandler(this, bClose);
		;

		setSize(500, 375);
		setResizable(false);
		setLocationRelativeTo(TOPALi.winMain);
	}

	private JPanel createControls()
	{
		bClose = new JButton(Text.I18N.getString("close"));
		bClose.addActionListener(this);

		bClipboard = new JButton(Text.I18N.getString("clipboard_1"));
		bClipboard.addActionListener(this);
		
		bSubmit = new JButton("Resubmit Job");
		bSubmit.addActionListener(this);

		bHelp = TOPALiHelp.getHelpButton("analysis_info");

		text = new JTextArea("blah blah");
		Utils.setTextAreaDefaults(text);

		JScrollPane sp = new JScrollPane(text);

		JPanel p1 = new JPanel(new BorderLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		p1.add(sp);

		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
		p2.add(bClose);
		p2.add(bClipboard);
		if(aResult.isResubmittable())
			p2.add(bSubmit);
		p2.add(bHelp);

		JPanel p3 = new JPanel(new BorderLayout());
		p3.add(p1, BorderLayout.CENTER);
		add(p2, BorderLayout.SOUTH);

		return p3;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClose)
			setVisible(false);

		else if (e.getSource() == bClipboard) {
			Utils.copyToClipboard(text.getText());
			MsgBox.msg(Text.I18N.getString("clipboard_2"), MsgBox.INF);
		}
		
		else if (e.getSource() == bSubmit)
			resubmitAnalysis();
	}

	public void setText(String str)
	{
		text.setText(str);
		text.setCaretPosition(0);
	}

	private void resubmitAnalysis()
	{
		String msg = "This will reselect the sequences used during this analysis, "
				+ "and prepare the job settings dialog with the values used during its "
				+ "run. Continue?";
		if (MsgBox.yesno(msg, 0) != JOptionPane.YES_OPTION)
			return;

		setVisible(false);

		// Reselect the sequences
		TOPALi.winMain.menuAnlsReselectSequences(aResult.selectedSeqs);

		// Resubmit a PDM job
		if (aResult instanceof PDMResult)
			TOPALi.winMain.menuAnlsRunPDM((PDMResult) aResult);

		// Resubmit a PDM2 job
		if (aResult instanceof PDM2Result)
			TOPALi.winMain.menuAnlsRunPDM2((PDM2Result) aResult);

		// Resubmit an HMM job
		if (aResult instanceof HMMResult)
			TOPALi.winMain.menuAnlsRunHMM((HMMResult) aResult);

		// Resubmit a DSS job
		else if (aResult instanceof DSSResult)
			TOPALi.winMain.menuAnlsRunDSS((DSSResult) aResult);

		// Resubmit a LRT job
		else if (aResult instanceof LRTResult)
			TOPALi.winMain.menuAnlsRunLRT((LRTResult) aResult);
		
		else if (aResult instanceof CodeMLResult) {
			CodeMLResult res = (CodeMLResult) aResult;
			if(res.type.equals(CodeMLResult.TYPE_SITEMODEL))
				TOPALi.winMain.menuAnlsRunCodeMLSite(res);
			else if(res.type.equals(CodeMLResult.TYPE_BRANCHMODEL))
				TOPALi.winMain.menuAnlsRunCodeMLBranch(res);
		}
		
		else if(aResult instanceof CodonWResult) {
			CodonWResult res = (CodonWResult) aResult;
			TOPALi.winMain.menuAnlsRunCW(res);
		}
		
		else if(aResult instanceof ModelTestResult) {
		    ModelTestResult res = (ModelTestResult) aResult;
		    TOPALi.winMain.menuAnlsRunMT(res);
		}
		
		else if(aResult instanceof MBTreeResult) {
			MBTreeResult res = (MBTreeResult)aResult;
			TOPALi.winMain.menuAnlsMrBayes(res);
		}
		
		else if(aResult instanceof PhymlResult) {
			PhymlResult res = (PhymlResult)aResult;
			TOPALi.winMain.menuAnlsPhyml(res);
		}
		
		else if(aResult instanceof RaxmlResult) {
			RaxmlResult res = (RaxmlResult)aResult;
			TOPALi.winMain.menuAnlsRaxml(res);
		}
	}
}