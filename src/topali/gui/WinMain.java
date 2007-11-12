// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.WindowEvent;
import java.awt.print.*;
import java.beans.*;
import java.io.File;
import java.net.URL;
import java.util.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.alignment.Alignment;
import pal.tree.Tree;
import sbrn.commons.file.FileUtils;
import topali.analyses.*;
import topali.cluster.LocalJobs;
import topali.data.*;
import topali.gui.dialog.*;
import topali.gui.dialog.jobs.*;
import topali.gui.dialog.jobs.cml.*;
import topali.gui.dialog.jobs.hmm.HMMSettingsDialog;
import topali.gui.dialog.jobs.mt.MTDialog;
import topali.gui.dialog.jobs.tree.CreateTreeDialog;
import topali.gui.dialog.jobs.tree.mrbayes.MrBayesDialog;
import topali.gui.dialog.jobs.tree.phyml.PhymlDialog;
import topali.gui.dialog.jobs.tree.raxml.RaxmlDialog;
import topali.gui.dialog.region.RegionDialog;
import topali.gui.nav.*;
import topali.mod.PrintPreview;
import topali.vamsas.VamsasManager;
import doe.MsgBox;

public class WinMain extends JFrame implements PropertyChangeListener
{
	Logger log = Logger.getLogger(this.getClass());

	// The user's project
	private Project project = new Project();

	// And associated vamsas session (if any)
	public VamsasManager vamsas = null;

	public static VamsasEvents vEvents = null;

	// GUI controls...
	private WinMainMenuBar menubar;

	public WinMainToolBar toolbar;

	private WinMainStatusBar sbar;

	private WinMainTipsPanel tips;

	// More GUI controls...except this time other packages need access to them
	public static NavPanel navPanel;

	public static JSplitPane splits;

	public static JobsPanel jobsPanel;

	public static RegionDialog rDialog;

	public static OverviewDialog ovDialog;

	public static FileDropAdapter dropAdapter;

	public WinMain()
	{
		Color c = new Color(195,206,217);
		UIManager.put("Table.selectionBackground", c);
		// GUI Control initialization
		createControls();

		// Position on screen
		setSize(Prefs.gui_win_width, Prefs.gui_win_height);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		setIconImage(Icons.APP_FRAME.getImage());
		setTitle(Text.Gui.getString("WinMain.gui01"));

		if (Prefs.gui_maximized)
			setExtendedState(Frame.MAXIMIZED_BOTH);

		project.addChangeListener(this);

		Tracker.log("OPEN");
	}

	public Project getProject()
	{
		return project;
	}

	private void createControls()
	{
		dropAdapter = new FileDropAdapter(this);

		menubar = new WinMainMenuBar(this);
		setJMenuBar(menubar);

		tips = new WinMainTipsPanel();
		jobsPanel = new JobsPanel();
		rDialog = new RegionDialog(this);
		ovDialog = new OverviewDialog(this);

		splits = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splits.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
		splits.setOneTouchExpandable(true);
		splits.setDropTarget(new DropTarget(splits, dropAdapter));
		navPanel = new NavPanel(this, tips, splits);
		add(splits);

		toolbar = new WinMainToolBar(navPanel);
		add(toolbar, BorderLayout.NORTH);

		sbar = new WinMainStatusBar(this);
		add(sbar, BorderLayout.SOUTH);
	}

	/*
	 * Called whenever an option has been selected that would close the current
	 * project. The user is therefore queried to find out if they would like to
	 * save it first (or cancel the operation).
	 */
	boolean okToContinue()
	{
		if (project != null)
		{
			if (WinMainMenuBar.aFileSave.isEnabled())
			{
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
				} else if (res == JOptionPane.NO_OPTION)
					return true;
				else if (res == JOptionPane.CANCEL_OPTION
						|| res == JOptionPane.CLOSED_OPTION)
					return false;
			}
		}

		// Cancels all locally running jobs, regardless of project status
		LocalJobs.cancelAll();

