package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import topali.data.*;
import topali.gui.*;

import doe.*;

public class ImportFileSetsDialog extends JDialog implements ActionListener
{
	private WinMain winMain;
	
	private JTextArea text;
	private JProgressBar pBar;
	private JButton bClose;
	
	private AlignmentData data = new AlignmentData();
	private File importDir;
	
	public ImportFileSetsDialog(WinMain winMain)
	{
		super(winMain, "Importing Alignments", true);
		this.winMain = winMain;
		
		// Prompt for the directory to import from
		getDirectory();
		// And quit if one isn't selected
		if (importDir == null)
			return;
		
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e)
			{
				Runnable r = new Runnable() {
					public void run()
					{
						doProcessing();
					}
				};
				
				new Thread(r).start();
			}
		});
		
		add(createControls());
		Utils.addCloseHandler(this, bClose);
		
		pack();
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(winMain);
		setVisible(true);		
	}
	
	private JPanel createControls()
	{
		text = new JTextArea(10, 75);
		Utils.setTextAreaDefaults(text);
		JScrollPane sp = new JScrollPane(text);
		
		bClose = new JButton("Close");
		bClose.setEnabled(false);
		bClose.addActionListener(this);
		JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p1.add(bClose);
		
		pBar = new JProgressBar();
		
		DoeLayout layout = new DoeLayout();
		layout.add(sp, 0, 0, 1, 1, new Insets(10, 10, 5, 10));
		layout.add(pBar, 0, 1, 1, 1, new Insets(0, 10, 10, 10));
		layout.add(p1, 0, 2, 1, 1, new Insets(0, 10, 10, 10));
		
		return layout.getPanel();
	}
	
	private void getDirectory()
	{
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new File(Prefs.gui_dir));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setDialogTitle("Select Import Directory");
		
		if (fc.showOpenDialog(winMain) == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			Prefs.gui_dir = "" + fc.getCurrentDirectory();
			
			importDir = file;
		}
	}
	
	private void doProcessing()
	{		
		populateFileDataSet(importDir);
	}
	
	private void populateFileDataSet(File dir)
	{
		File[] files = dir.listFiles();		
		pBar.setMaximum(files.length);
		
		for (File file: files)
		{
			if (file.isDirectory() == false)
			{
				SequenceSet ss = null;
				
				try
				{ 
					ss = new SequenceSet(file);
					data.addReference(file.getPath(), ss);
					text.append("Loaded: " + file.getName() + "\n");
				}
				catch (Exception e)
				{
					// TODO: catch proper error and report why it failed
					text.append("FAILED: " + file.getName() + "\n");
				}
				
				text.setCaretPosition(text.getText().length()-1);
			}
			
			pBar.setValue(pBar.getValue()+1);
		}
		
		bClose.setEnabled(true);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClose)
		{
			data.name = "DataSet";
			data.setIsReferenceList(true);			
			winMain.addNewAlignmentData(data);
			
			setVisible(false);
		}
	}
}