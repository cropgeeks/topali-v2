// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.data.SequenceSet;
import topali.gui.*;
import topali.i18n.Text;
import topali.var.utils.Utils;

public class GoToNucDialog extends JDialog implements ActionListener
{
	private SequenceSet ss;

	private AlignmentPanel panel;

	private JButton bOK, bCancel;

	private SpinnerNumberModel model;

	private JSpinner spinner;

	public GoToNucDialog(WinMain winMain, AlignmentPanel panel)
	{
		super(winMain, Text.I18N.getString("GoToNucDialog.gui01"), true);

		this.panel = panel;
		ss = panel.getSequenceSet();

		add(getControls(), BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bCancel);

		pack();

		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private JPanel getControls()
	{
		if (Prefs.gui_goto_nuc < 1 || Prefs.gui_goto_nuc > ss.getLength())
			Prefs.gui_goto_nuc = 1;

		model = new SpinnerNumberModel(Prefs.gui_goto_nuc, 1, ss.getLength(),
				100);
		spinner = new JSpinner(model);
		spinner.requestFocus();
		JLabel label = new JLabel(Text.I18N.getString("GoToNucDialog.gui02"));
		label.setDisplayedMnemonic(KeyEvent.VK_G);
		label.setLabelFor(spinner);

		((JSpinner.NumberEditor) spinner.getEditor()).getTextField()
				.setToolTipText("");

		JPanel p1 = new JPanel(new FlowLayout());
		p1.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		p1.add(label);
		p1.add(spinner);

		return p1;
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.I18N.getString("ok"));
		bCancel = new JButton(Text.I18N.getString("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "go_to_nucleotide");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bOK)
		{
			Prefs.gui_goto_nuc = (model.getNumber()).intValue();
			panel.jumpToPosition(Prefs.gui_goto_nuc, -1, false, false);

			setVisible(false);
		}
	}
}