// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.Insets;
import java.awt.event.*;

import javax.swing.*;

import topali.gui.nav.NavPanel;
import topali.var.SysPrefs;

public class WinMainToolBar extends JToolBar
{
	private JButton bFileNewProject;

	private JButton bFileOpenProject;

	private JButton bFileSave;

	private JButton bFilePrint;

	private JButton bFileImportDataSet;

	private JButton bAlgnFindSeq;

	private JButton bAlgnDisplaySummary;

	private JButton bAlgnMoveUp;

	private JButton bAlgnMoveDown;

	public JButton bVamsas;
	
	private JButton bHelpDisplay;

	// private JButton bVamExport;

	WinMainToolBar(final NavPanel navPanel)
	{
		setFloatable(false);
		setBorderPainted(false);
		setVisible(Prefs.gui_toolbar_visible);

		bFileNewProject = (JButton) getButton(false, "gui01", "gui02",
				Icons.NEW_PROJECT16, WinMainMenuBar.aFileNewProject);
		bFileOpenProject = (JButton) getButton(false, "gui03", "gui04",
				Icons.OPEN_PROJECT16, WinMainMenuBar.aFileOpenProject);
		bFileSave = (JButton) getButton(false, null, "gui05", Icons.SAVE16,
				WinMainMenuBar.aFileSave);
		bFileImportDataSet = (JButton) getButton(false, "gui06", "gui07",
				Icons.IMPORT16, WinMainMenuBar.aFileImportDataSet);
		bAlgnFindSeq = (JButton) getButton(false, null, "gui08", Icons.FIND16,
				WinMainMenuBar.aAlgnFindSeq);
		bAlgnDisplaySummary = (JButton) getButton(false, null, "gui09",
				Icons.INFO16, WinMainMenuBar.aAlgnDisplaySummary);
		bAlgnMoveUp = (JButton) getButton(false, null, "gui10", Icons.UP16,
				WinMainMenuBar.aAlgnMoveUp);
		bAlgnMoveDown = (JButton) getButton(false, null, "gui11", Icons.DOWN16,
				WinMainMenuBar.aAlgnMoveDown);
		bFilePrint = (JButton) getButton(false, null, "gui13", Icons.PRINT16,
				WinMainMenuBar.aFilePrint);

		bVamsas = (JButton)getButton(false, null, "gui20", Icons.VAMSASOFF, WinMainMenuBar.aVamsasButton);
		
		bHelpDisplay = (JButton) getButton(false, null, "gui14", Icons.HELP16,
				null);
		bHelpDisplay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				navPanel.displayHelp();
			}
		});
		// TOPALiHelp.enableHelpOnButton(bHelpDisplay, "intro");

		add(new JLabel(" "));

		add(bFileNewProject);
		add(bFileOpenProject);
		addSeparator();
		add(bFileSave);
		add(bFilePrint);
		addSeparator();
		add(bFileImportDataSet);
		addSeparator();
		add(bAlgnFindSeq);
		add(bAlgnDisplaySummary);
		add(bAlgnMoveUp);
		add(bAlgnMoveDown);
		addSeparator();
		add(bVamsas);
		addSeparator();
		add(bHelpDisplay);

		add(new JLabel(" "));
	}

	// Utility method to help create the buttons. Sets their text, tooltip, and
	// icon, as well as adding actionListener, defining margings, etc.
	public static AbstractButton getButton(boolean toggle, String title,
			String tt, ImageIcon icon, Action a)
	{
		AbstractButton button = null;

		if (toggle)
			button = new JToggleButton(a);
		else
			button = new JButton(a);

		if (title != null)
			button.setText(Text.Gui.getString("WinMainToolBar." + title));
		else
			button.setText("");

		if (tt != null)
			button.setToolTipText(Text.Gui.getString("WinMainToolBar." + tt));

		if (SysPrefs.isWindows)
			button.setBorderPainted(false);

		button.setMargin(new Insets(1, 1, 1, 1));
		button.setIcon(icon);
		// button.setFocusPainted(false);

		return button;
	}
	
	public void vamsasEnabled(boolean b) {
		if(b)
			bVamsas.setIcon(Icons.VAMSASON);
		else
			bVamsas.setIcon(Icons.VAMSASOFF);
	}
}
