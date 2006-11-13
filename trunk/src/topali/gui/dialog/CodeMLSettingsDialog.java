// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import doe.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;

public class CodeMLSettingsDialog extends JDialog implements ActionListener
{
	private JButton bRun, bCancel, bHelp;
	
	private AlignmentData data;
	private CodeMLResult result = null;

	public CodeMLSettingsDialog(WinMain winMain, AlignmentData data, CodeMLResult iResult)
	{
		super(winMain, "Positive Selection", true);
		this.data = data;
				
		bRun = new JButton("Run");
		bRun.addActionListener(this);
		bCancel = new JButton(Text.Gui.getString("cancel"));
		bCancel.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("cml_settings");
		
		
		JPanel p1 = new JPanel(new GridLayout(1, 3, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bRun);
		p1.add(bCancel);
		p1.add(bHelp);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		
		add(new JLabel("TODO: codeml settings"), BorderLayout.CENTER);
		add(p2, BorderLayout.SOUTH);

		
		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
		
		pack();
		
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}
		
	public CodeMLResult getCodeMLResult()
		{ return result; }
	
	private void createCodeMLObject(boolean makeRemote)
	{
		SequenceSet ss = data.getSequenceSet();
		
		result = new CodeMLResult();
		
		if (Prefs.isWindows)
			result.codemlPath = Utils.getLocalPath() + "codeml.exe";
		else
			result.codemlPath = Utils.getLocalPath() + "codeml/codeml";
		
		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = makeRemote;
	}
	
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bRun)
		{		
			createCodeMLObject((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
			setVisible(false);
		}
	}
}
