// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.data.Prefs;
import topali.gui.dialog.ExportDialog;
import topali.i18n.Text;
import topali.logging.GracefulShutdownHandler;

import scri.commons.gui.*;

public class WinMainMenuBar extends JMenuBar
{
	 Logger log = Logger.getLogger(this.getClass());

	private WinMain winMain;
	public static int menuShortcut;

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

	JMenuItem mAnlsPartition, mAnlsShowJobs, mAnlsRename, mAnlsRemove, mAnlsSettings;

	//Recombination
	JMenuItem mAnlsRunPDM, mAnlsRunDSS, mAnlsRunHMM, mAnlsRunLRT, mAnlsRunPDM2;

	//Positive Selection
	JMenuItem mAnlsRunCodeMLSite, mAnlsRunCodeMLBranch;

	//Phylogeny
	JMenuItem mAnlsRunMT, mAnlsQuickTree, mAnlsMrBayes, mAnlsPhyml, mAnlsRaxml;
	public static JMenu mAnlsNJ, mAnlsBayes, mAnlsML;

	//CodonW
	JMenuItem mAnlsRunCW;

	JMenu mVamsas;

	JMenuItem mVamSelectSession, mVamCommit;

	private JMenu mWnd;
	public static JMenuItem mWndMinimize, mWndZoom, mWndTOPALi;

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
			aAlgnRenameSeq, aAlgnMoveUp, aAlgnMoveDown, aAlgnMoveTop, aAlgnRemove, aAlgnRename,
			aAlgnDisplaySummary, aAlgnGoTo, aAlgnPhyloView, aAlgnShowPDialog, aAlgnShowOvDialog;

	//Recombination
	public static AbstractAction aAnlsRunPDM, aAnlsRunDSS, aAnlsRunHMM,
			aAnlsPartition, aAnlsShowJobs, aAnlsRename,
			aAnlsRemove, aAnlsSettings, aAnlsRunLRT, aAnlsRunPDM2;

	//Positive Selection
	public static AbstractAction aAnlsRunCodeMLSite, aAnlsRunCodeMLBranch;

	//Phylogeny
	public static AbstractAction aAnlsRunMT, aAnlsQuickTree, aAnlsMrBayes, aAnlsPhyml, aAnlsRaxml;

	//CodonW
	public static AbstractAction aAnlsRunCW;

	public static AbstractAction aVamSelectSession, aVamCommit; //aVamImport, aVamExport;

	public static AbstractAction aVamsasButton;

	public static AbstractAction aWndMinimize, aWndZoom, aWndTOPALi;

	public static AbstractAction aHelpContents, aHelpLicense, aHelpAbout,
			aHelpUpdate, aHelpTestMethod;

