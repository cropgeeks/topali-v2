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

import topali.analyses.*;
import topali.data.*;
import topali.fileio.*;
import topali.gui.*;
import topali.mod.*;
import static topali.mod.Filters.*;

import doe.*;

public class ImportDataSetDialog extends JDialog implements ActionListener
{
	private AlignmentData data;
	
	private JButton bOK, bCancel, bBrowse;
	
	public ImportDataSetDialog(WinMain winMain)
	{
		super(winMain, Text.GuiFile.getString("ImportDataSetDialog.gui01"), true);
	
		add(new GradientPanel(Text.GuiFile.getString("ImportDataSetDialog.gui01")),
			BorderLayout.NORTH);
		add(createControls());
		add(createButtons(), BorderLayout.SOUTH);
		
		pack();
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bOK);
		
		setLocationRelativeTo(winMain);
		setResizable(false);
//		setVisible(true);
	}
	
	public AlignmentData getAlignmentData()
		{ return data; }
	
	public boolean loadAlignment(File file)
	{
		if (file == null)
		{
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle(Text.GuiFile.getString("ImportDataSetDialog.gui01"));
			fc.setCurrentDirectory(new File(Prefs.gui_dir));
			
			Filters.setFilters(fc, -1, FAS, PHY_S, PHY_I, ALN, MSF, NEX, NEX_B);
			
			if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				file = fc.getSelectedFile();
				Prefs.gui_dir = "" + fc.getCurrentDirectory();
			}
			else
				return false;
		}
		
		String name = file.getName();
		if (name.indexOf(".") != -1)
			name = name.substring(0, name.lastIndexOf("."));
						
		return load(name, file, null);
	}	
	
	public boolean cloneAlignment(String name, Alignment alignment)
	{
		return load(name, null, alignment);
	}
	
	private boolean load(String name, File filename, Alignment alignment)
	{
		try
		{
			SequenceSet ss = null;
			if (filename != null)
				ss = new SequenceSet(filename);
			else
				ss = new SequenceSet(alignment);
				
			if (SequenceSetUtils.verifySequenceNames(ss) == false)
				MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err05"), MsgBox.WAR);
			
			data = new AlignmentData(name, ss);			
			return true;
		}
		catch (AlignmentLoadException e)
		{
			int code = e.getReason();			
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err0"
				+ code), MsgBox.ERR);
				
			return false;
		}
	}
	
	private JPanel createControls()
	{
		return new JPanel();
	}
	
	private JPanel createButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		
		return Utils.getButtonPanel(this, bOK, bCancel, "import_dataset");
	}
	
	public void actionPerformed(ActionEvent e)
	{
	}
}