// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.analyses.MakeNA;
import topali.data.AlignmentData;
import topali.fileio.AlignmentLoadException;
import topali.gui.*;
import topali.i18n.Text;
import topali.var.utils.Utils;
import scri.commons.gui.*;

public class MakeNADialog extends JDialog implements ActionListener
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private JTextField dnaText, proText;

	private JButton dnaBrowse, proBrowse;

	private JButton bOK, bCancel;

	public MakeNADialog(WinMain winMain)
	{
		super(winMain, "Alignment Creation (cDNA/Protein)", true);

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
		JLabel label1 = new JLabel("Select the two files that will be used to "
				+ "create a new DNA alignment.");

		dnaText = new JTextField(30);
		JLabel dnaLabel = new JLabel("Unaligned cDNAs input file:");
		dnaLabel.setDisplayedMnemonic(KeyEvent.VK_U);
		dnaLabel.setLabelFor(dnaText);

		proText = new JTextField(30);
		JLabel proLabel = new JLabel("Guide protein alignment file:");
		proLabel.setDisplayedMnemonic(KeyEvent.VK_G);
		proLabel.setLabelFor(proText);

		dnaBrowse = new JButton("Browse...");
		dnaBrowse.addActionListener(this);
		proBrowse = new JButton("Browse...");
		proBrowse.addActionListener(this);

		DoeLayout layout = new DoeLayout();
		layout.getPanel().setBorder(
				BorderFactory.createEmptyBorder(10, 10, 5, 10));
		layout.add(label1, 0, 0, 1, 3, new Insets(0, 0, 10, 0));

		layout.add(dnaLabel, 0, 1, 0, 1, new Insets(0, 0, 5, 5));
		layout.add(dnaText, 1, 1, 1, 1, new Insets(0, 0, 5, 5));
		layout.add(dnaBrowse, 2, 1, 0, 1, new Insets(0, 0, 5, 0));

		layout.add(proLabel, 0, 2, 0, 1, new Insets(0, 0, 0, 5));
		layout.add(proText, 1, 2, 1, 1, new Insets(0, 0, 0, 5));
		layout.add(proBrowse, 2, 2, 0, 1, new Insets(0, 0, 0, 0));

		return layout.getPanel();
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.get("ok"));
		bCancel = new JButton(Text.get("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "makena");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		else if (e.getSource() == bOK)
			onOK();
		else if (e.getSource() == dnaBrowse)
			doBrowse(dnaText);
		else if (e.getSource() == proBrowse)
			doBrowse(proText);
	}

	private void doBrowse(JTextField textField)
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Browse for File");
		fc.setCurrentDirectory(new File(Prefs.gui_dir));

		// Filters.setFilters(fc, -1, FAS, PHY_S, PHY_I, ALN, MSF, NEX, NEX_B);

		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getCurrentDirectory();

			textField.setText(file.getPath());
		}
	}

	private void onOK()
	{
		String dna = dnaText.getText();
		String pro = proText.getText();

		if (dna.length() == 0 || pro.length() == 0)
		{
			MsgBox.msg("Please ensure that details for both files have been "
					+ "entered.", MsgBox.ERR);
			return;
		}

		try
		{
			MakeNA makeNA = new MakeNA(new File(dna), new File(pro));
			if (makeNA.doConversion())
				setVisible(false);
			AlignmentData data = makeNA.getAlignmentData();
			if(data!=null)
				TOPALi.winMain.addNewAlignmentData(data);
		} catch (AlignmentLoadException e)
		{
			log.warn(e);
			MsgBox.msg(Text.get("ImportDataSetDialog.err0"
					+ e.getReason()), MsgBox.ERR);
		}
	}
}