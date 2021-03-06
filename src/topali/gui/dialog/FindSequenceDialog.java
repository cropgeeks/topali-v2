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
import scri.commons.gui.*;

public class FindSequenceDialog extends JDialog implements ActionListener
{
	private SequenceSet ss;

	private AlignmentPanel panel;

	private JButton bOK, bCancel;

	private JTextField seqName;

	private JCheckBox highlight, matchCase;

	public FindSequenceDialog(WinMain winMain, AlignmentPanel panel)
	{
		super(winMain, Text.get("FindSequenceDialog.gui01"), true);

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
		seqName = new JTextField(Prefs.gui_find_name, 40);
		seqName.setToolTipText(Text.get("FindSequenceDialog.gui07"));
		seqName.selectAll();

		highlight = new JCheckBox(Text.get("FindSequenceDialog.gui02"),
				Prefs.gui_find_highlight);
		highlight.setToolTipText(Text.get("FindSequenceDialog.gui05"));
		highlight.setMnemonic(KeyEvent.VK_S);
		matchCase = new JCheckBox(Text.get("FindSequenceDialog.gui03"), Prefs.gui_find_case);
		matchCase.setToolTipText(Text.get("FindSequenceDialog.gui06"));
		matchCase.setMnemonic(KeyEvent.VK_M);

		JLabel label = new JLabel(Text.get("FindSequenceDialog.gui04"));
		label.setDisplayedMnemonic(KeyEvent.VK_F);
		label.setLabelFor(seqName);

		DoeLayout layout = new DoeLayout();
		layout.add(label, 0, 0, 1, 1, new Insets(10, 10, 2, 10));
		layout.add(seqName, 0, 1, 1, 1, new Insets(5, 10, 5, 10));
		layout.add(highlight, 0, 2, 1, 1, new Insets(0, 6, 0, 10));
		layout.add(matchCase, 0, 3, 1, 1, new Insets(0, 6, 5, 10));

		return layout.getPanel();
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.get("ok"));
		bCancel = new JButton(Text.get("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "find_sequence");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bOK)
		{
			if (seqName.getText().length() == 0)
			{
				MsgBox.msg(Text.get("FindSequenceDialog.gui08"),
						MsgBox.ERR);
				return;
			}

			findSequence();
		}
	}

	private void findSequence()
	{
		int index = ss.getIndexOf(seqName.getText(), matchCase.isSelected());

		if (index == -1)
		{
			String msg = Text.get("FindSequenceDialog.gui09", seqName.getText());
			MsgBox.msg(msg, MsgBox.INF);

			return;
		} else
		{
			panel.findSequence(index, highlight.isSelected());
			setVisible(false);
		}

		Prefs.gui_find_name = seqName.getText();
		Prefs.gui_find_highlight = highlight.isSelected();
		Prefs.gui_find_case = matchCase.isSelected();
	}
}