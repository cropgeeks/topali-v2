// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.gui.*;
import topali.var.Utils;

public class ImportOptionsDialog extends JDialog implements ActionListener
{
	private JRadioButton rImport, rCDNA, rAlign, rMulti;

	private JButton bOK, bCancel;

	private boolean isOK = false;

	public ImportOptionsDialog(WinMain winMain)
	{
		super(winMain, "Import Alignment", true);

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
		DblClickListener dblListener = new DblClickListener();

		rImport = new JRadioButton(
				"by using an existing file on disk that already "
						+ "contains aligned DNA");
		rImport.setMnemonic(KeyEvent.VK_E);
		rImport.addMouseListener(dblListener);
		rCDNA = new JRadioButton(
				"by creating a new DNA alignment from unaligned cDNAs and "
						+ "a guide protein alignment");
		rCDNA.setMnemonic(KeyEvent.VK_C);
		rCDNA.addMouseListener(dblListener);
		rAlign = new JRadioButton("by performing a (clustal) alignment on "
				+ "existing, unaligned DNA");
		rAlign.setMnemonic(KeyEvent.VK_P);
		rAlign.setEnabled(false);
		rMulti = new JRadioButton(
				"by selecting a folder of multiple, multiple alignments "
						+ "for comparitive genomic analysis");
		rMulti.setMnemonic(KeyEvent.VK_F);
		rMulti.addMouseListener(dblListener);

		switch (Prefs.gui_import_method)
		{
		case 0:
			rImport.setSelected(true);
			break;
		case 1:
			rCDNA.setSelected(true);
			break;
		case 2:
			rAlign.setSelected(true);
			break;
		case 3:
			rMulti.setSelected(true);
			break;
		}

		ButtonGroup group = new ButtonGroup();
		group.add(rImport);
		group.add(rCDNA);
		group.add(rAlign);
		group.add(rMulti);

		JPanel p1 = new JPanel(new GridLayout(4, 1));
		p1.setBorder(BorderFactory
				.createTitledBorder("Please select how you'd "
						+ "like to import data into TOPALi:"));
		p1.add(rImport);
		p1.add(rCDNA);
		p1.add(rAlign);
		p1.add(rMulti);

		JPanel p2 = new JPanel(new BorderLayout());
		p2.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		p2.add(p1);

		return p2;
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "import_alignment");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bOK)
		{
			if (rImport.isSelected())
				Prefs.gui_import_method = 0;
			else if (rCDNA.isSelected())
				Prefs.gui_import_method = 1;
			else if (rAlign.isSelected())
				Prefs.gui_import_method = 2;
			else if (rMulti.isSelected())
				Prefs.gui_import_method = 3;

			isOK = true;
			setVisible(false);
		}
	}

	public boolean isOK()
	{
		return isOK;
	}

	private class DblClickListener extends MouseAdapter
	{
		public void mouseClicked(MouseEvent e)
		{
			if (e.getClickCount() != 2)
				return;

			bOK.doClick();
		}
	}
}