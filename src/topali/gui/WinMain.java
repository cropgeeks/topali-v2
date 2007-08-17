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
import java.util.Vector;

import javax.swing.*;

import org.apache.log4j.Logger;

import pal.tree.Tree;
import topali.analyses.*;
import topali.cluster.LocalJobs;
import topali.data.*;
import topali.gui.dialog.*;
import topali.gui.dialog.hmm.HMMSettingsDialog;
import topali.gui.dialog.region.RegionDialog;
import topali.gui.nav.*;
import topali.mod.PrintPreview;
import topali.vamsas.VamsasManager;
import topali.var.Utils;
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

		project.addChangeListener(this);
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

		project = new Project();
		project.addChangeListener(this);

		setTitle(Text.Gui.getString("WinMain.gui01"));
		menubar.setProjectOpenedState();
		navPanel.clear();

		rDialog.setAlignmentData(null);
		ovDialog.setAlignmentPanel(null);

		// Anything done here may need to go in LoadMonitorDialog.java too
	}

	// Opens an existing project
	void menuFileOpenProject(String name)
	{
		if (!okToContinue())
			return;

		LoadMonitorDialog dialog = new LoadMonitorDialog(this, menubar, name);
		if (dialog.getProject() != null) {
			project = dialog.getProject();
			project.addChangeListener(this);
		}
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
			case 4: {
				ImportDataSetDialog d = new ImportDataSetDialog(this);
				URL url = this.getClass().getResource("/res/example-alignment.phy");
				System.out.println(url);
				try
				{
					d.loadAlignment(new File(url.toURI()));
				} catch (Exception e)
				{
					log.warn("Couldn't find example dataset");
					MsgBox.msg("Couldn't load example dataset", MsgBox.ERR);
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
			//project.getDatasets().add(data);
			project.addDataSet(data);
			WinMainMenuBar.aFileSave.setEnabled(true);
			//navPanel.addAlignmentFolder(data);
		}
	}

	/* Save the current project to disk. */
	void menuFileSave(boolean saveAs)
	{
		setCursor(new Cursor(Cursor.WAIT_CURSOR));

		if (Project.save(project, saveAs))
		{
			WinMainMenuBar.aFileSave.setEnabled(false);
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
			//navPanel.getCurrentAlignmentPanel(data).refreshAndRepaint();
			// And mark the project as modified
			WinMainMenuBar.aFileSave.setEnabled(true);
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

//			navPanel.removeSelectedNode();
//			rDialog.setAlignmentData(null);
//			ovDialog.setAlignmentPanel(null);

			WinMainMenuBar.aFileSave.setEnabled(true);
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
		
		CMLSiteSettingsDialog dlg = new CMLSiteSettingsDialog(this, data,
				result);
		dlg.setVisible(true);
	}

	public void menuAnlsRunCodeMLBranch(CodeMLResult result)
	{
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunCodeML(ss) == false)
			return;
		
		CMLBranchSettingsDialog dlg = new CMLBranchSettingsDialog(this, data,
				result);
		dlg.setVisible(true);
	}
	
	public void menuAnlsRunMG(boolean remote)
	{

		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		
		MGResult result = new MGResult();
		result.mgPath = Utils.getLocalPath() + "modelgenerator.jar";
		result.isRemote = remote;
		result.javaPath = "java";
		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		
		int runNum = data.getTracker().getMgRunCount() + 1;
		data.getTracker().setMgRunCount(runNum);
		result.guiName = "Modelgenerator Result " + runNum;
		result.jobName = "Modelgenerator on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		submitJob(data, result);
	}
	
	public void menuAnlsRunCW(CodonWResult res) {
		
		AlignmentData data = navPanel.getCurrentAlignmentData();
		SequenceSet ss = data.getSequenceSet();
		if (SequenceSetUtils.canRunCodonW(ss) == false)
			return;
		
		CodonWDialog cwd = new CodonWDialog(data, res);
		cwd.setVisible(true);
		CodonWResult result = cwd.getResult();
		
		if(result!=null) 
			submitJob(data, result);
	}
	
	public void anlsRunFastML(FastMLResult res) {
		AlignmentData data = navPanel.getCurrentAlignmentData();
		
		Vector<Sequence> seqs = data.getSequenceSet().getSequences();
		String[][] seqNames = new String[seqs.size()][2];
		for(int i=0; i<seqs.size(); i++) {
			seqNames[i][0] = seqs.get(i).safeName;
			seqNames[i][1] = seqs.get(i).name;
		}
		res.seqNameMapping = seqNames;
		
		if(!data.getSequenceSet().isDNA()) {
		SequenceSetParams p = data.getSequenceSet().getParams();
		if(p.getModel()==SequenceSetParams.MODEL_AA_CPREV)
			res.model = FastMLResult.MODEL_AA_CPREV;
		else if(p.getModel()==SequenceSetParams.MODEL_AA_DAYHOFF)
			res.model = FastMLResult.MODEL_AA_DAY;
		else if(p.getModel()==SequenceSetParams.MODEL_AA_JONES)
			res.model = FastMLResult.MODEL_AA_JTT;
		else if(p.getModel()==SequenceSetParams.MODEL_AA_MTREV || p.getModel()==SequenceSetParams.MODEL_AA_MTMAM)
			res.model = FastMLResult.MODEL_AA_MTREV;
		else if(p.getModel()==SequenceSetParams.MODEL_AA_WAG)
			res.model = FastMLResult.MODEL_AA_WAG;
		else
			res.model = FastMLResult.MODEL_AA_WAG;
		}
		else
			res.model = FastMLResult.MODEL_DNA_JC;
		
		res.gamma = data.getSequenceSet().getParams().isModelGamma();
		
		res.alignment.name = data.name+" (+ancestral seq. using FastML)";
		submitJob(data, res);
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
			//TreePane treePane = navPanel.getCurrentTreePane(data, true);
			//TreeCreator creator = new TreeCreator(dialog.getAlignment());
			TreeCreator creator = new TreeCreator(dialog.getAlignment(), data.getSequenceSet().getParams().isDNA());
			Tree palTree = creator.getTree(true);

			if (palTree != null)
			{
				result.setTreeStr(palTree.toString());
				result.startTime = creator.getStartTime();
				result.endTime = creator.getEndTime();
				result.status = topali.cluster.JobStatus.COMPLETED;
				String model = data.getSequenceSet().getParams().isDNA() ? "F84 (ts/tv=2)" : "WAG";
				result.info = "Sub. Model: "+model+"\nRate Model: Gamma (alpha=4)\nAlgorithm: Neighbour Joining";
			
				// Add the tree to the project
				//data.getResults().add(result);
				data.addResult(result);

				//treePane.displayTree(data.getSequenceSet(), result);
				WinMainMenuBar.aFileSave.setEnabled(true);
			}
		}
		// Tree being created as a cluster/local job
		else
			submitJob(data, result);
	}

	public void submitJob(AlignmentData data, AnalysisResult result)
	{
		//data.getResults().add(result);
		data.addResult(result);

		//jobsPanel.createJob(result, data);
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
			//navPanel.removeSelectedNode();

			WinMainMenuBar.aFileSave.setEnabled(true);
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
			WinMainMenuBar.aFileSave.setEnabled(true);
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

	void menuVamsasSelectSession() {
//		 First ensure it's ok to begin a new session
		if (vamsas != null)
		{
			String msg = "An association with an existing VAMSAS session has "
					+ "already been established. Would you like to drop this\n"
					+ "connection and begin (or connect to) a new session?";
			if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
				return;
		}
		
		vamsas = new VamsasManager();
		vEvents = new VamsasEvents(this);
		
		try
		{
			vamsas.connect(project, null);
		} catch (Exception e)
		{
			MsgBox.msg("Error connecting/creating VAMSAS session.", MsgBox.ERR);
		}
	}
	
//	void menuVamsasSelectSession()
//	{
//		// First ensure it's ok to begin a new session
//		if (vamsas != null)
//		{
//			String msg = "An association with an existing VAMSAS session has "
//					+ "already been established. Would you like to drop this\n"
//					+ "connection and begin (or connect to) a new session?";
//			if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
//				return;
//		}
//		
//		vamsas = new VamsasManager();
//		vEvents = new VamsasEvents(this);
//		
//		if (vamsas.initializeVamsas(project))
//		{
//			MsgBox.msg("TOPALi initiated a successfull connection to a VAMSAS "
//				+ "session.", MsgBox.INF);
//		}
//
//		// Create a dialog to select the session file
///*		JFileChooser fc = new JFileChooser();
//		fc.setCurrentDirectory(new File(Prefs.gui_dir));
//		fc.setSelectedFile(new File("vamsas-session.zip"));
//		fc.setDialogTitle("Select (or Create) VAMSAS Session File");
//
//		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
//		{
//			File file = fc.getSelectedFile();
//			Prefs.gui_dir = "" + fc.getCurrentDirectory();
//
//			vClient = new VamsasClient(file);
//		}
//*/
//	}

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
		}
		else
			MsgBox.msg("TOPALi has not been associated with a VAMSAS session yet.",	MsgBox.WAR);
	}