		return true;
	}

	public static void repaintDisplay()
	{
		splits.getRightComponent().repaint();
	}

	void menuFileNewProject()
	{
		if (!okToContinue())
			return;

		newProject();

		// Anything done here may need to go in LoadMonitorDialog.java too
	}

	// Opens an existing project
	void menuFileOpenProject(String name)
	{
		if (!okToContinue())
			return;

		LoadMonitorDialog dialog = new LoadMonitorDialog(this, menubar, name);
		if (dialog.getProject() != null)
		{
			project = dialog.getProject();
			project.addChangeListener(this);
		}

		ProjectState.reset();
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
				new MakeNADialog(this);
				break;
			case 2:
				// TODO:
				break;
			case 3:
				new ImportFileSetsDialog(this);
				break;
			case 4:
			{
				ImportDataSetDialog d = new ImportDataSetDialog(this);
				URL url = getClass().getResource("/res/example-alignment.phy");
				try
				{
					File tmpFile = new File(Prefs.tmpDir, "example.txt");
					FileUtils.writeFile(tmpFile, url.openStream());
					d.loadAlignment(tmpFile);
				} catch (Exception e)
				{
					log.warn("Couldn't find example dataset");
					MsgBox.msg("Couldn't load example dataset.\n  " + e, MsgBox.ERR);
				}
			}
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
			// project.getDatasets().add(data);
			project.addDataSet(data);
			//WinMainMenuBar.aFileSave.setEnabled(true);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setDataChanged();
			// navPanel.addAlignmentFolder(data);
		}
	}

	/* Save the current project to disk. */
	void menuFileSave(boolean saveAs)
	{
		setCursor(new Cursor(Cursor.WAIT_CURSOR));

		if (Project.save(project, saveAs))
		{
			//WinMainMenuBar.aFileSave.setEnabled(false);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setFileSaved();
			menubar.updateRecentFileList(project);
		}

		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	void menuFilePrintSetup()
	{
		PrinterDialog.showPageSetupDialog(this);
	}

	void menuFilePrintPreview()
	{
		IPrintable toPrint = navPanel.getSelectedPrintableNode();

		PageFormat format = new PageFormat();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();
		for (Printable p : toPrint.getPrintables())
			book.append(p, format);

		PrintPreview pre = new PrintPreview(book);
		pre.pack();
		pre.setVisible(true);
	}

	void menuFilePrint()
	{
		IPrintable toPrint = navPanel.getSelectedPrintableNode();
		PrinterDialog dlg = new PrinterDialog(toPrint.getPrintables());
		dlg.setVisible(true);
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

		for (SequenceSetNode node : navPanel.getSequenceSetNodes())
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

	void menuAlgnSelectHighlighted()
	{
		AlignmentPanel p = navPanel.getCurrentAlignmentPanel(null);
		p.getListPanel().selectHighlighted(p.mouseHighlight.y,
				p.mouseHighlight.y + p.mouseHighlight.height);
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
			// navPanel.getCurrentAlignmentPanel(data).refreshAndRepaint();
			// And mark the project as modified
			//WinMainMenuBar.aFileSave.setEnabled(true);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setDataChanged();
		}
	}

	void menuAlgnShowPartitionDialog()
	{
		rDialog.setVisible(true);
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

			// navPanel.removeSelectedNode();
			// rDialog.setAlignmentData(null);
			// ovDialog.setAlignmentPanel(null);

			//WinMainMenuBar.aFileSave.setEnabled(true);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setDataChanged();
		}
	}

	public void menuAnlsRunPDM(PDMResult iResult)
	{

		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunPDM(ss) == false)
			return;

		PDMResult result = new PDMSettingsDialog(this, data, iResult)
				.getPDMResult();
		if (result == null)
			return;

		submitJob(data, result);
	}

	public void menuAnlsRunPDM2(PDM2Result iResult)
	{

		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunPDM(ss) == false)
			return;

		PDM2Result result = new PDM2SettingsDialog(this, data, iResult)
				.getPDM2Result();
		if (result == null)
			return;

		// TODO: move this (and hmm/dss) into the SettingsDialog for each job
		// type
		// TODO: increment a counter for PDM2 not PDM(1)
		int runNum = data.getTracker().getPdm2RunCount() + 1;
		data.getTracker().setPdm2RunCount(runNum);
		result.guiName = "PDM2 Result " + runNum;
		result.jobName = "PDM2 Analysis " + runNum + " on " + data.name + " ("
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

		HMMResult result = new HMMSettingsDialog(this, data, iResult)
				.getHMMResult();
		if (result == null)
			return;

		submitJob(data, result);
	}

	public void menuAnlsRunDSS(DSSResult iResult)
	{

		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunDSS(ss) == false)
			return;

		DSSResult result = new DSSSettingsDialog(this, data, iResult)
				.getDSSResult();
		if (result == null)
			return;

		submitJob(data, result);
	}

	public void menuAnlsRunLRT(LRTResult iResult)
	{
		// Perform an initial check on the data and selected sequences
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunLRT(ss) == false)
			return;

		LRTResult result = new LRTSettingsDialog(this, data, iResult)
				.getLRTResult();
		if (result == null)
			return;

		submitJob(data, result);
	}

	public void menuAnlsRunCodeMLSite(CodeMLResult result)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunCodeML(ss) == false)
			return;

		CMLSiteDialog dlg = new CMLSiteDialog(this, data,
				result);
		dlg.setVisible(true);
	}

	public void menuAnlsRunCodeMLBranch(CodeMLResult result)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunCodeML(ss) == false)
			return;

		if(!data.isPartitionCodons()) {
			MsgBox.msg("Selected Partition doesn't contain codons \n(Partition's length is not a multiple of 3)!", MsgBox.ERR);
			return;
		}

		LinkedList<int[]> pos = data.containsPartitionStopCodons();
		if(pos.size()>0) {
			String msg = "Selected sequences contain stop codons: \n";
			for(int[] p : pos) {
				msg += "Seq: "+p[0]+", Pos: "+p[1]+"\n";
			}
			msg += "\nPlease select a partition without stop codons!";
			MsgBox.msg(msg, MsgBox.ERR);
			return;
		}

		//CMLBranchSettingsDialog dlg = new CMLBranchSettingsDialog(this, data,result);
		CMLBranchDialog dlg = new CMLBranchDialog(this, data,result);
		dlg.setVisible(true);
	}
	
	public void menuAnlsRunMT(ModelTestResult result)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();

