// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import static topali.mod.Filters.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.alignment.Alignment;
import topali.analyses.SequenceSetUtils;
import topali.data.*;
import topali.gui.*;
import topali.i18n.Text;
import topali.mod.Filters;
import topali.var.utils.Utils;
import scri.commons.gui.*;

public class ExportDialog extends JDialog implements ActionListener
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private WinMain winMain;

	private AlignmentData data;

	private RegionAnnotations annotations;

	private SequenceSet ss;

	// Selected partition indexes (at the time this dialog was opened)
	private int[] regions = null;

	private JButton bOK, bCancel;

	private JRadioButton rAllSeq, rSelSeq;

	private JRadioButton rAllPar, rSelPar;

	private JRadioButton rDisk, rProject, rCodonPos;

	public ExportDialog(WinMain winMain, AlignmentData data,
			RegionAnnotations annotations, int[] regions)
	{
		super(winMain, "", true);

		this.winMain = winMain;
		this.data = data;
		this.regions = regions;
		this.annotations = annotations;
		ss = data.getSequenceSet();

		add(getControls(), BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bCancel);

		pack();

		setTitle(Text.get("ExportDialog.gui01",data.name));
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private JPanel getControls()
	{
		int allCount = ss.getSize();
		int selCount = ss.getSelectedSequences().length;

		String tAllSeq = Text.get("ExportDialog.1", allCount);
		String tSelSeq = Text.get("ExportDialog.2", selCount, allCount);
		rAllSeq = new JRadioButton(tAllSeq, Prefs.gui_export_allseqs);
		rAllSeq.setMnemonic(KeyEvent.VK_A);
		rSelSeq = new JRadioButton(tSelSeq, !Prefs.gui_export_allseqs);
		rSelSeq.setMnemonic(KeyEvent.VK_C);
		ButtonGroup group1 = new ButtonGroup();
		group1.add(rAllSeq);
		group1.add(rSelSeq);

		if (selCount == 0)
		{
			rAllSeq.setSelected(true);
			rSelSeq.setEnabled(false);
		}

		JPanel p1 = new JPanel(new GridLayout(2, 1, 0, 0));
		p1.setBorder(BorderFactory.createTitledBorder(Text.get("Sequence_selection")));
		p1.add(rAllSeq);
		p1.add(rSelSeq);

		String tAllPar = Text.get("ExportDialog.3", ss.getLength());
		String tSelPar = Text.get("ExportDialog.4", countConcatenatedLength());
		rAllPar = new JRadioButton(tAllPar, true);
		rAllPar.setMnemonic(KeyEvent.VK_F);
		rSelPar = new JRadioButton(tSelPar);
		rSelPar.setMnemonic(KeyEvent.VK_S);

		if (regions.length == 0)
			rSelPar.setEnabled(false);

		ButtonGroup group2 = new ButtonGroup();
		group2.add(rAllPar);
		group2.add(rSelPar);

		if (Prefs.gui_export_pars == 2 && regions.length > 0)
			rSelPar.setSelected(true);

		JPanel p2 = new JPanel(new GridLayout(2, 1, 0, 0));
		p2.setBorder(BorderFactory.createTitledBorder(Text.get("Alignment_length")));
		p2.add(rAllPar);
		p2.add(rSelPar);

		rDisk = new JRadioButton(Text.get("ExportDialog.5"),
				Prefs.gui_export_todisk);
		rDisk.setMnemonic(KeyEvent.VK_D);
		rProject = new JRadioButton(
			Text.get("ExportDialog.6"),
				!Prefs.gui_export_todisk);
		rProject.setMnemonic(KeyEvent.VK_N);
		rCodonPos = new JRadioButton(Text.get("ExportDialog.7"));
		rCodonPos.setMnemonic(KeyEvent.VK_C);
		rCodonPos.setEnabled(ss.isCodons());
		ButtonGroup group3 = new ButtonGroup();
		group3.add(rDisk);
		group3.add(rProject);
		group3.add(rCodonPos);
		
		JPanel p3 = new JPanel(new GridLayout(3, 1, 0, 0));
		p3.setBorder(BorderFactory.createTitledBorder(Text.get("ExportDialog.8")));
		p3.add(rDisk);
		p3.add(rProject);
		p3.add(rCodonPos);

		DoeLayout layout = new DoeLayout();
		layout.add(p1, 0, 0, 1, 1, new Insets(10, 10, 0, 10));
		layout.add(p2, 0, 1, 1, 1, new Insets(0, 10, 0, 10));
		layout.add(p3, 0, 2, 1, 1, new Insets(0, 10, 0, 10));

		return layout.getPanel();
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.get("ok"));
		bCancel = new JButton(Text.get("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "export_alignment");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bOK)
		{
			Prefs.gui_export_allseqs = rAllSeq.isSelected();

			if (rAllPar.isSelected())
				Prefs.gui_export_pars = 1;
			if (rSelPar.isSelected())
				Prefs.gui_export_pars = 2;

			Prefs.gui_export_todisk = rDisk.isSelected();

			if (export())
				setVisible(false);
		}
	}

	// (Tries) to calculate what total length a new alignment would have if it
	// was formed by concatenating all the selected partitions together
	private int countConcatenatedLength()
	{
		int concatLength = 0;

		for (int r : regions)
		{
			RegionAnnotations.Region region = annotations.get(r);
			concatLength += (region.getE() - region.getS()) + 1;
		}
		return concatLength;
	}

	private boolean export()
	{
		// Alignment size
		int[] seqs = ss.getAllSequences();
		if (Prefs.gui_export_allseqs == false)
			seqs = ss.getSelectedSequences();

		if(rCodonPos.isSelected()) {
			SequenceSet ss1 = SequenceSetUtils.getCodonPosSequenceSet(data, 1, seqs);
			SequenceSet ss2 = SequenceSetUtils.getCodonPosSequenceSet(data, 2, seqs);
			SequenceSet ss3 = SequenceSetUtils.getCodonPosSequenceSet(data, 3, seqs);
			
			Alignment alignment = ss1.getAlignment(false);
			new ImportDataSetDialog(winMain).cloneAlignment(data.name+"_p1",
					alignment);
			
			Alignment alignment2 = ss2.getAlignment(false);
			new ImportDataSetDialog(winMain).cloneAlignment(data.name+"_p2",
					alignment2);
			
			Alignment alignment3 = ss3.getAlignment(false);
			new ImportDataSetDialog(winMain).cloneAlignment(data.name+"_p3",
					alignment3);
			
			return true;
		}

		// We recreate the alignment so it only contains the sequences and
		// regions that
		// correspond to the user's selection
		SequenceSet toExport = SequenceSetUtils.getConcatenatedSequenceSet(
				data, annotations, seqs, regions);

		// Work out a new name for the alignment
		String name = data.name + " (" + toExport.getSize() + "x"
				+ (toExport.getLength()) + ")";
		
		// Save to disk...
		if (Prefs.gui_export_todisk)
		{
			File filename = showSaveDialog(name);
			if (filename == null)
				return true;

			try
			{
				toExport.save(filename, Prefs.gui_filter_algn, false);
				MsgBox.msg(Text.get("ExportDialog.9", filename),
						MsgBox.INF);
				return true;
			} catch (Exception e)
			{
				log.warn("Export failed.\n",e);
				MsgBox.msg(Text.get("ExportDialog.10", filename)+"\n"
						+ e, MsgBox.ERR);
				return false;
			}
		}
		// Or add as a new project alignment
		else
		{
			Alignment alignment = toExport.getAlignment(false);
			new ImportDataSetDialog(winMain).cloneAlignment(data.name,
					alignment);
		}

		return true;
	}

	private File showSaveDialog(String name)
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Save As");
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setSelectedFile(new File(name));
		fc.setAcceptAllFileFilterUsed(false);

		Filters.setFilters(fc, Prefs.gui_filter_algn, FAS, PHY_S, PHY_I, ALN,
				MSF, NEX, NEX_B);

		while (fc.showSaveDialog(TOPALi.winMain) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);

			// Confirm overwrite
			if (file.exists())
			{
				String msg = Text.get("ExportDialog.11", file);
				int response = MsgBox.yesnocan(msg, 1);

				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION
						|| response == JOptionPane.CLOSED_OPTION)
					return null;
			}

			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_filter_algn = ((Filters) fc.getFileFilter()).getExtInt();

			return file;
		}

		return null;
	}
}