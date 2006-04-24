// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import pal.alignment.*;

import topali.data.*;
import topali.gui.*;
import topali.mod.*;
import static topali.mod.Filters.*;

import doe.*;

public class ExportDialog extends JDialog implements ActionListener
{
	private WinMain winMain;
	private AlignmentData data;
	private SequenceSet ss;
	
	private JButton bOK, bCancel;
	private JRadioButton rAllSeq, rSelSeq;
	private JRadioButton rAllPar, rSelPar;
	private JRadioButton rDisk, rProject;
	
	public ExportDialog(WinMain winMain, AlignmentData data)
	{
		super(winMain, "", true);
	
		this.winMain = winMain;
		this.data = data;
		ss = data.getSequenceSet();
		
		add(getControls(), BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bCancel);
		
		pack();
		
		setTitle(Text.format(Text.GuiDiag.getString("ExportDialog.gui01"),
			data.name));
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}
	
	private JPanel getControls()
	{
		int allCount = ss.getSize();
		int selCount = ss.getSelectedSequences().length;
				
		String tAllSeq = "Export all sequences in the alignment (" + allCount + ")";
		String tSelSeq = "Only export the currently selected sequences (" + selCount + "/" + allCount + ")";
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
		p1.setBorder(BorderFactory.createTitledBorder("Sequence selection:"));
		p1.add(rAllSeq);
		p1.add(rSelSeq);
		
		PartitionAnnotations pAnnotations =
			data.getTopaliAnnotations().getPartitionAnnotations();
		int s = pAnnotations.getCurrentStart();
		int e = pAnnotations.getCurrentEnd();
		
		String tAllPar = "Export the full alignment (1-" + ss.getLength() + ")";
		String tSelPar = "Only export the currently selected partition ("
			+ s + "-" + e + ")";
		rAllPar = new JRadioButton(tAllPar, Prefs.gui_export_allpars);
		rAllPar.setMnemonic(KeyEvent.VK_F);
		rSelPar = new JRadioButton(tSelPar, !Prefs.gui_export_allpars);
		rSelPar.setMnemonic(KeyEvent.VK_P);
		ButtonGroup group2 = new ButtonGroup();
		group2.add(rAllPar);
		group2.add(rSelPar);
		
		JPanel p2 = new JPanel(new GridLayout(2, 1, 0, 0));
		p2.setBorder(BorderFactory.createTitledBorder("Alignment length:"));
		p2.add(rAllPar);
		p2.add(rSelPar);
		
		rDisk = new JRadioButton("Export to a file on disk", Prefs.gui_export_todisk);
		rDisk.setMnemonic(KeyEvent.VK_D);
		rProject = new JRadioButton("Add as a new alignment within the current project", !Prefs.gui_export_todisk);
		rProject.setMnemonic(KeyEvent.VK_N);
		ButtonGroup group3 = new ButtonGroup();
		group3.add(rDisk);
		group3.add(rProject);
		
		JPanel p3 = new JPanel(new GridLayout(2, 1, 0, 0));
		p3.setBorder(BorderFactory.createTitledBorder("Export options:"));
		p3.add(rDisk);
		p3.add(rProject);
		
		DoeLayout layout = new DoeLayout();		
		layout.add(p1, 0, 0, 1, 1, new Insets(10, 10, 0, 10));
		layout.add(p2, 0, 1, 1, 1, new Insets(0, 10, 0, 10));
		layout.add(p3, 0, 2, 1, 1, new Insets(0, 10, 0, 10));

		return layout.getPanel();
	}
	
	private JPanel getButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		
		return Utils.getButtonPanel(this, bOK, bCancel, "export_alignment");
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
			
		else if (e.getSource() == bOK)
		{
			Prefs.gui_export_allseqs = rAllSeq.isSelected();
			Prefs.gui_export_allpars = rAllPar.isSelected();
			Prefs.gui_export_todisk  = rDisk.isSelected();
			
			if (export())
				setVisible(false);
		}
	}

	private boolean export()
	{
		// Alignment size
		int[] seqs = ss.getAllSequences();
		if (Prefs.gui_export_allseqs == false)
			seqs = ss.getSelectedSequences();
		
		// Alignment length
		int nStart = 1;
		int nEnd = ss.getLength();
		if (Prefs.gui_export_allpars == false)
		{
			PartitionAnnotations pa = data.getTopaliAnnotations().getPartitionAnnotations();
			
			nStart = pa.getCurrentStart();
			nEnd = pa.getCurrentEnd();
		}
		
		// Work out a new name for the alignment
		String name = data.name + " ("+seqs.length+"x"+(nEnd-nStart+1)+ ")";
		
		// Save to disk...
		if (Prefs.gui_export_todisk)
		{
			File filename = showSaveDialog(name);
			if (filename == null)
				return true;

			try
			{
				ss.save(filename, seqs, nStart, nEnd, Prefs.gui_filter_algn, false);
				MsgBox.msg(filename + " was successfully saved to disk.", MsgBox.INF);
				return true;
			}
			catch (Exception e)
			{
				MsgBox.msg(filename + " could not be saved due to the following error:\n "
					+ e, MsgBox.ERR);
				return false;
			}
		}
		// Or add as a new project alignment
		else
		{
			Alignment alignment = ss.getAlignment(seqs, nStart, nEnd, false);
			new ImportDataSetDialog(winMain).cloneAlignment(data.name, alignment);
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
		
		Filters.setFilters(fc, Prefs.gui_filter_algn, FAS, PHY_S, PHY_I, ALN, MSF, NEX, NEX_B);

		while (fc.showSaveDialog(MsgBox.frm) == JFileChooser.APPROVE_OPTION)
		{
			File file = Filters.getSelectedFileForSaving(fc);
			
			// Confirm overwrite
			if (file.exists())
			{
				String msg = file + " already exists.\nDo you want to replace it?";
				int response = MsgBox.yesnocan(msg, 1);
					
				if (response == JOptionPane.NO_OPTION)
					continue;
				else if (response == JOptionPane.CANCEL_OPTION ||
					response == JOptionPane.CLOSED_OPTION)
					return null;
			}
			
			// Otherwise it's ok to save...
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			Prefs.gui_filter_algn = ((Filters)fc.getFileFilter()).getExtInt();
			
			return file;
		}
		
		return null;
	}
}