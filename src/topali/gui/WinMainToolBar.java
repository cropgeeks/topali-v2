// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import topali.gui.nav.NavPanel;

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

	private JButton bAnlsCreateTree;

	private JButton bAnlsRunPDM;

	// private JButton bAnlsRunPDM2;
	private JButton bAnlsRunHMM;

	private JButton bAnlsRunDSS;

	private JButton bAnlsRunLRT;

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
		bAnlsCreateTree = (JButton) getButton(false, null, "gui12",
				Icons.CREATE_TREE, WinMainMenuBar.aAnlsCreateTree);
		bAnlsRunPDM = (JButton) getButton(false, null, "gui17", Icons.RUN_PDM,
				WinMainMenuBar.aAnlsRunPDM);
		// bAnlsRunPDM2 = (JButton) getButton(false, null, "gui19",
		// Icons.RUN_PDM2, mb.aAnlsRunPDM2);
		bAnlsRunHMM = (JButton) getButton(false, null, "gui16", Icons.RUN_HMM,
				WinMainMenuBar.aAnlsRunHMM);
		bAnlsRunDSS = (JButton) getButton(false, null, "gui15", Icons.RUN_DSS,
				WinMainMenuBar.aAnlsRunDSS);
		bAnlsRunLRT = (JButton) getButton(false, null, "gui18", Icons.RUN_LRT,
				WinMainMenuBar.aAnlsRunLRT);
		bFilePrint = (JButton) getButton(false, null, "gui13", Icons.PRINT16,
				WinMainMenuBar.aFilePrint);

		// bVamExport = (JButton) getButton(false, null, "gui20",
		// null, WinMainMenuBar.aVamExport);

		bVamsas = (JButton)getButton(false, null, "gui20", Icons.VAMSASOFF, WinMainMenuBar.aVamsas);
		
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
		add(bAnlsCreateTree);
		addSeparator();
		// add(bVamExport);
		// addSeparator();
		add(bAnlsRunPDM);
		// add(bAnlsRunPDM2);
		add(bAnlsRunHMM);
		add(bAnlsRunDSS);
		add(bAnlsRunLRT);
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

		if (Prefs.isWindows)
			button.setBorderPainted(false);

		button.setMargin(new Insets(1, 1, 1, 1));
		button.setIcon(icon);
		// button.setFocusPainted(false);

		return button;
	}
}