	WinMainMenuBar(WinMain winMain)
	{
		this.winMain = winMain;

		createActions();

		// Returns value for "CTRL" under most OSs, and the "apple" key for OS X
		menuShortcut = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		createFileMenu();
		createViewMenu();
		createAlgnMenu();
		createAnlsMenu();
		createVamsasMenu();
		if (SystemUtils.isMacOS())
			createWndMenu();
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

		KeyStroke provokeError = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
		Action aprovokeError = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				String s = null;
				s.length();
			}
		};
		registerKeyboardAction(aprovokeError, provokeError, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void createActions()
	{
		aFileNewProject = new AbstractAction(Text.get("aFileNewProject"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileNewProject();
			}
		};

		aFileOpenProject = new AbstractAction(Text.get("aFileOpenProject"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileOpenProject(null);
			}
		};

		aFileSave = new AbstractAction(Text.get("aFileSave"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileSave(false);
			}
		};

		aFileSaveAs = new AbstractAction(Text.get("aFileSaveAs"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileSave(true);
			}
		};

		aFileImportDataSet = new AbstractAction(Text.get("aFileImportDataSet"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileImportDataSet();
			}
		};

		aFileExportDataSet = new AbstractAction(Text.get("aFileExportDataSet"))
		{
			public void actionPerformed(ActionEvent e)
			{
				new ExportDialog(TOPALi.winMain, TOPALi.winMain.navPanel.getCurrentAlignmentData(), null);
			}
		};

		aFilePrintSetup = new AbstractAction(Text.get("aFilePrintSetup"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrintSetup();
			}
		};

		aFilePrintPreview = new AbstractAction(Text.get("aFilePrintPreview"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrintPreview();
			}
		};

		aFilePrint = new AbstractAction(Text.get("aFilePrint"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFilePrint();
			}
		};

		aFileExit = new AbstractAction(Text.get("aFileExit"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuFileExit();
			}
		};

		aViewToolBar = new AbstractAction(Text.get("aViewToolBar"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewToolBar();
			}
		};

		aViewStatusBar = new AbstractAction(Text.get("aViewStatusBar"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewStatusBar();
			}
		};

		aViewTipsPanel = new AbstractAction(Text.get("aViewTipsPanel"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewTipsPanel();
			}
		};

		aViewDisplaySettings = new AbstractAction(Text.get("aViewDisplaySettings"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuViewDisplaySettings(false);
			}
		};

		aAlgnDisplaySummary = new AbstractAction(Text.get("aAlgnDisplaySummary"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnDisplaySummary();
			}
		};

		aAlgnPhyloView = new AbstractAction(Text.get("aAlgnPhyloView"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnPhyloView();
			}
		};

		aAlgnFindSeq = new AbstractAction(Text.get("aAlgnFindSeq"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnFindSequence();
			}
		};

		aAlgnSelectAll = new AbstractAction(Text.get("aAlgnSelectAll"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectAll();
			}
		};

		aAlgnSelectNone = new AbstractAction(Text.get("aAlgnSelectNone"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectNone();
			}
		};

		aAlgnSelectUnique = new AbstractAction(Text.get("aAlgnSelectUnique"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectUnique();
			}
		};

		aAlgnSelectInvert = new AbstractAction(Text.get("aAlgnSelectInvert"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectInvert();
			}
		};

		aAlgnSelectHighlighted = new AbstractAction(Text.get("aAlgnSelectHighlighted"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnSelectHighlighted();
			}
		};

		aAlgnRenameSeq = new AbstractAction(Text.get("aAlgnRename"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnRenameSeq();
			}
		};

		aAlgnRename = new AbstractAction("Rename")
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnRename();
			}
		};

		aAlgnMoveUp = new AbstractAction(Text.get("aAlgnMoveUp"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(true, false);
			}
		};

		aAlgnMoveDown = new AbstractAction(Text.get("aAlgnMoveDown"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(false, false);
			}
		};

		aAlgnMoveTop = new AbstractAction(Text.get("aAlgnMoveTop"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnMove(false, true);
			}
		};

		aAlgnRemove = new AbstractAction(Text.get("aAlgnRemove"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnRemove();
			}
		};

		aAlgnGoTo = new AbstractAction(Text.get("aAlgnGoTo"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnGoTo();
			}
		};

		aAlgnShowPDialog = new AbstractAction(Text.get("aAlgnShowPDialog"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnShowPartitionDialog();
			}
		};

		aAlgnShowOvDialog = new AbstractAction(Text.get("aAlgnShowOvDialog"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAlgnShowOvDialog();
			}
		};

		aAnlsRunPDM = new AbstractAction(Text.get("aAnlsRunPDM"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunPDM(null);
			}
		};

		aAnlsRunPDM2 = new AbstractAction(Text.get("aAnlsRunPDM2"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunPDM2(null);
			}
		};

		aAnlsRunHMM = new AbstractAction(Text.get("aAnlsRunHMM"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunHMM(null);
			}
		};

		aAnlsRunDSS = new AbstractAction(Text.get("aAnlsRunDSS"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunDSS(null);
			}
		};

		aAnlsRunLRT = new AbstractAction(Text.get("aAnlsRunLRT"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunLRT(null);
			}
		};

		aAnlsRunCodeMLSite = new AbstractAction(Text.get("aAnlsRunCodeMLSite"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCodeMLSite(null);
			}
		};

		aAnlsRunCodeMLBranch = new AbstractAction(Text.get("aAnlsRunCodeMLBranch"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCodeMLBranch(null);
			}
		};

		aAnlsRunMT = new AbstractAction(Text.get("aAnlsRunMT")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunMT(null);
			}
		};

		aAnlsQuickTree = new AbstractAction(Text.get("aAnlsQuickTree")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsQuickTree();
			}
		};

		aAnlsMrBayes = new AbstractAction(Text.get("aAnlsMrBayes")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsMrBayes(null);
			}
		};

		aAnlsPhyml = new AbstractAction(Text.get("aAnlsPhyml")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsPhyml(null);
			}
		};

		aAnlsRaxml = new AbstractAction(Text.get("aAnlsRaxml")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRaxml(null);
			}
		};

		aAnlsRunCW = new AbstractAction(Text.get("aAnlsRunCW")) {
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRunCW(null);
			}
		};

		aAnlsPartition = new AbstractAction(Text.get("aAnlsPartition"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsPartition();
			}
		};

		aAnlsShowJobs = new AbstractAction(Text.get("aAnlsShowJobs"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsShowJobs();
			}
		};

		aAnlsRename = new AbstractAction(Text.get("aAnlsRename"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRename();
			}
		};

		aAnlsRemove = new AbstractAction(Text.get("aAnlsRemove"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsRemove();
			}
		};

		aAnlsSettings = new AbstractAction(Text.get("aAnlsSettings"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuAnlsSettings();
			}
		};

		aVamSelectSession = new AbstractAction(Text.get("aVamSelectSession"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuVamsasSelectSession();
			}
		};

		aVamCommit = new AbstractAction(Text.get("aVamCommit"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuVamsasCommit();
			}
		};

		aVamsasButton = new AbstractAction(Text.get("aVamsasButton")) {
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

		aWndMinimize = new AbstractAction(Text.get("aWndMinimize"))
		{
			public void actionPerformed(ActionEvent e)
			{
				TOPALi.osxMinimize();
			}
		};

		aWndZoom = new AbstractAction(Text.get("aWndZoom"))
		{
			public void actionPerformed(ActionEvent e)
			{
				TOPALi.osxZoom();
			}
		};

		aWndTOPALi = new AbstractAction(Text.get("aWndTOPALi"))
		{
			public void actionPerformed(ActionEvent e)
			{
				TOPALi.osxTOPALi();
			}
		};

		aHelpContents = new AbstractAction(Text.get("aHelpContents"))
		{
			public void actionPerformed(ActionEvent e)
			{
			}
		};

		aHelpLicense = new AbstractAction(Text.get("aHelpLicense"))
		{
			public void actionPerformed(ActionEvent e)
			{
			}
		};

		aHelpAbout = new AbstractAction(Text.get("aHelpAbout"))
		{
			public void actionPerformed(ActionEvent e)
			{
				winMain.menuHelpAbout();
			}
		};

		aHelpUpdate = new AbstractAction(Text.get("aHelpUpdate"))
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
		mFile = new JMenu(Text.get("menuFile"));
		mFile.setMnemonic(KeyEvent.VK_F);

		mFileRecent = new JMenu(Text.get("mFileRecent"));
		mFileRecent.setMnemonic(KeyEvent.VK_R);
		setRecentMenu("");

		mFileNewProject = getItem(aFileNewProject, KeyEvent.VK_N,
				KeyEvent.VK_N, menuShortcut, getIcon(Icons.NEW_PROJECT16));
		mFileOpenProject = getItem(aFileOpenProject, KeyEvent.VK_O,
				KeyEvent.VK_O, menuShortcut, getIcon(Icons.OPEN_PROJECT16));
		mFileSave = getItem(aFileSave, KeyEvent.VK_S, KeyEvent.VK_S,
				menuShortcut, getIcon(Icons.SAVE16));
		mFileSaveAs = getItem(aFileSaveAs, KeyEvent.VK_A, 0, 0, getIcon(Icons.SAVEAS16));
		mFileSaveAs.setDisplayedMnemonicIndex(5);
		mFileImportDataSet = getItem(aFileImportDataSet, KeyEvent.VK_I, 0, 0,
				getIcon(Icons.IMPORT16));
		mFileExportDataSet = getItem(aFileExportDataSet, KeyEvent.VK_E, 0, 0);
		mFilePrintSetup = getItem(aFilePrintSetup, KeyEvent.VK_U, 0, 0);
		mFilePrintPreview = getItem(aFilePrintPreview, KeyEvent.VK_V, 0, 0);
		mFilePrint = getItem(aFilePrint, KeyEvent.VK_P, KeyEvent.VK_P,
				menuShortcut, getIcon(Icons.PRINT16));
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
		// We don't add these options to OS X as they are auto-added by Apple
		if (SystemUtils.isMacOS() == false)
		{
			mFile.addSeparator();
			mFile.add(mFileExit);
		}

		add(mFile);
	}

	private void createViewMenu()
	{
		mView = new JMenu(Text.get("menuView"));
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
		mAlgn = new JMenu(Text.get("menuAlgn"));
		mAlgn.setMnemonic(KeyEvent.VK_A);
		mAlgnSelect = new JMenu(Text.get("menuAlgnSelect"));
		mAlgnSelect.setMnemonic(KeyEvent.VK_S);

		mAlgnDisplaySummary = getItem(aAlgnDisplaySummary, KeyEvent.VK_I, 0, 0,
				getIcon(Icons.INFO16));
		mAlgnDisplaySummary.setDisplayedMnemonicIndex(16);
		mAlgnPhyloView = getItem(aAlgnPhyloView, KeyEvent.VK_O, 0, 0);
		mAlgnPhyloView.setDisplayedMnemonicIndex(13);
		mAlgnSelectAll = getItem(aAlgnSelectAll, KeyEvent.VK_A, KeyEvent.VK_A,
				menuShortcut);
		mAlgnSelectNone = getItem(aAlgnSelectNone, KeyEvent.VK_N, 0, 0);
		mAlgnSelectUnique = getItem(aAlgnSelectUnique, KeyEvent.VK_U, 0, 0);
		mAlgnSelectInvert = getItem(aAlgnSelectInvert, KeyEvent.VK_I, 0, 0);
		mAlgnSelectHighlighted = getItem(aAlgnSelectHighlighted, KeyEvent.VK_H,
				0, 0);

		mAlgnMoveUp = getItem(aAlgnMoveUp, 0, KeyEvent.VK_UP,
				InputEvent.ALT_MASK, getIcon(Icons.UP16));
		mAlgnMoveUp.setDisplayedMnemonicIndex(15);
		mAlgnMoveDown = getItem(aAlgnMoveDown, KeyEvent.VK_D, KeyEvent.VK_DOWN,
				InputEvent.ALT_MASK, getIcon(Icons.DOWN16));
		mAlgnMoveTop = getItem(aAlgnMoveTop, 0, 0, 0);
		mAlgnMoveTop.setDisplayedMnemonicIndex(18);
		mAlgnFindSeq = getItem(aAlgnFindSeq, KeyEvent.VK_F, KeyEvent.VK_F,
				menuShortcut, getIcon(Icons.FIND16));
		mAlgnRename = getItem(aAlgnRenameSeq, KeyEvent.VK_R, 0, 0);
		mAlgnRemove = getItem(aAlgnRemove, KeyEvent.VK_M, 0, 0, getIcon(Icons.REMOVE16));
		mAlgnGoTo = getItem(aAlgnGoTo, KeyEvent.VK_G, 0, 0);
		mAlgnShowPDialog = getItem(aAlgnShowPDialog, KeyEvent.VK_A,
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
		mAnls = new JMenu(Text.get("menuAnls"));
		mAnls.setMnemonic(KeyEvent.VK_N);

		//Recombination
		mAnlsRunPDM = getItem(aAnlsRunPDM, KeyEvent.VK_P, 0, 0);
		mAnlsRunPDM2 = getItem(aAnlsRunPDM2, KeyEvent.VK_P, 0, 0);
		mAnlsRunDSS = getItem(aAnlsRunDSS, KeyEvent.VK_D, 0, 0);
		mAnlsRunHMM = getItem(aAnlsRunHMM, KeyEvent.VK_H, 0, 0);
		mAnlsRunLRT = getItem(aAnlsRunLRT, KeyEvent.VK_L, 0, 0);

		//Positive Selection
		mAnlsRunCodeMLSite = getItem(aAnlsRunCodeMLSite, KeyEvent.VK_S, 0, 0);
		mAnlsRunCodeMLBranch = getItem(aAnlsRunCodeMLBranch, KeyEvent.VK_B, 0, 0);

		//Phylogeny
		mAnlsRunMT = getItem(aAnlsRunMT, KeyEvent.VK_M, 0, 0);
		mAnlsQuickTree = getItem(aAnlsQuickTree, KeyEvent.VK_Q, 0, 0);
		mAnlsMrBayes = getItem(aAnlsMrBayes, 0, 0, 0);
		mAnlsPhyml = getItem(aAnlsPhyml, 0, 0, 0);
		mAnlsRaxml = getItem(aAnlsRaxml, 0, 0, 0);
		mAnlsNJ = new JMenu(Text.get("mNJ"));
		mAnlsBayes = new JMenu(Text.get("mBayes"));
		mAnlsML = new JMenu(Text.get("mML"));

		//Misc
		mAnlsRunCW = getItem(aAnlsRunCW, KeyEvent.VK_C, 0, 0);

		mAnlsPartition = getItem(aAnlsPartition, KeyEvent.VK_A, 0, 0);
		mAnlsShowJobs = getItem(aAnlsShowJobs, KeyEvent.VK_J, KeyEvent.VK_J,
				menuShortcut);
		mAnlsRename = getItem(aAnlsRename, KeyEvent.VK_N, 0, 0);
		mAnlsRemove = getItem(aAnlsRemove, KeyEvent.VK_R, 0, 0, getIcon(Icons.REMOVE16));
		mAnlsSettings = getItem(aAnlsSettings, KeyEvent.VK_S, 0, 0,
				getIcon(Icons.SETTINGS));

		JMenu h1 = new JMenu(Text.get("mRecomb"));
		h1.setIcon(getIcon(Icons.RECOMBINATION));
		mAnls.add(h1);
		h1.add(mAnlsRunPDM);
		h1.add(mAnlsRunHMM);
		h1.add(mAnlsRunDSS);
		h1.add(mAnlsRunLRT);
//		mAnls.addSeparator();
		JMenu h2 = new JMenu(Text.get("mSelection"));
		h2.setIcon(getIcon(Icons.POSSELECTION));
		mAnls.add(h2);
		h2.add(mAnlsRunCodeMLSite);
		h2.add(mAnlsRunCodeMLBranch);
//		mAnls.addSeparator();
		JMenu h3 = new JMenu(Text.get("mPhylo"));
		h3.setIcon(getIcon(Icons.TREE));
		mAnls.add(h3);
		h3.add(mAnlsRunMT);
			mAnlsNJ.add(mAnlsQuickTree);
		h3.add(mAnlsNJ);
			mAnlsBayes.add(mAnlsMrBayes);
		h3.add(mAnlsBayes);
			mAnlsML.add(mAnlsPhyml);
			mAnlsML.add(mAnlsRaxml);
		h3.add(mAnlsML);
		//mAnls.add(mAnlsCreateTree);
//		mAnls.addSeparator();
		JMenu h4 = new JMenu(Text.get("mCodon"));
		h4.setIcon(getIcon(Icons.CODINGREGIONS));
		mAnls.add(h4);
		h4.add(mAnlsRunCW);
		mAnls.addSeparator();
		mAnls.add(mAnlsPartition);
		mAnls.add(mAnlsRename);
		mAnls.add(mAnlsRemove);
		mAnls.addSeparator();
		mAnls.add(mAnlsShowJobs);
		if (SystemUtils.isMacOS() == false)
			mAnls.add(mAnlsSettings);


		add(mAnls);
	}

	private void createVamsasMenu()
	{
		mVamsas = new JMenu(Text.get("menuVamsas"));
		mVamsas.setMnemonic(KeyEvent.VK_S);

		mVamSelectSession = getItem(aVamSelectSession, KeyEvent.VK_S, 0, 0);
		mVamCommit = getItem(aVamCommit, KeyEvent.VK_U, 0, 0, getIcon(Icons.MIDPOINT_ROOT));
		//mVamImport = getItem(aVamImport, KeyEvent.VK_I, 0, 0, Icons.IMPORT16);
		//mVamExport = getItem(aVamExport, KeyEvent.VK_E, 0, 0, Icons.EXPORT);

		mVamsas.add(mVamSelectSession);
		mVamsas.add(mVamCommit);
		//mVamsas.addSeparator();
		//mVamsas.add(mVamImport);
		//mVamsas.add(mVamExport);

		add(mVamsas);
	}

	private void createWndMenu()
	{
		mWnd = new JMenu(Text.get("menuWnd"));
		mWnd.setMnemonic(KeyEvent.VK_W);

		mWndMinimize = getItem(aWndMinimize, KeyEvent.VK_M, KeyEvent.VK_M, menuShortcut);
		mWndZoom = getItem(aWndZoom, KeyEvent.VK_Z, 0, 0);
		mWndTOPALi = getCheckedItem(aWndTOPALi, KeyEvent.VK_F, 0, 0, true);

		mWnd.add(mWndMinimize);
		mWnd.add(mWndZoom);
		mWnd.addSeparator();
		mWnd.add(mWndTOPALi);

		add(mWnd);
	}

	private void createHelpMenu()
	{
		mHelp = new JMenu(Text.get("menuHelp"));
		mHelp.setMnemonic(KeyEvent.VK_H);

		mHelpContents = getItem(aHelpContents, KeyEvent.VK_H, KeyEvent.VK_F1,
				0, getIcon(Icons.HELP16));
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
		// We don't add this option to OS X as it is auto-added by Apple
		if (SystemUtils.isMacOS() == false)
		{
			mHelp.addSeparator();
			mHelp.add(mHelpAbout);
		}

		add(mHelp);
	}

	// Returns null for any given icon on the Mac, meaning they are never shown
	// (because Apple doesn't want icons on menus for some reason)
	private ImageIcon getIcon(ImageIcon icon)
	{
		if (SystemUtils.isMacOS())
			return null;
		else
			return icon;
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

		item.setIcon(icon);

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

	public static JCheckBoxMenuItem getCheckedItem(Action action, int mnemonic, int keymask, int modifiers, boolean state)
	{
		JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
		item.setMnemonic(mnemonic);
		item.setState(state);

		if (keymask != 0)
			item.setAccelerator(KeyStroke.getKeyStroke(keymask, modifiers));

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
		aAlgnRenameSeq.setEnabled(false);
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

		mAnlsNJ.setEnabled(false);
		aAnlsQuickTree.setEnabled(false);
		mAnlsBayes.setEnabled(false);
		aAnlsMrBayes.setEnabled(false);
		mAnlsML.setEnabled(false);
		aAnlsPhyml.setEnabled(false);
		aAnlsRaxml.setEnabled(false);
		aAnlsRunCW.setEnabled(false);
		aAnlsPartition.setEnabled(false);
		aAnlsRename.setEnabled(false);
		aAnlsRemove.setEnabled(false);
	}

	public void updateRecentFileList(Project project)
	{
		setRecentMenu(project.filename.getPath());
		winMain.setTitle(Text.get("title") + " - "
				+ project.filename.getName());
	}

	void setRecentMenu(String newStr)
	{
		mFileRecent.removeAll();
		int loc = -1;

		LinkedList<String> gui_recent = new LinkedList<String>();
		if(Prefs.gui_recent0!=null && !Prefs.gui_recent0.equals("null"))
		    gui_recent.add(Prefs.gui_recent0);
		if(Prefs.gui_recent1!=null && !Prefs.gui_recent1.equals("null"))
		    gui_recent.add(Prefs.gui_recent1);
		if(Prefs.gui_recent2!=null && !Prefs.gui_recent2.equals("null"))
		    gui_recent.add(Prefs.gui_recent2);
		if(Prefs.gui_recent3!=null && !Prefs.gui_recent3.equals("null"))
		    gui_recent.add(Prefs.gui_recent3);

		// First see if it already exists, and reorder the list if it does
		for (int i = 0; i < gui_recent.size(); i++)
		{
			String value = gui_recent.get(i);

			if (value.equals(newStr))
				loc = i;
		}

		if (loc != -1)
		{
			gui_recent.remove(loc);
			gui_recent.addFirst(newStr);
		} else if (newStr.length() > 0)
			gui_recent.addFirst(newStr);

		// Then ensure the list only contains 5 elements
		while (gui_recent.size() > 4)
			gui_recent.removeLast();

		// Finally, convert the list into menu items...
		for (int i = 0; i < gui_recent.size(); i++)
		{
			String value = gui_recent.get(i);
			createRecentMenuItem(value, (i + 1));
		}

		// ... and enable/disable the menu depending on its contents
		if (gui_recent.size() == 0)
			mFileRecent.setEnabled(false);
		else
			mFileRecent.setEnabled(true);

		for(int i=0; i<gui_recent.size(); i++) {
		    if(i==0)
			Prefs.gui_recent0 = gui_recent.get(0);
		    if(i==1)
			Prefs.gui_recent1 = gui_recent.get(1);
		    if(i==2)
			Prefs.gui_recent2 = gui_recent.get(2);
		    if(i==3)
			Prefs.gui_recent3 = gui_recent.get(3);
		}
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