// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.settings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import topali.gui.*;
import topali.var.Utils;
import doe.MsgBox;

public class SettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;

	private WebPanel webPanel;

	private CachePanel cachePanel;

	private JButton bOK, bCancel, bDefault, bHelp;

	public SettingsDialog(WinMain winMain)
	{
		super(winMain, "TOPALi Settings", true);

		tabs = new JTabbedPane();
		webPanel = new WebPanel();
		webPanel.setBorder(BorderFactory.createEmptyBorder(-8, -8, -10, -8));
		tabs.addTab("Web/Cluster", webPanel);
		tabs.addTab("Local Job Settings", cachePanel = new CachePanel());

		add(tabs, BorderLayout.CENTER);
		add(getButtons(), BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bCancel);

		pack();

		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private void setDefaults()
	{
		String msg = "This will return all settings (on all tabs) to their default values. Continue?";
		if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
			return;

		webPanel.setDefaults(true);
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bOK.addActionListener(this);
		bCancel = new JButton(Text.Gui.getString("cancel"));
		bCancel.addActionListener(this);
		bDefault = new JButton(Text.Gui.getString("defaults"));
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("settings");

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
			cachePanel.isOK();

			if (webPanel.isOK())
			{
				TOPALi.setProxy();
				setVisible(false);
			}
		}
	}
}
