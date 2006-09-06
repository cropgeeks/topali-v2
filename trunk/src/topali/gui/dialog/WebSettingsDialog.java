// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import topali.gui.*;

import doe.*;

public class WebSettingsDialog extends JDialog implements ActionListener
{
	private JButton bOK, bCancel, bDefault, bHelp;
	
	private JTextField topaliURL;
	private JSpinner minSpin;
	private SpinnerNumberModel minModel;
	
	public WebSettingsDialog(WinMain winMain)
	{
		super(winMain, "Web/Cluster Settings", true);
				
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
		topaliURL = new JTextField(Prefs.web_broker_url, 40);
		JLabel label1 = new JLabel("Remote resource broker: ");
		label1.setLabelFor(topaliURL);
		label1.setDisplayedMnemonic(KeyEvent.VK_T);
		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createTitledBorder("Web services:"));
		p1.add(label1);
		p1.add(topaliURL);
		
		minModel = new SpinnerNumberModel(Prefs.web_check_secs, 10, 300, 10);
		minSpin = new JSpinner(minModel);
		JLabel label2 = new JLabel("Check remote job progress every: ");
		label2.setLabelFor(minSpin);
		label2.setDisplayedMnemonic(KeyEvent.VK_R);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p2.setBorder(BorderFactory.createTitledBorder("Other settings:"));
		p2.add(label2);
		p2.add(minSpin);
		p2.add(new JLabel("seconds"));
				
		DoeLayout layout = new DoeLayout();
		layout.add(p1, 0, 0, 1, 1, new Insets(10, 10, 0, 10));
		layout.add(p2, 0, 1, 1, 1, new Insets(5, 10, 0, 10));
		
		return layout.getPanel();
	}
	
	private void setDefaults()
	{
		String msg = "This will return all settings to their default values. Continue?";
		if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
			return;
		
		Prefs.setWebDefaults();
		
		topaliURL.setText(Prefs.web_broker_url);
		minSpin.setValue((Integer)Prefs.web_check_secs);
	}
	
	private JPanel getButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bOK.addActionListener(this);
		bCancel = new JButton(Text.Gui.getString("cancel"));
		bCancel.addActionListener(this);
		bDefault = new JButton(Text.Gui.getString("defaults"));
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("web_settings");
				
		JPanel p1 = new JPanel(new GridLayout(1, 4, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bOK);
		p1.add(bDefault);
		p1.add(bCancel);
		p1.add(bHelp);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		
		return p2;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bDefault)
			setDefaults();
			
		else if (e.getSource() == bOK)
		{
			if (topaliURL.getText().length() == 0)
			{
				MsgBox.msg("Please ensure a URL is entered for the resource broker.", MsgBox.ERR);
				return;
			}
			
			Prefs.web_broker_url = topaliURL.getText();
			Prefs.web_check_secs = minModel.getNumber().intValue();
			
			setVisible(false);
		}
	}
}
