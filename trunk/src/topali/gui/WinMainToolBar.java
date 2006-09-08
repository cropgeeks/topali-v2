// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import topali.gui.nav.*;

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
	private JButton bAnlsRunPDM2;
	private JButton bAnlsRunHMM;
	private JButton bAnlsRunDSS;
	private JButton bAnlsRunLRT;
	private JButton bHelpDisplay;
	
	private JButton bVamExport;
	
	
	WinMainToolBar(WinMainMenuBar mb, final NavPanel navPanel)
	{
		setFloatable(false);
		setBorderPainted(false);
		setVisible(Prefs.gui_toolbar_visible);
		
		bFileNewProject = (JButton) getButton(false, "gui01", "gui02",
			Icons.NEW_PROJECT16, mb.aFileNewProject);
		bFileOpenProject = (JButton) getButton(false, "gui03", "gui04",
			Icons.OPEN_PROJECT16, mb.aFileOpenProject);
		bFileSave = (JButton) getButton(false, null, "gui05",
			Icons.SAVE16, mb.aFileSave);
		bFileImportDataSet = (JButton) getButton(false, "gui06", "gui07",
			Icons.IMPORT16, mb.aFileImportDataSet);
		bAlgnFindSeq = (JButton) getButton(false, null, "gui08",
			Icons.FIND16, mb.aAlgnFindSeq);
		bAlgnDisplaySummary = (JButton) getButton(false, null, "gui09",
			Icons.INFO16, mb.aAlgnDisplaySummary);
		bAlgnMoveUp = (JButton) getButton(false, null, "gui10",
			Icons.UP16, mb.aAlgnMoveUp);
		bAlgnMoveDown = (JButton) getButton(false, null, "gui11",
			Icons.DOWN16, mb.aAlgnMoveDown);
		bAnlsCreateTree = (JButton) getButton(false, null, "gui12",
			Icons.CREATE_TREE, mb.aAnlsCreateTree);
		bAnlsRunPDM = (JButton) getButton(false, null, "gui17",
			Icons.RUN_PDM, mb.aAnlsRunPDM);
		bAnlsRunPDM2 = (JButton) getButton(false, null, "gui19",
			Icons.RUN_PDM2, mb.aAnlsRunPDM2);
		bAnlsRunHMM = (JButton) getButton(false, null, "gui16",
			Icons.RUN_HMM, mb.aAnlsRunHMM);
		bAnlsRunDSS = (JButton) getButton(false, null, "gui15",
			Icons.RUN_DSS, mb.aAnlsRunDSS);
		bAnlsRunLRT = (JButton) getButton(false, null, "gui18",
			Icons.RUN_LRT, mb.aAnlsRunLRT);
		bFilePrint = (JButton) getButton(false, null, "gui13",
			Icons.PRINT16, mb.aFilePrint);
		
		bVamExport = (JButton) getButton(false, null, "gui20",
			null, mb.aVamExport);
		
		bHelpDisplay = (JButton) getButton(false, null, "gui14", Icons.HELP16, null);
		bHelpDisplay.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				navPanel.displayHelp();
			}
		});
//		TOPALiHelp.enableHelpOnButton(bHelpDisplay, "intro");
		
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
		add(bVamExport);
		addSeparator();
		add(bAnlsRunPDM);
//		add(bAnlsRunPDM2);
		add(bAnlsRunHMM);
		add(bAnlsRunDSS);
		add(bAnlsRunLRT);
		addSeparator();
		add(bHelpDisplay);
		
		add(new JLabel(" "));
	}
	
	// Utility method to help create the buttons. Sets their text, tooltip, and
	// icon, as well as adding actionListener, defining margings, etc.
	public static AbstractButton getButton(
		boolean toggle, String title, String tt, ImageIcon icon, Action a)
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
//		button.setFocusPainted(false);
		
		return button;
	}
}
