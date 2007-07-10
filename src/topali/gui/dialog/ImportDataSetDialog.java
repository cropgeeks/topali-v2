// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

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
	private WinMain winMain;
	
	private JButton bOK, bCancel, bBrowse;
	
	public ImportDataSetDialog(WinMain winMain)
	{
		super(winMain, Text.GuiFile.getString("ImportDataSetDialog.gui01"), true);
		this.winMain = winMain;
	
/*		add(new GradientPanel(Text.GuiFile.getString("ImportDataSetDialog.gui01")),
			BorderLayout.NORTH);
		add(createControls());
		add(createButtons(), BorderLayout.SOUTH);
		
		pack();
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bOK);
		
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
*/
	}
	
	public void promptForAlignment()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(Text.GuiFile.getString("ImportDataSetDialog.gui01"));
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		
		Filters.setFilters(fc, -1, FAS, PHY_S, PHY_I, ALN, MSF, NEX, NEX_B);
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			
			loadAlignment(file);
		}
	}
	
	public void loadAlignment(File file)
	{
		String name = file.getName();
		if (name.indexOf(".") != -1)
			name = name.substring(0, name.lastIndexOf("."));
					
		load(name, file, null);
	}
	
	public void cloneAlignment(String name, Alignment alignment)
	{
		load(name, null, alignment);
	}
	
	private void load(String name, File filename, Alignment alignment)
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
		}
		catch (AlignmentLoadException e)
		{
			int code = e.getReason();			
			MsgBox.msg(Text.GuiFile.getString("ImportDataSetDialog.err0"
				+ code), MsgBox.ERR);
				
			return;
		}
		
		// Finally, add the data to the project (via WinMain)
		winMain.addNewAlignmentData(data);
	}
	
/*	private JPanel createControls()
	{
		return new JPanel();
	}
	
	private JPanel createButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		
		return Utils.getButtonPanel(this, bOK, bCancel, "import_dataset");
	}
*/

	public void actionPerformed(ActionEvent e)
	{
	}
}