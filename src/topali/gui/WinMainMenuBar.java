// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.logging.GracefulShutdownHandler;

public class WinMainMenuBar extends JMenuBar
{
	 Logger log = Logger.getLogger(this.getClass());
	
	private WinMain winMain;

	JMenu mFile, mFileRecent;

	JMenuItem mFileNewProject, mFileOpenProject, mFileSave, mFileSaveAs,
			mFileImportDataSet, mFilePrintSetup, mFilePrintPreview, mFilePrint, mFileExit,
			mFileExportDataSet;

	JMenu mView;

	JCheckBoxMenuItem mViewToolBar, mViewStatusBar, mViewTipsPanel;

	JMenuItem mViewDisplaySettings;

	JMenu mAlgn, mAlgnSelect;

	JMenuItem mAlgnFindSeq, mAlgnSelectAll, mAlgnSelectNone, mAlgnSelectUnique,
			mAlgnSelectInvert, mAlgnSelectHighlighted, mAlgnRename,
			mAlgnMoveUp, mAlgnMoveDown, mAlgnGoTo, mAlgnMoveTop, mAlgnRemove,
			mAlgnDisplaySummary, mAlgnPhyloView, mAlgnShowPDialog,
			mAlgnShowOvDialog;

	JMenu mAnls;

	public static JMenu mAnlsRunCodeML;
	
	//Recombination
	JMenuItem mAnlsRunPDM, mAnlsRunDSS, mAnlsRunHMM, mAnlsCreateTree,
			mAnlsPartition, mAnlsShowJobs, mAnlsRename, mAnlsRemove,
			mAnlsSettings, mAnlsRunLRT, mAnlsRunPDM2;
	
	//Positive Selection
	JMenuItem mAnlsRunCodeMLSite, mAnlsRunCodeMLBranch;
	
	//Model test
	JMenuItem mAnlsRunMT;

	//CodonW
	JMenuItem mAnlsRunCW;
	
	JMenu mVamsas;

	JMenuItem mVamSelectSession, mVamCommit;

	JMenu mHelp;

	JMenuItem mHelpContents, mHelpLicense, mHelpAbout, mHelpUpdate,
			mHelpTestMethod;

	public static AbstractAction aFileNewProject, aFileOpenProject, aFileSave,
			aFileSaveAs, aFileImportDataSet, aFilePrintSetup, aFilePrintPreview, aFilePrint,
			aFileExit, aFileExportDataSet;

	public static AbstractAction aViewToolBar, aViewStatusBar, aViewTipsPanel,
			aViewDisplaySettings;

	public static AbstractAction aAlgnFindSeq, aAlgnSelectAll, aAlgnSelectNone,
			aAlgnSelectUnique, aAlgnSelectInvert, aAlgnSelectHighlighted,
			aAlgnRename, aAlgnMoveUp, aAlgnMoveDown, aAlgnMoveTop, aAlgnRemove,
			aAlgnDisplaySummary, aAlgnGoTo, aAlgnPhyloView, aAlgnShowPDialog, aAlgnShowOvDialog;

	//Recombination
	public static AbstractAction aAnlsRunPDM, aAnlsRunDSS, aAnlsRunHMM,
			aAnlsCreateTree, aAnlsPartition, aAnlsShowJobs, aAnlsRename,
			aAnlsRemove, aAnlsSettings, aAnlsRunLRT, aAnlsRunPDM2;
	
	//Positive Selection
	public static AbstractAction aAnlsRunCodeMLSite, aAnlsRunCodeMLBranch;
	
	//Model test
	public static AbstractAction aAnlsRunMT;
	
	//CodonW
	public static AbstractAction aAnlsRunCW;

	public static AbstractAction aVamSelectSession, aVamCommit; //aVamImport, aVamExport;

	public static AbstractAction aVamsasButton;
	
	public static AbstractAction aHelpContents, aHelpLicense, aHelpAbout,
			aHelpUpdate, aHelpTestMethod;

