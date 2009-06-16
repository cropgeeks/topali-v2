// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import topali.gui.*;
import topali.gui.results.ResultPanel;
import topali.var.Utils;
import doe.MsgBox;

public class ThresholdDialog extends JDialog implements ActionListener, ChangeListener
{
	private ResultPanel panel;
	
	private double threshold;

	private JButton bClose, bHelp;

	private SpinnerNumberModel numModel;

	private JSpinner numSpin;

	public ThresholdDialog(ResultPanel panel, double threshold)
	{
		super(MsgBox.frm, "Adjust Threshold Significance", true);

		this.panel = panel;
		this.threshold = threshold;

		add(getControls(), BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bClose);
		Utils.addCloseHandler(this, bClose);

		pack();

		setLocationRelativeTo(MsgBox.frm);
		setResizable(false);
	}

	private JPanel getControls()
	{

		numModel = new SpinnerNumberModel(threshold, 0, 1, 0.01);
		numSpin = new JSpinner(numModel);
		numSpin.addChangeListener(this);
		Dimension d = numSpin.getPreferredSize();
		d.width = 55;
		numSpin.setPreferredSize(d);

		// ((JSpinner.NumberEditor)winSpin.getEditor()).getTextField()
		// .setToolTipText(Text.GuiDiag.getString("DSSSettingsDialog.gui07"));

		JLabel label1 = new JLabel(
				"Adjust threshold (percentile) significance: ");
		label1.setDisplayedMnemonic(KeyEvent.VK_A);
		label1.setLabelFor(numSpin);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
		p1.add(label1);
		p1.add(numSpin);

		return p1;
	}

	private JPanel getButtons()
	{
		bClose = new JButton(Text.Gui.getString("close"));
		bClose.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("adjust_threshold");

		JPanel p1 = new JPanel(new GridLayout(1, 2, 5, 5));
		p1.add(bClose);
		p1.add(bHelp);

		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		p2.add(p1);

		return p2;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClose)
			setVisible(false);
	}

	public void stateChanged(ChangeEvent e)
	{
		float value = numModel.getNumber().floatValue();

		panel.setThreshold(value);
		//WinMainMenuBar.aFileSave.setEnabled(true);
		//WinMainMenuBar.aVamCommit.setEnabled(true);
		ProjectState.setDataChanged();
	}
}