// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.results;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.gui.*;
import topali.var.Utils;
import doe.MsgBox;

public class TreeToolTipDialog extends JDialog implements ActionListener
{
	private boolean enable;

	private int window, length;

	private JButton bClose, bHelp;

	private JCheckBox checkEnable;

	private SpinnerNumberModel numModel;

	private JSpinner numSpin;

	public TreeToolTipDialog(boolean enable, int window, int length)
	{
		super(MsgBox.frm, "Tree ToolTips", true);

		this.enable = enable;
		this.window = window;
		this.length = length;

		add(getControls(), BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bClose);
		Utils.addCloseHandler(this, bClose);

		pack();

		setLocationRelativeTo(MsgBox.frm);
		setResizable(false);
		setVisible(true);
	}

	private JPanel getControls()
	{
		checkEnable = new JCheckBox("Enable phylogenetic tree tooltips", enable);
		if (Prefs.isWindows)
			checkEnable.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		if (window > length)
			window = length / 2;

		numModel = new SpinnerNumberModel(window, 1, length, 5);
		numSpin = new JSpinner(numModel);
		Dimension d = numSpin.getPreferredSize();
		d.width = 55;
		numSpin.setPreferredSize(d);

		JLabel label1 = new JLabel(
				"Window size under mouse cursor (in nucleotides): ");
		label1.setDisplayedMnemonic(KeyEvent.VK_A);
		label1.setLabelFor(numSpin);

		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p1.add(label1);
		p1.add(numSpin);

		JPanel p2 = new JPanel(new GridLayout(2, 1, 0, 0));
		p2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p2.add(checkEnable);
		p2.add(p1);

		return p2;
	}

	private JPanel getButtons()
	{
		bClose = new JButton(Text.Gui.getString("close"));
		bClose.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("tree_tooltips");

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

	public boolean isOptionChecked()
	{
		return checkEnable.isSelected();
	}

	public int getWindowSize()
	{
		return numModel.getNumber().intValue();
	}
}