	WinMainMenuBar(WinMain winMain)
	{
		this.winMain = winMain;

		createActions();

		createFileMenu();
		createViewMenu();
		createAlgnMenu();
		createAnlsMenu();
		createVamsasMenu();
		createHelpMenu();

		setBorderPainted(false);
		// setStartupState();
		setProjectOpenedState();
		
		//Register a shortcut for showing log messages
		KeyStroke showLogs = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
		Action aShowLogs = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(GracefulShutdownHandler.instance!=null)
					GracefulShutdownHandler.instance.showLogs(false);
			}
		};
		registerKeyboardAction(aShowLogs, showLogs, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void createActions()
	{
		aFileNewProject = new AbstractAction(Text.Gui
				.getString("aFileNewProject"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileNewProject();
			}
		};

		aFileOpenProject = new AbstractAction(Text.Gui
				.getString("aFileOpenProject"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileOpenProject(null);
			}
		};

		aFileSave = new AbstractAction(Text.Gui.getString("aFileSave"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileSave(false);
			}
		};

		aFileSaveAs = new AbstractAction(Text.Gui.getString("aFileSaveAs"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileSave(true);
			}
		};

		aFileImportDataSet = new AbstractAction(Text.Gui
				.getString("aFileImportDataSet"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileImportDataSet();
			}
		};

		aFileExportDataSet = new AbstractAction(Text.Gui
				.getString("aFileExportDataSet"))
		{
			public void actionPerformed(ActionEvent e)
			{
				WinMain.rDialog.doExport();
			}
		};

		aFilePrintSetup = new AbstractAction(Text.Gui
				.getString("aFilePrintSetup"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrintSetup();
			}
		};

		aFilePrintPreview = new AbstractAction(Text.Gui
				.getString("aFilePrintPreview"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrintPreview();
			}
		};
		
		aFilePrint = new AbstractAction(Text.Gui.getString("aFilePrint"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrint();
			}
		};

		aFileExit = new AbstractAction(Text.Gui.getString("aFileExit"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileExit();
			}
		};

		aViewToolBar = new AbstractAction(Text.Gui.getString("aViewToolBar"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewToolBar();
			}
		};

		aViewStatusBar = new AbstractAction(Text.Gui
				.getString("aViewStatusBar"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewStatusBar();
			}
		};

		aViewTipsPanel = new AbstractAction(Text.Gui
				.getString("aViewTipsPanel"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewTipsPanel();
			}
		};

		aViewDisplaySettings = new AbstractAction(Text.Gui
				.getString("aViewDisplaySettings"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewDisplaySettings(false);
			}
		};

		aAlgnDisplaySummary = new AbstractAction(Text.Gui
				.getString("aAlgnDisplaySummary"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnDisplaySummary();
			}
		};

		aAlgnPhyloView = new AbstractAction(Text.Gui
				.getString("aAlgnPhyloView"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnPhyloView();
			}
		};

		aAlgnFindSeq = new AbstractAction(Text.Gui.getString("aAlgnFindSeq"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnFindSequence();
			}
		};

		aAlgnSelectAll = new AbstractAction(Text.Gui
				.getString("aAlgnSelectAll"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectAll();
			}
		};

		aAlgnSelectNone = new AbstractAction(Text.Gui
				.getString("aAlgnSelectNone"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectNone();
			}
		};

		aAlgnSelectUnique = new AbstractAction(Text.Gui
				.getString("aAlgnSelectUnique"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectUnique();
			}
		};

		aAlgnSelectInvert = new AbstractAction(Text.Gui
				.getString("aAlgnSelectInvert"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectInvert();
			}
		};

		aAlgnSelectHighlighted = new AbstractAction(Text.Gui
				.getString("aAlgnSelectHighlighted"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectHighlighted();
			}
		};

		aAlgnRename = new AbstractAction(Text.Gui.getString("aAlgnRename"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnRename();
			}
		};

		aAlgnMoveUp = new AbstractAction(Text.Gui.getString("aAlgnMoveUp"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(true, false);
			}
		};

		aAlgnMoveDown = new AbstractAction(Text.Gui.getString("aAlgnMoveDown"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(false, false);
			}
		};

		aAlgnMoveTop = new AbstractAction(Text.Gui.getString("aAlgnMoveTop"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(false, true);
			}
		};

		aAlgnRemove = new AbstractAction(Text.Gui.getString("aAlgnRemove"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnRemove();
			}
		};

		aAlgnGoTo = new AbstractAction(Text.Gui.getString("aAlgnGoTo"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnGoTo();
			}
		};

		aAlgnShowPDialog = new AbstractAction(Text.Gui
				.getString("aAlgnShowPDialog"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnShowPartitionDialog();
			}
		};

		aAlgnShowOvDialog = new AbstractAction(Text.Gui
				.getString("aAlgnShowOvDialog"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnShowOvDialog();
			}
		};

		aAnlsRunPDM = new AbstractAction(Text.Gui.getString("aAnlsRunPDM"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunPDM(null);
			}
		};

		aAnlsRunPDM2 = new AbstractAction(Text.Gui.getString("aAnlsRunPDM2"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunPDM2(null);
			}
		};

		aAnlsRunHMM = new AbstractAction(Text.Gui.getString("aAnlsRunHMM"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunHMM(null);
			}
		};

		aAnlsRunDSS = new AbstractAction(Text.Gui.getString("aAnlsRunDSS"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunDSS(null);
			}
		};

		aAnlsRunLRT = new AbstractAction(Text.Gui.getString("aAnlsRunLRT"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunLRT(null);
			}
		};

		aAnlsRunCodeMLSite = new AbstractAction("Site Model Analysis...")
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCodeMLSite(null);
			}
		};

		aAnlsRunCodeMLBranch = new AbstractAction("Branch Model Analysis...")
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCodeMLBranch(null);
			}
		};
		
		aAnlsRunMT = new AbstractAction("Model Selection...") {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunMT(null);
			}
		};
		
		aAnlsRunCW = new AbstractAction("Check Codon Usage...") {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCW(null);
			}
		};
		
		aAnlsCreateTree = new AbstractAction(Text.Gui
				.getString("aAnlsCreateTree"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsCreateTree(null);
			}
		};

		aAnlsPartition = new AbstractAction(Text.Gui
				.getString("aAnlsPartition"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsPartition();
			}
		};

		aAnlsShowJobs = new AbstractAction(Text.Gui.getString("aAnlsShowJobs"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsShowJobs();
			}
		};

		aAnlsRename = new AbstractAction(Text.Gui.getString("aAnlsRename"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRename();
			}
		};

		aAnlsRemove = new AbstractAction(Text.Gui.getString("aAnlsRemove"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRemove();
			}
		};

		aAnlsSettings = new AbstractAction(Text.Gui.getString("aAnlsSettings"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsSettings();
			}
		};

		aVamSelectSession = new AbstractAction(Text.Gui
				.getString("aVamSelectSession"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuVamsasSelectSession();
			}
		};
		
		aVamCommit = new AbstractAction(Text.Gui
				.getString("aVamCommit"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuVamsasCommit();
			}
		};

		aVamsasButton = new AbstractAction("VAMSAS") {
			public void actionPerformed(ActionEvent e)
			{
				if(!ProjectState.isVamsasSession()) {
					winMain.menuVamsasSelectSession();
				}
				else {
					winMain.menuVamsasCommit();
				}
			}
		};
		
		aHelpContents = new AbstractAction(Text.Gui.getString("aHelpContents"))
		{
			public void actionPerformed(ActionEvent e)
			{
			}
		};
		
		aHelpLicense = new AbstractAction(Text.Gui.getString("aHelpLicense"))
		{
			public void actionPerformed(ActionEvent e)
			{
			}
		};

		aHelpAbout = new AbstractAction(Text.Gui.getString("aHelpAbout"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuHelpAbout();
			}
		};

		aHelpUpdate = new AbstractAction(Text.Gui.getString("aHelpUpdate"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuHelpUpdate(true);
			}
		};

		aHelpTestMethod = new AbstractAction("Testing Only...")
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuHelpTestMethod();
			}
		};
	}

	private void createFileMenu()
	{
		mFile = new JMenu(Text.Gui.getString("menuFile"));
		mFile.setMnemonic(KeyEvent.VK_F);

		mFileRecent = new JMenu("Recent Projects");
		mFileRecent.setMnemonic(KeyEvent.VK_R);
		setRecentMenu("");

		mFileNewProject = getItem(aFileNewProject, KeyEvent.VK_N,
				KeyEvent.VK_N, InputEvent.CTRL_MASK, Icons.NEW_PROJECT16);
		mFileOpenProject = getItem(aFileOpenProject, KeyEvent.VK_O,
				KeyEvent.VK_O, InputEvent.CTRL_MASK, Icons.OPEN_PROJECT16);
		mFileSave = getItem(aFileSave, KeyEvent.VK_S, KeyEvent.VK_S,
				InputEvent.CTRL_MASK, Icons.SAVE16);
		mFileSaveAs = getItem(aFileSaveAs, KeyEvent.VK_A, 0, 0, Icons.SAVEAS16);
		mFileSaveAs.setDisplayedMnemonicIndex(5);
		mFileImportDataSet = getItem(aFileImportDataSet, KeyEvent.VK_I, 0, 0,
				Icons.IMPORT16);
		mFileExportDataSet = getItem(aFileExportDataSet, KeyEvent.VK_E, 0, 0);
		mFilePrintSetup = getItem(aFilePrintSetup, KeyEvent.VK_U, 0, 0);
		mFilePrintPreview = getItem(aFilePrintPreview, KeyEvent.VK_V, 0, 0);
		mFilePrint = getItem(aFilePrint, KeyEvent.VK_P, KeyEvent.VK_P,
				InputEvent.CTRL_MASK, Icons.PRINT16);
		mFileExit = getItem(aFileExit, KeyEvent.VK_X, 0, 0);

		mFile.add(mFileNewProject);
		mFile.add(mFileOpenProject);
		mFile.addSeparator();
		mFile.add(mFileSave);
		mFile.add(mFileSaveAs);
		mFile.addSeparator();
		mFile.add(mFileImportDataSet);
		mFile.add(mFileExportDataSet);
		mFile.addSeparator();
		mFile.add(mFilePrintSetup);
		mFile.add(mFilePrintPreview);
		mFile.add(mFilePrint);
		mFile.addSeparator();
		mFile.add(mFileRecent);
		mFile.addSeparator();
		mFile.add(mFileExit);

		add(mFile);
	}

	private void createViewMenu()
	{
		mView = new JMenu(Text.Gui.getString("menuView"));
		mView.setMnemonic(KeyEvent.VK_V);

		mViewToolBar = getItem(aViewToolBar, KeyEvent.VK_T,
				Prefs.gui_toolbar_visible);
		mViewStatusBar = getItem(aViewStatusBar, KeyEvent.VK_S,
				Prefs.gui_statusbar_visible);
		mViewTipsPanel = getItem(aViewTipsPanel, KeyEvent.VK_P,
				Prefs.gui_tips_visible);
		mViewDisplaySettings = getItem(aViewDisplaySettings, KeyEvent.VK_D,
				KeyEvent.VK_F5, 0);

		mView.add(mViewToolBar);
		mView.add(mViewStatusBar);
		mView.add(mViewTipsPanel);
		mView.addSeparator();
		mView.add(mViewDisplaySettings);

		add(mView);
	}

	private void createAlgnMenu()
	{
		mAlgn = new JMenu(Text.Gui.getString("menuAlgn"));
		mAlgn.setMnemonic(KeyEvent.VK_A);
		mAlgnSelect = new JMenu(Text.Gui.getString("menuAlgnSelect"));
		mAlgnSelect.setMnemonic(KeyEvent.VK_S);

		mAlgnDisplaySummary = getItem(aAlgnDisplaySummary, KeyEvent.VK_I, 0, 0,
				Icons.INFO16);
		mAlgnDisplaySummary.setDisplayedMnemonicIndex(16);
		mAlgnPhyloView = getItem(aAlgnPhyloView, KeyEvent.VK_O, 0, 0);
		mAlgnPhyloView.setDisplayedMnemonicIndex(13);
		mAlgnSelectAll = getItem(aAlgnSelectAll, KeyEvent.VK_A, KeyEvent.VK_A,
				InputEvent.CTRL_MASK);
		mAlgnSelectNone = getItem(aAlgnSelectNone, KeyEvent.VK_N, 0, 0);
		mAlgnSelectUnique = getItem(aAlgnSelectUnique, KeyEvent.VK_U, 0, 0);
		mAlgnSelectInvert = getItem(aAlgnSelectInvert, KeyEvent.VK_I, 0, 0);
		mAlgnSelectHighlighted = getItem(aAlgnSelectHighlighted, KeyEvent.VK_H,
				0, 0);

		mAlgnMoveUp = getItem(aAlgnMoveUp, 0, KeyEvent.VK_UP,
				InputEvent.ALT_MASK, Icons.UP16);
		mAlgnMoveUp.setDisplayedMnemonicIndex(15);
		mAlgnMoveDown = getItem(aAlgnMoveDown, KeyEvent.VK_D, KeyEvent.VK_DOWN,
				InputEvent.ALT_MASK, Icons.DOWN16);
		mAlgnMoveTop = getItem(aAlgnMoveTop, 0, 0, 0);
		mAlgnMoveTop.setDisplayedMnemonicIndex(18);
		mAlgnFindSeq = getItem(aAlgnFindSeq, KeyEvent.VK_F, KeyEvent.VK_F,
				InputEvent.CTRL_MASK, Icons.FIND16);
		mAlgnRename = getItem(aAlgnRename, KeyEvent.VK_R, 0, 0);
		mAlgnRemove = getItem(aAlgnRemove, KeyEvent.VK_M, 0, 0, Icons.REMOVE16);
		mAlgnGoTo = getItem(aAlgnGoTo, KeyEvent.VK_G, 0, 0);
		mAlgnShowPDialog = getItem(aAlgnShowPDialog, KeyEvent.VK_P,
				KeyEvent.VK_F3, 0);
		mAlgnShowOvDialog = getItem(aAlgnShowOvDialog, KeyEvent.VK_V,
				KeyEvent.VK_F7, 0);
		
		mAlgnSelect.add(mAlgnSelectAll);
		mAlgnSelect.add(mAlgnSelectNone);
		mAlgnSelect.add(mAlgnSelectHighlighted);
		mAlgnSelect.add(mAlgnSelectUnique);
		mAlgnSelect.addSeparator();
		mAlgnSelect.add(mAlgnSelectInvert);

		mAlgn.add(mAlgnDisplaySummary);
		mAlgn.add(mAlgnPhyloView);
		mAlgn.addSeparator();
		mAlgn.add(mAlgnSelect);
		mAlgn.addSeparator();
		mAlgn.add(mAlgnMoveUp);
		mAlgn.add(mAlgnMoveDown);
		mAlgn.add(mAlgnMoveTop);
		mAlgn.add(mAlgnFindSeq);
		mAlgn.add(mAlgnRename);
		mAlgn.add(mAlgnGoTo);
		mAlgn.addSeparator();
		mAlgn.add(mAlgnShowPDialog);
		mAlgn.add(mAlgnShowOvDialog);
		mAlgn.addSeparator();
		mAlgn.add(mAlgnRemove);

		add(mAlgn);
	}

	private void createAnlsMenu()
	{
		mAnls = new JMenu(Text.Gui.getString("menuAnls"));
		mAnls.setMnemonic(KeyEvent.VK_N);

//		mAnlsRunPDM = getItem(aAnlsRunPDM, KeyEvent.VK_P, 0, 0, Icons.RUN_PDM);
//		mAnlsRunPDM2 = getItem(aAnlsRunPDM2, KeyEvent.VK_P, 0, 0, Icons.RUN_PDM);
//		mAnlsRunDSS = getItem(aAnlsRunDSS, KeyEvent.VK_D, 0, 0, Icons.RUN_DSS);
//		mAnlsRunHMM = getItem(aAnlsRunHMM, KeyEvent.VK_H, 0, 0, Icons.RUN_HMM);
//		mAnlsRunLRT = getItem(aAnlsRunLRT, KeyEvent.VK_L, 0, 0, Icons.RUN_LRT);
		//same without icons:
		mAnlsRunPDM = getItem(aAnlsRunPDM, KeyEvent.VK_P, 0, 0);
		mAnlsRunPDM2 = getItem(aAnlsRunPDM2, KeyEvent.VK_P, 0, 0);
		mAnlsRunDSS = getItem(aAnlsRunDSS, KeyEvent.VK_D, 0, 0);
		mAnlsRunHMM = getItem(aAnlsRunHMM, KeyEvent.VK_H, 0, 0);
		mAnlsRunLRT = getItem(aAnlsRunLRT, KeyEvent.VK_L, 0, 0);
		
		mAnlsRunCodeMLSite = getItem(aAnlsRunCodeMLSite, KeyEvent.VK_S, 0, 0);
		mAnlsRunCodeMLBranch = getItem(aAnlsRunCodeMLBranch, KeyEvent.VK_B, 0, 0);
		
		mAnlsRunMT = getItem(aAnlsRunMT, KeyEvent.VK_M, 0, 0);
		
		mAnlsRunCW = getItem(aAnlsRunCW, KeyEvent.VK_C, 0, 0);
		
		//mAnlsCreateTree = getItem(aAnlsCreateTree, KeyEvent.VK_T, KeyEvent.VK_T, KeyEvent.CTRL_MASK, Icons.CREATE_TREE);
		mAnlsCreateTree = getItem(aAnlsCreateTree, KeyEvent.VK_T, KeyEvent.VK_T, InputEvent.CTRL_MASK);
		mAnlsPartition = getItem(aAnlsPartition, KeyEvent.VK_A, 0, 0);
		mAnlsCreateTree.setDisplayedMnemonicIndex(9);
		mAnlsShowJobs = getItem(aAnlsShowJobs, KeyEvent.VK_J, KeyEvent.VK_J,
				InputEvent.CTRL_MASK);
		mAnlsRename = getItem(aAnlsRename, KeyEvent.VK_N, 0, 0);
		mAnlsRemove = getItem(aAnlsRemove, KeyEvent.VK_R, 0, 0, Icons.REMOVE16);
		mAnlsSettings = getItem(aAnlsSettings, KeyEvent.VK_S, 0, 0,
				Icons.SETTINGS);

		MenuHeading h1 = new MenuHeading("Recombination");
		h1.setIcon(Icons.RECOMBINATION);
		mAnls.add(h1);
		mAnls.add(mAnlsRunPDM);
		mAnls.add(mAnlsRunHMM);
		mAnls.add(mAnlsRunDSS);
		mAnls.add(mAnlsRunLRT);
		mAnls.addSeparator();
		MenuHeading h2 = new MenuHeading("Selection");
		h2.setIcon(Icons.POSSELECTION);
		mAnls.add(h2);
		mAnls.add(mAnlsRunCodeMLSite);
		mAnls.add(mAnlsRunCodeMLBranch);
		mAnls.addSeparator();
		MenuHeading h3 = new MenuHeading("Phylogenetics");
		h3.setIcon(Icons.TREE);
		mAnls.add(h3);
		mAnls.add(mAnlsRunMT);
		mAnls.add(mAnlsCreateTree);
		mAnls.addSeparator();
		MenuHeading h4 = new MenuHeading("Codon Usage");
		h4.setIcon(Icons.CODINGREGIONS);
		mAnls.add(h4);
		mAnls.add(mAnlsRunCW);
		mAnls.addSeparator();
		mAnls.add(mAnlsPartition);
		mAnls.add(mAnlsRename);
		mAnls.add(mAnlsRemove);
		mAnls.addSeparator();
		mAnls.add(mAnlsShowJobs);
		mAnls.add(mAnlsSettings);
		

		add(mAnls);
	}

	private void createVamsasMenu()
	{
		mVamsas = new JMenu(Text.Gui.getString("menuVamsas"));
		mVamsas.setMnemonic(KeyEvent.VK_S);

		mVamSelectSession = getItem(aVamSelectSession, KeyEvent.VK_S, 0, 0);
		mVamCommit = getItem(aVamCommit, KeyEvent.VK_U, 0, 0, Icons.MIDPOINT_ROOT);
		//mVamImport = getItem(aVamImport, KeyEvent.VK_I, 0, 0, Icons.IMPORT16);
		//mVamExport = getItem(aVamExport, KeyEvent.VK_E, 0, 0, Icons.EXPORT);

		mVamsas.add(mVamSelectSession);
		mVamsas.add(mVamCommit);
		//mVamsas.addSeparator();
		//mVamsas.add(mVamImport);
		//mVamsas.add(mVamExport);

		add(mVamsas);
	}

	private void createHelpMenu()
	{
		mHelp = new JMenu(Text.Gui.getString("menuHelp"));
		mHelp.setMnemonic(KeyEvent.VK_H);

		mHelpContents = getItem(aHelpContents, KeyEvent.VK_H, KeyEvent.VK_F1,
				0, Icons.HELP16);
		mHelpLicense = getItem(aHelpLicense, KeyEvent.VK_L, 0, 0);
		mHelpAbout = getItem(aHelpAbout, KeyEvent.VK_A, 0, 0);
		mHelpUpdate = getItem(aHelpUpdate, KeyEvent.VK_C, 0, 0);
		mHelpTestMethod = getItem(aHelpTestMethod, KeyEvent.VK_T, 0, 0);

		TOPALiHelp.enableHelpOnButton(mHelpContents, "intro");
		TOPALiHelp.enableHelpOnButton(mHelpLicense, "license");

		mHelp.add(mHelpContents);
		mHelp.add(mHelpLicense);
		mHelp.addSeparator();
		mHelp.add(mHelpUpdate);
	    mHelp.addSeparator();
		// mHelp.add(mHelpTestMethod);
		// mHelp.addSeparator();
		mHelp.add(mHelpAbout);

		add(mHelp);
	}

	public static JMenuItem getItem(Action act, int m, int k, int mask)
	{
		return getItem(act, m, k, mask, null);
	}

	public static JMenuItem getItem(Action act, int m, int k, int mask,
			ImageIcon icon)
	{
		JMenuItem item = new JMenuItem(act);
		item.setMnemonic(m);

		// if (Prefs.gui_menu_icons)
		{
			if (icon == null)
				item.setIcon(Icons.EMPTY);
			else
				item.setIcon(icon);
		}

		if (k != 0)
			item.setAccelerator(KeyStroke.getKeyStroke(k, mask));
		// if (p != null)
		// item.addMouseListener(textListener);

		return item;
	}

	public static JCheckBoxMenuItem getItem(Action act, int m, boolean state)
	{
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("" + act, state);
		item.setAction(act);
		item.setMnemonic(m);
		// if (p != null)
		// item.addMouseListener(textListener);

		return item;
	}

	public void setProjectOpenedState()
	{
		aFileSave.setEnabled(false);
		aFileSaveAs.setEnabled(true);
		aFileImportDataSet.setEnabled(true);
		aFilePrintPreview.setEnabled(false);
		aFilePrint.setEnabled(false);

		aAlgnShowOvDialog.setEnabled(true);
		aAlgnShowPDialog.setEnabled(true);

		aAnlsShowJobs.setEnabled(true);

		aVamCommit.setEnabled(false);
		
		setMenusForNavChange();
	}

	// Everytime the focus on the navigation tree changes, we disable all
	// dynamic menu options - they are then individually reenabled depending on
	// the new tree tab selected
	public static void setMenusForNavChange()
	{
		aFileExportDataSet.setEnabled(false);
		aFilePrintPreview.setEnabled(false);
		aFilePrint.setEnabled(false);

		aAlgnDisplaySummary.setEnabled(false);
		aAlgnPhyloView.setEnabled(false);
		aAlgnSelectAll.setEnabled(false);
		aAlgnSelectNone.setEnabled(false);
		aAlgnSelectUnique.setEnabled(false);
		aAlgnSelectInvert.setEnabled(false);
		aAlgnSelectHighlighted.setEnabled(false);
		aAlgnMoveUp.setEnabled(false);
		aAlgnMoveDown.setEnabled(false);
		aAlgnMoveTop.setEnabled(false);
		aAlgnFindSeq.setEnabled(false);
		aAlgnRename.setEnabled(false);
		aAlgnRemove.setEnabled(false);
		aAlgnGoTo.setEnabled(false);

		aAnlsRunPDM.setEnabled(false);
		aAnlsRunPDM2.setEnabled(false);
		aAnlsRunHMM.setEnabled(false);
		aAnlsRunDSS.setEnabled(false);
		aAnlsRunLRT.setEnabled(false);
		aAnlsRunCodeMLBranch.setEnabled(false);
		aAnlsRunCodeMLSite.setEnabled(false);
		aAnlsRunMT.setEnabled(false);
		aAnlsRunCW.setEnabled(false);
		aAnlsCreateTree.setEnabled(false);
		aAnlsPartition.setEnabled(false);
		aAnlsRename.setEnabled(false);
		aAnlsRemove.setEnabled(false);
	}

	public void updateRecentFileList(Project project)
	{
		setRecentMenu(project.filename.getPath());
		winMain.setTitle(Text.Gui.getString("WinMain.gui01") + " - "
				+ project.filename.getName());
	}

	void setRecentMenu(String newStr)
	{
		mFileRecent.removeAll();
		int loc = -1;

		// First see if it already exists, and reorder the list if it does
		for (int i = 0; i < Prefs.gui_recent.size(); i++)
		{
			String value = Prefs.gui_recent.get(i);

			if (value.equals(newStr))
				loc = i;
		}

		if (loc != -1)
		{
			Prefs.gui_recent.remove(loc);
			Prefs.gui_recent.addFirst(newStr);
		} else if (newStr.length() > 0)
			Prefs.gui_recent.addFirst(newStr);

		// Then ensure the list only contains 5 elements
		while (Prefs.gui_recent.size() > 5)
			Prefs.gui_recent.removeLast();

		// Finally, convert the list into menu items...
		for (int i = 0; i < Prefs.gui_recent.size(); i++)
		{
			String value = Prefs.gui_recent.get(i);
			createRecentMenuItem(value, (i + 1));
		}

		// ... and enable/disable the menu depending on its contents
		if (Prefs.gui_recent.size() == 0)
			mFileRecent.setEnabled(false);
		else
			mFileRecent.setEnabled(true);
	}

	private void createRecentMenuItem(final String filename, int shortcut)
	{
		JMenuItem item = new JMenuItem(shortcut + " " + filename);

		switch (shortcut)
		{
		case 1:
			item.setMnemonic(KeyEvent.VK_1);
			break;
		case 2:
			item.setMnemonic(KeyEvent.VK_2);
			break;
		case 3:
			item.setMnemonic(KeyEvent.VK_3);
			break;
		case 4:
			item.setMnemonic(KeyEvent.VK_4);
			break;
		case 5:
			item.setMnemonic(KeyEvent.VK_5);
			break;
		}

		item.addActionListener(new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileOpenProject(filename);
			}
		});

		mFileRecent.add(item);
	}
	
	class MenuHeading extends JMenuItem {
		
		public MenuHeading(String text) {
			super(text);
			Font f = getFont();
			setFont(f.deriveFont(Font.BOLD));
		}

		@Override
		public void processMouseEvent(MouseEvent e, MenuElement[] path, MenuSelectionManager manager)
		{
			
		}

		@Override
		protected void processMouseEvent(MouseEvent e)
		{
			
		}
		
		
	}
}