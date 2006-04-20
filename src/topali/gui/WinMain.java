// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.io.*;
import javax.swing.*;

import topali.analyses.*;
import topali.cluster.*;
import topali.data.*;
import topali.gui.dialog.*;
import topali.gui.dialog.hmm.*;
import topali.gui.nav.*;
import topali.gui.tree.*;

import pal.alignment.*;
import pal.tree.*;

import doe.*;

public class WinMain extends JFrame
{
	// The user's project
	private Project project = new Project();
	
	// GUI controls...
	private WinMainMenuBar menubar;
	private WinMainToolBar toolbar;
	private WinMainStatusBar sbar;
	private WinMainTipsPanel tips;

	// More GUI controls...except this time other packages need access to them
	public static NavPanel navPanel;
	public static JSplitPane splits;
	public static JobsPanel jobsPanel;
	public static PartitionDialog pDialog;
	public static OverviewDialog ovDialog;
	public static FileDropAdapter dropAdapter;
		
	public WinMain()
	{
		// GUI Control initialization
		createControls();
		
		// Position on screen
		setSize(Prefs.gui_win_width, Prefs.gui_win_height);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		setIconImage(Icons.APP_FRAME.getImage());
		setTitle(Text.Gui.getString("WinMain.gui01"));
		
		if (Prefs.gui_maximized)
			setExtendedState(JFrame.MAXIMIZED_BOTH);
	}
	
	public Project getProject()
		{ return project; }
	
	private void createControls()
	{
		dropAdapter = new FileDropAdapter(this);
		
		menubar = new WinMainMenuBar(this);
		setJMenuBar(menubar);
		
		tips = new WinMainTipsPanel();		
		jobsPanel = new JobsPanel(this);		
		pDialog = new PartitionDialog(this);
		ovDialog = new OverviewDialog(this);
		
		splits = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splits.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		splits.setOneTouchExpandable(true);
		splits.setDropTarget(new DropTarget(splits, dropAdapter));
		navPanel = new NavPanel(this, tips, splits);
		add(splits);
		
		toolbar = new WinMainToolBar(menubar, navPanel);
		add(toolbar, BorderLayout.NORTH);
	
		sbar = new WinMainStatusBar(this);
		add(sbar, BorderLayout.SOUTH);
	}

	/* Called whenever an option has been selected that would close the current
	 * project. The user is therefore queried to find out if they would like to
	 * save it first (or cancel the operation).
	 */
	boolean okToContinue()
	{
		if (project != null)
		{
			if (menubar.aFileSave.isEnabled())
			{
				// TODO: warn about shutting down with local jobs active
				String msg = "The current project has unsaved changes. Save now?";
				if (jobsPanel.hasJobs())
					msg = "The current project has unsaved changes (and analysis jobs are still running!). Save now?";
				
				int res = MsgBox.yesnocan(msg, 0);
								
				if (res == JOptionPane.YES_OPTION)
				{
					if (Project.save(project, false))
						return true;
					else
						return false;
				}
				else if (res == JOptionPane.NO_OPTION)
					return true;
				else if (res == JOptionPane.CANCEL_OPTION ||
					res == JOptionPane.CLOSED_OPTION)
					return false;
			}
		}
		
		// Cancels all locally running jobs, regardless of project status
		LocalJobs.cancelAll();
		
		return true;
	}
	
	public static void repaintDisplay()
		{ splits.getRightComponent().repaint(); }
	
	void menuFileNewProject()
	{
		if (!okToContinue())
			return;

		project = new Project();
		
		setTitle(Text.Gui.getString("WinMain.gui01"));
		menubar.setProjectOpenedState();
		navPanel.clear();
			
		pDialog.setAlignmentData(null);
		ovDialog.setAlignmentPanel(null);
		
		// Anything done here may need to go in LoadMonitorDialog.java too
	}
	
	// Opens an existing project
	void menuFileOpenProject(String name)
	{
		if (!okToContinue())
			return;
		
		LoadMonitorDialog dialog = new LoadMonitorDialog(this, menubar, name);	
		
		if (dialog.getProject() != null)
			project = dialog.getProject();
	}
	
