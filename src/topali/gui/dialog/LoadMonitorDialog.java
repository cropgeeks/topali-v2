// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.gui.*;

public class LoadMonitorDialog extends JDialog implements Runnable
{
	private WinMainMenuBar menubar;

	private String filename;

	private Project project;

	private static JLabel label1;

	private static JLabel label2;

	public LoadMonitorDialog(WinMain winMain, WinMainMenuBar menubar,
			String filename)
	{
		super(winMain, Text.GuiDiag.getString("LoadMonitorDialog.gui01"), true);

		this.menubar = menubar;
		this.filename = filename;

		addWindowListener(new WindowAdapter()
		{
			public void windowOpened(WindowEvent e)
			{
				doLoad();
			}
		});

		JLabel icon = new JLabel(Icons.OPEN_PROJECT);
		icon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
		label1 = new JLabel(Text.GuiDiag.getString("LoadMonitorDialog.gui02"));
		label2 = new JLabel(" ");
		JPanel p1 = new JPanel(new GridLayout(2, 1, 0, 2));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
		p1.add(label1);
		p1.add(label2);
		add(p1);
		add(icon, BorderLayout.WEST);

		pack();
		setResizable(false);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(winMain);
		setVisible(true);
	}

	private void doLoad()
	{
		new Thread(this).start();
	}

	public Project getProject()
	{
		return project;
	}

	public void run()
	{
		setLabel(Text.GuiDiag.getString("LoadMonitorDialog.gui03"));
		Project temp = Project.open(filename);
		setLabel(Text.GuiDiag.getString("LoadMonitorDialog.gui04"));

		if (temp != null)
		{
			project = temp;

			menubar.setProjectOpenedState();
			menubar.updateRecentFileList(project);
			WinMain.navPanel.displayProject(project);
			WinMainMenuBar.aFileSave.setEnabled(false);
		}

		setVisible(false);
	}

	public static void setLabel(final String msg)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				label2.setText("(" + msg + ")");
			}
		};

		try
		{
			SwingUtilities.invokeAndWait(r);
		} catch (Exception e)
		{
		}
	}
}