//	boolean vamsasUpdate() {
//		if(vamsas == null) {
//			vamsas = new VamsasManager();
//			vEvents = new VamsasEvents(this);
//			
//			if (!vamsas.initializeVamsas(project))
//			{
//				MsgBox.msg("Connection to a VAMSAS session could not be established", MsgBox.ERR);
//				return false;
//			}
//			
//			try
//			{
//				vamsas.readFromDocument();
//			} catch (Exception e)
//			{
//				MsgBox.msg("Could not retrieve data from VAMSAS session", MsgBox.ERR);
//				return false;
//			}
//			
//			return true;
//		}
//		else {
//			try
//			{
//				vamsas.writeToDocument();
//				return true;
//			} catch (Exception e)
//			{
//				MsgBox.msg("There was an error while synchronizing VAMSAS session", MsgBox.ERR);
//				e.printStackTrace();
//				return false;
//			}
//		}
//	}
	
	void menuVamsasImport()
	{
		if(vamsas != null) {
//			
//			boolean emptyProject = (project.getDatasets().size()==0);
//			
			try
			{
				vamsas.read();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
//			
//			if(emptyProject) {
//				for(AlignmentData data : project.getDatasets())
//					navPanel.addAlignmentFolder(data);
//			}
		}
		else
			MsgBox.msg("TOPALi has not been associated with a VAMSAS session yet.",	MsgBox.WAR);
		
/*		if (vClient == null)
		{
			MsgBox
					.msg(
							"TOPALi has not been associated with a VAMSAS session yet.",
							MsgBox.WAR);
			return;
		}

		AlignmentData[] datasets = vClient.readFromFile();

		// topali.vamsas.FileHandler fh = new topali.vamsas.FileHandler();

		// AlignmentData[] datasets = fh.loadVamsas();

		if (datasets != null)
		{
			for (AlignmentData data1 : datasets)
				addNewAlignmentData(data1);

			if (datasets.length == 1)
				MsgBox.msg("1 alignment has been imported into TOPALi.",
						MsgBox.INF);
			else
				MsgBox.msg(datasets.length
						+ " alignments have been imported into TOPALi.",
						MsgBox.INF);
		}
*/
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

	/**
	 * This is called when the current project is modified (data added/removed)
	 */
	public void propertyChange(PropertyChangeEvent evt)
	{
		if(evt.getPropertyName().equals("alignmentData")) {
			//a new dataset was added
			if(evt.getOldValue()==null && evt.getNewValue()!=null) {
				navPanel.addAlignmentFolder((AlignmentData)evt.getNewValue());
			}
			//a dataset was removed
			if(evt.getOldValue()!=null && evt.getNewValue()==null) {
				navPanel.removeSelectedNode();
				rDialog.setAlignmentData(null);
				ovDialog.setAlignmentPanel(null);
			}
		}
		
		
	}
	
	
}
