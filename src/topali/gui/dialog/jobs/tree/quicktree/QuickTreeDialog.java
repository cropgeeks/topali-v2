// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.quicktree;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.data.Prefs;
import topali.var.utils.Utils;

public class QuickTreeDialog extends JDialog implements ActionListener
{

	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	private QuickTreeDialogPanel panel;
	
	private boolean dna;
	
	public int bs = -1;
	public double tstv;
	public double alpha;
	public boolean estimate;
	
	public QuickTreeDialog(Frame owner, boolean dna) {
		super(owner, "Quick Tree Estimation", true);
		this.dna = dna;
		
		this.getContentPane().setLayout(new BorderLayout());
		
		panel = new QuickTreeDialogPanel();
		if(!dna) {
			panel.estimate.setSelected(false);
			panel.estimate.setEnabled(false);
		}
		this.getContentPane().add(panel, BorderLayout.CENTER);
		
		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "f84gamma");
		this.getContentPane().add(bp, BorderLayout.SOUTH);
		
		this.pack();
		this.setResizable(false);
		this.setLocationRelativeTo(owner);
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel) {
			this.bs = -1;
			setVisible(false);
		}

		else if (e.getSource() == bRun) {
			this.bs = (Integer)panel.bs.getValue();
			this.tstv = (Double)panel.tstv.getValue();
			this.alpha = (Double)panel.alpha.getValue();
			this.estimate = panel.estimate.isSelected();
			
			Prefs.qt_alpha = this.alpha;
			Prefs.qt_tstv = this.tstv;
			Prefs.qt_bootstrap = this.bs;
			Prefs.qt_estimate = this.estimate;
			
			setVisible(false);
		}
		
		else if(e.getSource() == bDefault) {
			panel.bs.setValue(Prefs.qt_bootstrap_default);
			panel.tstv.setValue(Prefs.qt_tstv_default);
			panel.alpha.setValue(Prefs.qt_alpha_default);
			if(dna)
			    panel.estimate.setSelected(Prefs.qt_estimate_default);
			else
			    panel.estimate.setSelected(false);
		}
	}
	
	
}