	public void menuFileImportDataSet()
	{
		ImportOptionsDialog optionsDialog = new ImportOptionsDialog(this);
		
		if (optionsDialog.isOK())
		{
			switch (Prefs.gui_import_method)
			{
				case 0:
					new ImportDataSetDialog(this).promptForAlignment();
					break;
				case 1: 
					break;
				case 2:
					// TODO:
					break;
			}
		}
	}
	
	public void menuFileImportDataSet(File file)
	{
		new ImportDataSetDialog(this).loadAlignment(file);
	}
	
	// Adds an AlignmentData object to the current project
	public void addNewAlignmentData(AlignmentData data)
	{
		if (data != null)
		{
			project.getDatasets().add(data);
			menubar.aFileSave.setEnabled(true);
			navPanel.addAlignmentFolder(data);
		}
	}
	
	void menuFileExportDataSet()
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		new ExportDialog(this, data);
	}
	
	/* Save the current project to disk. */
	void menuFileSave(boolean saveAs)
	{
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		
		if (Project.save(project, saveAs))
		{
			menubar.aFileSave.setEnabled(false);
			menubar.updateRecentFileList(project);
		}
		
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	void menuFilePrintSetup()
	{
		PrinterDialog.showPageSetupDialog(this);
	}
	
	void menuFilePrint()
	{
		IPrintable toPrint = navPanel.getSelectedPrintableNode();
		toPrint.print();
	}
		
	void menuFileExit()
	{
		WindowEvent evt = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
		processWindowEvent(evt);
	}
	
	void menuViewToolBar()
	{
		toolbar.setVisible(Prefs.gui_toolbar_visible = !toolbar.isVisible());
	}
	
	void menuViewStatusBar()
	{
		sbar.setVisible(Prefs.gui_statusbar_visible = !sbar.isVisible());
	}
	
	void menuViewTipsPanel()
	{
		Prefs.gui_tips_visible = !Prefs.gui_tips_visible;
		navPanel.toggleTipsPanelVisibility();
	}
	
	public void menuViewDisplaySettings(boolean refreshOnly)
	{
		// Do we want to show the settings dialog?
		if (refreshOnly == false)
			new DisplaySettingsDialog(this);
		
		for (SequenceSetNode node: navPanel.getSequenceSetNodes())
			((AlignmentPanel) node.getPanel()).refreshAndRepaint();
	}
	
	void menuAlgnShowOvDialog()
	{
		ovDialog.setVisible(true);
	}
	
	void menuAlgnDisplaySummary()
	{
		new SummaryDialog(this, navPanel.getCurrentAlignmentData());
	}
	
	void menuAlgnPhyloView()
	{
		SequenceSet ss = navPanel.getCurrentAlignmentData().getSequenceSet();

		new MovieDialog(this, ss);
	}
	
	void menuAlgnSelectAll()
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		p.getListPanel().selectAll();
	}
	
	void menuAlgnSelectNone()
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		p.getListPanel().selectNone();
	}
	
	void menuAlgnSelectUnique()
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		p.getListPanel().selectUnique();
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}
	
	void menuAlgnSelectInvert()
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		p.getListPanel().selectInvert();
	}
	
	void menuAlgnMove(boolean up, boolean top)
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		p.getListPanel().moveSequences(up, top);
	}
	
	void menuAlgnFindSequence()
	{
		new FindSequenceDialog(this, navPanel.getCurrentAlignmentPanel(null));
	}
	
	void menuAlgnGoTo()
	{
		new GoToNucDialog(this, navPanel.getCurrentAlignmentPanel(null));
	}
	
	void menuAlgnRename()
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
				
		if (SequenceSetUtils.renameSequence(data.getSequenceSet()))
		{
			// Force the display to redraw itself
			navPanel.getCurrentAlignmentPanel(data).refreshAndRepaint();
			// And mark the project as modified
			menubar.aFileSave.setEnabled(true);
		}
	}
	
	void menuAlgnShowPartitionDialog()
	{
		pDialog.setVisible(true);
	}
	
	/* Removes an alignment from the current project. */
	void menuAlgnRemove()
	{
		// Find out which alignment needs removing
		AlignmentData data = navPanel.getCurrentAlignmentData();
		if (jobsPanel.hasJobs(data))
		{
			MsgBox.msg(Text.Gui.getString("WinMain.msg03"), MsgBox.WAR);
			return;
		}
		
		String msg = Text.Gui.getString("WinMain.msg02");
		if (MsgBox.yesno(msg, 1) == JOptionPane.YES_OPTION)
		{
			// Then remove it from both the project and the navigation panel
			project.removeDataSet(data);
			
			navPanel.removeSelectedNode();
			pDialog.setAlignmentData(null);
			ovDialog.setAlignmentPanel(null);
			
			menubar.aFileSave.setEnabled(true);
		}
	}
	
	public void menuAnlsRunPDM(PDMResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunPDM(ss) == false)
			return;
		
		PDMResult result = new PDMSettingsDialog(this, data, iResult).getPDMResult();
		if (result == null)
			return;
		
		// TODO: move this (and hmm/dss) into the SettingsDialog for each job type
		int runNum = data.getTracker().getPdmRunCount() + 1;
		data.getTracker().setPdmRunCount(runNum);
		result.guiName = "PDM Result " + runNum;
		result.jobName = "PDM Analysis " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		submitJob(data, result);
	}
	
	public void menuAnlsRunPDM2(PDMResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunPDM(ss) == false)
			return;
		
		PDMResult result = new PDMSettingsDialog(this, data, iResult).getPDMResult();
		if (result == null)
			return;
		
		// TODO: move this (and hmm/dss) into the SettingsDialog for each job type
		// TODO: increment a counter for PDM2 not PDM(1)
		int runNum = data.getTracker().getPdmRunCount() + 1;
		data.getTracker().setPdmRunCount(runNum);
		result.guiName = "PDM Result " + runNum;
		result.jobName = "PDM Analysis " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		submitJob(data, result);
	}
	
	public void menuAnlsRunHMM(HMMResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunHMM(ss) == false)
			return;
		
		HMMResult result = new HMMSettingsDialog(this, data, iResult).getHMMResult();
		if (result == null)
			return;
		
		int runNum = data.getTracker().getHmmRunCount() + 1;
		data.getTracker().setHmmRunCount(runNum);
		result.guiName = "HMM Result " + runNum;
		result.jobName = "HMM Analysis " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		submitJob(data, result);
	}
	
	public void menuAnlsRunDSS(DSSResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunDSS(ss) == false)
			return;
		
		DSSResult result = new DSSSettingsDialog(this, data, iResult).getDSSResult();
		if (result == null)
			return;
		
		int runNum = data.getTracker().getDssRunCount() + 1;
		data.getTracker().setDssRunCount(runNum);
		result.guiName = "DSS Result " + runNum;
		result.jobName = "DSS Analysis " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		submitJob(data, result);
	}
	
	public void menuAnlsRunLRT(LRTResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunLRT(ss) == false)
			return;
		
		LRTResult result = new LRTSettingsDialog(this, data, iResult).getLRTResult();
		if (result == null)
			return;
		
		int runNum = data.getTracker().getLrtRunCount() + 1;
		data.getTracker().setLrtRunCount(runNum);
		result.guiName = "LRT Result " + runNum;
		result.jobName = "LRT Analysis " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		submitJob(data, result);
	}
	
	void menuAnlsCreateTree()
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		
		CreateTreeDialog dialog = new CreateTreeDialog(this, data);
		TreeResult result = dialog.getTreeResult();
		if (result == null)
			return;
		
		// Tree being created on-the-fly as part of TOPALi
		if (Prefs.gui_tree_method == 0)
		{
			TreePane treePane = navPanel.getCurrentTreePane(data, true);
			TreeCreator creator = new TreeCreator(dialog.getAlignment());
			Tree palTree = creator.getTree(true);
			
			if (palTree != null)
			{
				result.setTreeStr(palTree.toString());
				result.status = topali.cluster.JobStatus.COMPLETED;
				
				// Add the tree to the project
				data.getResults().add(result);
				
				treePane.displayTree(data.getSequenceSet(), result);			
				menubar.aFileSave.setEnabled(true);
			}
		}
		// Tree being created as a cluster/local job
		else
			submitJob(data, result);
	}
	
	private void submitJob(AlignmentData data, AnalysisResult result)
	{
		data.getResults().add(result);
		
		jobsPanel.createJob(result, data);
		menuAnlsShowJobs();
	}
	
	void menuAnlsPartition()
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		AnalysisResult result = navPanel.getCurrentAnalysisResult();
				
		new AutoPartitionDialog(this, data, result);
	}
	
	void menuAnlsShowJobs()
	{
		menubar.setMenusForNavChange();
		navPanel.clearSelection();
		pDialog.setAlignmentData(null);
		ovDialog.setAlignmentPanel(null);
		
		int location = splits.getDividerLocation();
		splits.setRightComponent(jobsPanel);
		splits.setDividerLocation(location);
		tips.setDisplayedTips(WinMainTipsPanel.TIPS_JOB);
	}
	
	void menuAnlsRemove()
	{
		String msg = Text.Gui.getString("WinMain.msg04");
		if (MsgBox.yesno(msg, 1) == JOptionPane.YES_OPTION)
		{
			// Find out which alignment the result is being removed from
			AlignmentData data = navPanel.getCurrentAlignmentData();
			// And what result is being removed
			AnalysisResult result = navPanel.getCurrentAnalysisResult();
			
			// Then remove it from both the alignment and the navigation panel
			data.removeResult(result);
			navPanel.removeSelectedNode();
			
			menubar.aFileSave.setEnabled(true);
		}
	}
	
	void menuAnlsRename()
	{
		// Find out which alignment the result is being removed from
		AlignmentData data = navPanel.getCurrentAlignmentData();
		// And what result is being removed
		AnalysisResult result = navPanel.getCurrentAnalysisResult();
		
		String newName = (String) JOptionPane.showInputDialog(this, 
			"Enter a new name for this result:", "Rename Result",
			JOptionPane.PLAIN_MESSAGE, null, null, result.guiName);

		if (newName != null)
		{		
			result.guiName = newName;
			navPanel.nodesChanged();
			menubar.aFileSave.setEnabled(true);
		}
	}
	
	// Selects the sequences that match the safenames in the given array
	public void menuAnlsReselectSequences(String[] seqs)
	{
		// Find the current dataset and sequenceset
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		
		// Work out the indices of the given sequences
		int[] indices = ss.getIndicesFromNames(seqs);
		
		// And select them in the GUI (which will reselect them in the ss)
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(data);
		p.getListPanel().updateList(indices);
	}
	
	void menuAnlsSettings()
	{
		new topali.gui.dialog.settings.SettingsDialog(this);
	}
	
	void menuVamsasImport()
	{
		topali.vamsas.FileHandler fh = new topali.vamsas.FileHandler();
		
		AlignmentData[] datasets = fh.loadVamsas();
		
		if (datasets != null)
		{
			for (AlignmentData data1: datasets)
				addNewAlignmentData(data1);
		
			if (datasets.length == 1)
				MsgBox.msg("1 alignment has been imported into TOPALi.", MsgBox.INF);
			else
				MsgBox.msg(datasets.length + " alignments have been imported into TOPALi.", MsgBox.INF);
		}
	}
	
	void menuVamsasExport()
	{
		topali.vamsas.FileHandler fh = new topali.vamsas.FileHandler();
		
		AlignmentData data = navPanel.getCurrentAlignmentData();
		fh.saveVamsas(data);
	}
	
	void menuHelpUpdate(boolean useGUI)
	{
		new UpdateChecker(useGUI);
	}
	
	void menuHelpAbout()
	{
		UpdateChecker.helpAbout();
	}
	
	// Takes an existing PAL alignment and returns a simulated alignment object
	// that can be used to generate further alignments
	void menuHelpTestMethod()
	{
		
//		
	}	
}