//		ModelTestDialog dlg = new ModelTestDialog(data, result);
//		dlg.setVisible(true);
		
		MTDialog dlg = new MTDialog(this, data, result);
		dlg.setVisible(true);
		
		ModelTestResult res = dlg.getResult();
		
		if(res!=null)
			submitJob(data, res);
	}

	public void menuAnlsQuickTree() {
		AlignmentData data = navPanel.getCurrentAlignmentData();
		int[] indices = data.getSequenceSet().getSelectedSequences();
		if (indices.length < 3)
		{
			MsgBox.msg("You must have at least 3 sequences selected to create "
					+ "a phylogenetic tree.", MsgBox.ERR);
			return;
		}
		
		TreeResult tr = new TreeResult();
		
		tr.setPartitionStart(data.getActiveRegionS());
		tr.setPartitionEnd(data.getActiveRegionE());
		tr.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		
		if(data.getSequenceSet().getParams().isDNA())
			tr.guiName = "F84+G Tree" + runNum;
		else
			tr.guiName = "WAG+G Tree" + runNum;
		tr.jobName = "Tree Estimation";
		
		Alignment alignment = data.getSequenceSet().getAlignment(indices, data.getActiveRegionS(), data.getActiveRegionE(), true);
		TreeCreator creator = new TreeCreator(alignment, data
				.getSequenceSet().getParams().isDNA(), true, true);
		Tree palTree = creator.getTree();

		if (palTree != null)
		{
			tr.setTreeStr(palTree.toString());
			tr.startTime = creator.getStartTime();
			tr.endTime = creator.getEndTime();
			tr.status = topali.cluster.JobStatus.COMPLETED;
			String model = data.getSequenceSet().getParams().isDNA() ? "F84 (ts/tv=2)"
					: "WAG";
			tr.info = "Sub. Model: "
					+ model
					+ "\nRate Model: Gamma (alpha=4)\nAlgorithm: Neighbour Joining";
			data.addResult(tr);
			ProjectState.setDataChanged();
		}
	}
	
	public void menuAnlsMrBayes(TreeResult result) {
		AlignmentData data = navPanel.getCurrentAlignmentData();
		MrBayesDialog dlg = new MrBayesDialog(this, data, result);
		dlg.setVisible(true);
		
		MBTreeResult res = dlg.getResult();
		
		if(res!=null)
			submitJob(data, res);
	}

	public void menuAnlsPhyml(TreeResult result) {
		AlignmentData data = navPanel.getCurrentAlignmentData();
		PhymlDialog dlg = new PhymlDialog(this, data, result);
		dlg.setVisible(true);
		
		PhymlResult res = dlg.getResult();
		
		if(res!=null)
			submitJob(data, res);
	}
	
	public void menuAnlsRaxml(TreeResult result) {
		AlignmentData data = navPanel.getCurrentAlignmentData();
		RaxmlDialog dlg = new RaxmlDialog(this, data, result);
		dlg.setVisible(true);
		
		RaxmlResult res = dlg.getResult();
		
		if(res!=null)
			submitJob(data, res);
	}

	public void menuAnlsRunCW(CodonWResult res)
	{

		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunCodonW(ss) == false)
			return;

		CodonWDialog cwd = new CodonWDialog(data, res);
		cwd.setVisible(true);
		CodonWResult result = cwd.getResult();

		if (result != null)
			submitJob(data, result);
	}

	public void anlsRunFastML(FastMLResult res)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();

		Vector<Sequence> seqs = data.getSequenceSet().getSequences();
		String[][] seqNames = new String[seqs.size()][2];
		for (int i = 0; i < seqs.size(); i++)
		{
			seqNames[i][0] = seqs.get(i).safeName;
			seqNames[i][1] = seqs.get(i).name;
		}
		res.seqNameMapping = seqNames;

		if (!data.getSequenceSet().isDNA())
		{
			SequenceSetParams p = data.getSequenceSet().getParams();
			if (p.getModel().is("cprev"))
				res.model = FastMLResult.MODEL_AA_CPREV;
			else if (p.getModel().is("day"))
				res.model = FastMLResult.MODEL_AA_DAY;
			else if (p.getModel().is("jtt"))
				res.model = FastMLResult.MODEL_AA_JTT;
			else if (p.getModel().is("mtrev")
					|| p.getModel().is("mtmam"))
				res.model = FastMLResult.MODEL_AA_MTREV;
			else if (p.getModel().is("wag"))
				res.model = FastMLResult.MODEL_AA_WAG;
			else
				res.model = FastMLResult.MODEL_AA_WAG;
		} else
			res.model = FastMLResult.MODEL_DNA_JC;

		res.gamma = data.getSequenceSet().getParams().getModel().isGamma();

		res.alignment.name = data.name + " (+ancestral seq. using FastML)";
		submitJob(data, res);
	}

	public void menuAnlsCreateTree(TreeResult res)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();

		CreateTreeDialog dialog = new CreateTreeDialog(this, data, res);
		TreeResult result = dialog.getTreeResult();
		if (result == null)
			return;

		// Tree being created on-the-fly as part of TOPALi
		if (Prefs.gui_tree_method == 0)
		{
			// TreePane treePane = navPanel.getCurrentTreePane(data, true);
			// TreeCreator creator = new TreeCreator(dialog.getAlignment());
			TreeCreator creator = new TreeCreator(dialog.getAlignment(), data
					.getSequenceSet().getParams().isDNA(), true, true);
			Tree palTree = creator.getTree();

			if (palTree != null)
			{
				result.setTreeStr(palTree.toString());
				result.startTime = creator.getStartTime();
				result.endTime = creator.getEndTime();
				result.status = topali.cluster.JobStatus.COMPLETED;
				String model = data.getSequenceSet().getParams().isDNA() ? "F84 (ts/tv=2)"
						: "WAG";
				result.info = "Sub. Model: "
						+ model
						+ "\nRate Model: Gamma (alpha=4)\nAlgorithm: Neighbour Joining";

				// Add the tree to the project
				// data.getResults().add(result);
				data.addResult(result);

				// treePane.displayTree(data.getSequenceSet(), result);
				//WinMainMenuBar.aFileSave.setEnabled(true);
				//WinMainMenuBar.aVamCommit.setEnabled(true);
				ProjectState.setDataChanged();
			}
		}
		// Tree being created as a cluster/local job
		else
			submitJob(data, result);
	}

	public void submitJob(AlignmentData data, AnalysisResult result)
	{
		// data.getResults().add(result);
		data.addResult(result);

		// jobsPanel.createJob(result, data);
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
		WinMainMenuBar.setMenusForNavChange();
		navPanel.clearSelection();
		rDialog.setAlignmentData(null);
		ovDialog.setAlignmentPanel(null);

		int location = splits.getDividerLocation();
		splits.setRightComponent(jobsPanel);
		splits.setDividerLocation(location);
		WinMainTipsPanel.setDisplayedTips(WinMainTipsPanel.TIPS_JOB);
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
			// navPanel.removeSelectedNode();

			//WinMainMenuBar.aFileSave.setEnabled(true);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setDataChanged();
		}
	}

	void menuAnlsRename()
	{
		// What result is being removed
		AnalysisResult result = navPanel.getCurrentAnalysisResult();

		String newName = (String) JOptionPane.showInputDialog(this,
				"Enter a new name for this result:", "Rename Result",
				JOptionPane.PLAIN_MESSAGE, null, null, result.guiName);

		if (newName != null)
		{
			result.guiName = newName;
			navPanel.nodesChanged();
			//WinMainMenuBar.aFileSave.setEnabled(true);
			//WinMainMenuBar.aVamCommit.setEnabled(true);
			ProjectState.setDataChanged();
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

	void menuVamsasSelectSession()
	{
		// First ensure it's ok to begin a new session
		if (ProjectState.isVamsasSession())
		{
			String msg = "An association with an existing VAMSAS session has "
					+ "already been established. Would you like to drop this\n"
					+ "connection and begin (or connect to) a new session?";
			if (MsgBox.yesno(msg, 1) == JOptionPane.YES_OPTION)
			{
				if (WinMainMenuBar.aVamCommit.isEnabled())
				{
					if (MsgBox
							.yesno(
									"There are uncommitted changes. Commit to VAMSAS session now?",
									1) == JOptionPane.YES_OPTION)
						menuVamsasCommit();
				}
				vamsasDisconnect();
			} else
				return;
		}

		try
		{
			boolean writeEnabled = (project.getDatasets().size()>0);

			vamsas = new VamsasManager(project);

			String[] tmp = vamsas.getAvailableSessions();
			if (tmp != null && tmp.length > 0)
			{
				String[] sessions = new String[tmp.length + 1];
				String newSession = "Create new session...";
				sessions[0] = newSession;
				System.arraycopy(tmp, 0, sessions, 1, tmp.length);
				VamsasSessionDialog dlg = new VamsasSessionDialog(this,
						sessions);
				dlg.setVisible(true);
				String session = dlg.getSelSession();
				if (session == null)
				{
					toolbar.vamsasEnabled(false);
					return;
				} else if (session.equals(newSession)) {
					vamsas.connect(VamsasManager.newSession);
					ProjectState.setVamsasSession(true);
				}
				else
				{
					vamsas.connect(session);
					ProjectState.setVamsasSession(true);
					vamsas.read();
				}
			} else
			{
				vamsas.connect(null);
				ProjectState.setVamsasSession(true);
			}


			if(writeEnabled)
				ProjectState.setDataChanged();

			//vEvents = new VamsasEvents(this, project.getVamsasMapper());
			vEvents = new VamsasEvents(this, vamsas);

			toolbar.vamsasEnabled(true);

		} catch (Exception e)
		{
			vamsas = null;
			ProjectState.setVamsasSession(false);
			MsgBox.msg("Error connecting/creating VAMSAS session.", MsgBox.ERR);
			log.error("Error connecting/creating VAMSAS session.", e);
			toolbar.vamsasEnabled(false);
		}
	}

	void menuVamsasCommit()
	{
		if (vamsas != null)
		{
			ProjectState.setVamsasCommitted();
			try
			{
				vamsas.write();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else
			MsgBox
					.msg(
							"TOPALi has not been associated with a VAMSAS session yet.",
							MsgBox.WAR);
	}

	void menuVamsasExport()
	{
		if (vamsas != null)
		{
			try
			{
				vamsas.write();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else
			MsgBox
					.msg(
							"TOPALi has not been associated with a VAMSAS session yet.",
							MsgBox.WAR);
	}


	void menuVamsasImport()
	{
		if (vamsas != null)
		{
			try
			{
				vamsas.read();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else
			MsgBox
					.msg(
							"TOPALi has not been associated with a VAMSAS session yet.",
							MsgBox.WAR);
	}

	void menuHelpUpdate(boolean useGUI)
	{
		new UpdateChecker(useGUI);
	}

	void menuHelpAbout()
	{
		UpdateChecker.helpAbout();
	}

	void menuHelpTestMethod()
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();

		long s = System.currentTimeMillis();

		String[] codons =
		{ "ATG", "GTG", "TTG", "TAG", "TGA", "TAA" }; // AUG, GUG, UUG

		// stops: UAG is amber, UGA is opal (sometimes also called umber), and
		// UAA is ochre

		for (int i = 0; i < ss.getLength() - 2; i++)
		{
			for (int j = 0; j < codons.length; j++)
			{
				boolean found = true;

				for (Sequence seq : ss.getSequences())
					if (seq.getBuffer().substring(i, i + 3).equals(codons[j]) == false)
						found = false;

				if (found)
				{
					if (j < 2)
						System.out.print("START: ");
					else
						System.out.print("STOP:  ");
					System.out.println(codons[j] + " at " + (i + 1));
				}
			}
		}

		long e = System.currentTimeMillis();
		System.out.println("Time: " + (e - s) + "ms");
		//
	}

	private void newProject()
	{
		project = new Project();
		project.addChangeListener(this);

		setTitle(Text.Gui.getString("WinMain.gui01"));
		menubar.setProjectOpenedState();
		navPanel.clear();

		rDialog.setAlignmentData(null);
		ovDialog.setAlignmentPanel(null);
	}

	public void vamsasDisconnect()
	{
		if (vamsas == null)
			return;
		vamsas.disconnect();
		ProjectState.setVamsasSession(false);
		toolbar.vamsasEnabled(false);
	}

	/**
	 * This is called when the current project is modified (data added/removed)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("alignmentData"))
		{
			// a new dataset was added
			if (evt.getOldValue() == null && evt.getNewValue() != null)
			{
				navPanel.addAlignmentFolder((AlignmentData) evt.getNewValue());
			}
			// a dataset was removed
			if (evt.getOldValue() != null && evt.getNewValue() == null)
			{
				navPanel.removeSelectedNode();
				rDialog.setAlignmentData(null);
				ovDialog.setAlignmentPanel(null);
			}
		}

	}

}
