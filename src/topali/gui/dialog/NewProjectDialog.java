// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import topali.gui.*;

import doe.*;

// Dialog class that presents a "New Project" dialog to the user, allowing them
// to select a name and a location for a new project. This project is then
// created and saved to disk ready for use.

public class NewProjectDialog extends JDialog implements ActionListener
{
	private Project project = null;
	
	private JTextField name, location;
	private JButton bOK, bCancel, bBrowse, bHelp;
	
	public NewProjectDialog(WinMain winMain)
	{
		super(winMain, Text.GuiDiag.getString("NewProjectDialog.gui01"), true);
	
		add(new GradientPanel(Text.GuiDiag.getString("NewProjectDialog.gui01")),
			BorderLayout.NORTH);
		add(createControls());
		add(createButtons(), BorderLayout.SOUTH);
		
		pack();
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bOK);
		
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}
	
	public Project getProject() { return project; }
	
	private JPanel createControls()
	{
		JLabel label1, label2, label3;
		
		label1 = new JLabel(Text.GuiDiag.getString("NewProjectDialog.gui02"));
		name = new JTextField("TOPALi Project " + Prefs.gui_project_count, 25);
		name.setToolTipText(Text.GuiDiag.getString("NewProjectDialog.gui04"));
		location = new JTextField(Prefs.gui_dir, 25);
		location.setToolTipText(Text.GuiDiag.getString("NewProjectDialog.gui05"));
		bBrowse = new JButton(Text.GuiDiag.getString("NewProjectDialog.gui06"));
		bBrowse.setMnemonic(KeyEvent.VK_B);
		bBrowse.addActionListener(this);
		bBrowse.setToolTipText(Text.GuiDiag.getString("NewProjectDialog.gui07"));
		label2 = new JLabel(Text.GuiDiag.getString("NewProjectDialog.gui08"));
		label2.setDisplayedMnemonic('N');
		label2.setLabelFor(name);
		label3 = new JLabel(Text.GuiDiag.getString("NewProjectDialog.gui09"));
		label3.setDisplayedMnemonic('L');
		label3.setLabelFor(location);
		
label2.addMouseListener(new MouseAdapter() {
public void mouseClicked(MouseEvent e) {
if (e.getClickCount() == 2)
name.setText("MyProject");
}});

		
		DoeLayout layout = new DoeLayout();
		layout.add(label1, 0, 0, 1, 3, new Insets(5, 5, 5, 5));
		layout.add(label2, 0, 1, 1, 1, new Insets(5, 5, 5, 0));
		layout.add(name, 1, 1, 1, 2, new Insets(5, 5, 5, 5));
		layout.add(label3, 0, 2, 1, 1, new Insets(0, 5, 5, 0));
		layout.add(location, 1, 2, 1, 1, new Insets(0, 5, 5, 0));
		layout.add(bBrowse, 2, 2, 1, 1, new Insets(0, 5, 5, 5));
		
		layout.getPanel().setBorder(
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
		return layout.getPanel();
	}
	
	private JPanel createButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));
		
		return Utils.getButtonPanel(this, bOK, bCancel, "new_project");
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bBrowse)
			browseForLocation();
		
		else if (e.getSource() == bOK)
			createNewProject();
	}
	
	private void browseForLocation()
	{
		JFileChooser fc = new JFileChooser();		
		fc.setDialogTitle(Text.GuiDiag.getString("NewProjectDialog.gui12"));
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		if (fc.showDialog(this, Text.GuiDiag.getString("NewProjectDialog.gui13"))
			== JFileChooser.APPROVE_OPTION)
		{
			File filename = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getSelectedFile();
			
			location.setText("" + filename);
		}
	}
	
	private void createNewProject()
	{
		// Check that values were entered in all required fields
		if (name.getText().length() == 0 ||
			location.getText().length() == 0)
		{
			MsgBox.msg(Text.GuiDiag.getString("NewProjectDialog.err01"),
				MsgBox.ERR);
			return;
		}
		
		// Check directory is ok
		String dir = location.getText();
		File dirFile = new File(dir);
		
		if (dirFile.exists() && !dirFile.isDirectory())
		{
			MsgBox.msg(Text.format(Text.GuiDiag.getString("NewProjectDialog.err02"),
				dirFile), MsgBox.ERR);
			return;
		}			
		else if (!dirFile.exists() && !dirFile.mkdirs())
		{
			MsgBox.msg(Text.format(Text.GuiDiag.getString("NewProjectDialog.err03"),
				dirFile), MsgBox.ERR);
			return;
		}
		
		// Check filename is ok
		File filename = new File(dir, name.getText() + ".topali");
		if (filename.exists())
		{
			// Confirm overwrite
			String msg = Text.format(
				Text.GuiDiag.getString("NewProjectDialog.msg01"), filename);
				
			if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
				return;
		}
	
// disabled when NewProjectDialog removed from TOPALi - bring back if needed
//		project = new Project(name.getText(), filename);
		project = new Project();
		Prefs.gui_project_count++;
		setVisible(false);
	}
}