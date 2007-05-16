// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import topali.data.*;
import topali.gui.Prefs;
import topali.gui.Text;
import topali.var.Utils;

public class CodonWDialog extends JDialog implements ActionListener
{

	AlignmentData data;
	SequenceSet ss;
	JButton bOK, bCancel;
	JComboBox cbCode;
	CodonWResult result;
	
	public CodonWDialog(AlignmentData data, CodonWResult res) {
		this.data = data;
		this.ss = data.getSequenceSet();
		init();
		
		if(res!=null)
			setSelectedCode(res.geneticCode);
		
		pack();
		setLocationRelativeTo(null);
		setModal(true);
	}
	
	private void init() {
		JPanel p1 = new JPanel();
		cbCode = new JComboBox(SequenceSetParams.availCodes);
		String code = ss.getParams().getGeneticCode();
		setSelectedCode(code);
		
		p1.add(new JLabel("Genetic Code: "));
		p1.add(cbCode);
		
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		JPanel p2 = Utils.getButtonPanel(this, bOK, bCancel, "codonw");
		
		this.add(p1, BorderLayout.CENTER);
		this.add(p2, BorderLayout.SOUTH);
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
		if(e.getSource().equals(bOK)) {
			ss.getParams().setGeneticCode((String)cbCode.getSelectedItem());
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;
			
			result = new CodonWResult();
			
			if(Prefs.isWindows)
				result.codonwPath = Utils.getLocalPath() + "CodonW.exe";
			else
				result.codonwPath = Utils.getLocalPath() + "codonW/codonw";
			
			result.isRemote = remote;
			result.geneticCode = (String)cbCode.getSelectedItem();
			result.selectedSeqs = ss.getSelectedSequenceSafeNames();
			
			int runNum = data.getTracker().getCwRunCount() + 1;
			data.getTracker().setCwRunCount(runNum);
			result.guiName = "CodonW Result " + runNum;
			result.jobName = "CodonW on " + data.name
					+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
					+ " sequences)";
			
			this.setVisible(false);
		}
		else if(e.getSource().equals(bCancel)) {
			result = null;
			this.setVisible(false);
		}
	}
	
	public CodonWResult getResult() {
		return result;
	}
}
