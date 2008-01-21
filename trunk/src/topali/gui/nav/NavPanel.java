// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.nav;

import static topali.gui.WinMainMenuBar.*;

import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.KeyEvent;
import java.beans.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import org.apache.log4j.Logger;

import scri.commons.gui.MsgBox;

import topali.cluster.JobStatus;
import topali.data.*;
import topali.gui.*;
import topali.gui.dialog.LoadMonitorDialog;
import topali.gui.tree.TreePane;

public class NavPanel extends JPanel implements TreeSelectionListener,
		PropertyChangeListener
{
	 Logger log = Logger.getLogger(this.getClass());

	static JPanel blankPanel;

	private DefaultMutableTreeNode root;

	private DefaultTreeModel model;

	private JTree tree;

	private JScrollPane sp;

	// Reference to the SplitsPane for assigning it panels
	private WinMain winMain;

	private WinMainTipsPanel tips;

	private JSplitPane splits, splitsInternal;

	private GradientPanel titlePanel;

	// We maintain a list of SequenceSetNodes for quick access
	private LinkedList<SequenceSetNode> seqNodes;

	// Set to true during a load/import so that the tree doesn't respond to
	// selection events during this time
	private boolean isLoadingProject = false;

	public NavPanel(WinMain winMain, WinMainTipsPanel tips, JSplitPane splits)
	{
		this.winMain = winMain;
		this.tips = tips;
		this.splits = splits;

		root = new DefaultMutableTreeNode("root");
		model = new DefaultTreeModel(root);

		tree = new JTree(model);
		tree.setRootVisible(false);
		tree.addTreeSelectionListener(this);
		tree.addMouseListener(new MyPopupMenuAdapter());
		tree.setCellRenderer(new NavPanelRenderer());
		tree.getSelectionModel().setSelectionMode(
				TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setDropTarget(new DropTarget(tree, WinMain.dropAdapter));

		sp = new JScrollPane(tree);
		setLayout(new BorderLayout());
		titlePanel = new GradientPanel("");
		titlePanel.setStyle(GradientPanel.OFFICE2003);
		setPanelTitle();
		add(titlePanel, BorderLayout.NORTH);
		add(sp);

		splitsInternal = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitsInternal.setTopComponent(this);
		splitsInternal.setResizeWeight(0.75);
		splitsInternal.setDividerLocation(0.75);
		toggleTipsPanelVisibility();

		splits.setLeftComponent(splitsInternal);
		blankPanel = new JPanel(new BorderLayout());
		blankPanel.setBackground(Color.white);
		blankPanel.add(new JLabel(
				"Import an alignment to begin working with TOPALi",
				SwingConstants.CENTER));
		splits.setRightComponent(blankPanel);
		splits.setDividerLocation(Prefs.gui_splits_loc);

		seqNodes = new LinkedList<SequenceSetNode>();
	}

	public DefaultTreeModel getModel()
	{
		return model;
	}

	public void nodesChanged()
	{
		model.nodeChanged(this.getSelectedNavNode());
	}

	public void toggleTipsPanelVisibility()
	{
		if (Prefs.gui_tips_visible)
		{
			splitsInternal.setBottomComponent(tips);
			splitsInternal.setDividerSize(splits.getDividerSize());
		} else
		{
			splitsInternal.setBottomComponent(null);
			splitsInternal.setDividerSize(0);
		}
	}

	public void clear()
	{
		while (root.getChildCount() > 0)
			model.removeNodeFromParent((MutableTreeNode) root.getChildAt(0));
		titlePanel.setTitle(Text.GuiNav.getString("NavPanel.gui01"));

		int location = splits.getDividerLocation();
		splits.setRightComponent(blankPanel);
		splits.setDividerLocation(location);

		seqNodes = new LinkedList<SequenceSetNode>();

		WinMain.jobsPanel.clear();
	}

	public LinkedList<SequenceSetNode> getSequenceSetNodes()
	{
		return seqNodes;
	}

	private void setPanelTitle()
	{
		String str = Text.GuiNav.getString("NavPanel.gui01");
		if (root.getChildCount() > 0)
			str = Text.GuiNav.getString("NavPanel.gui02");

		titlePanel.setTitle(Text.format(str, root.getChildCount()));
	}

	// Clears and updates the tree to display a recently opened Project file
	public void displayProject(Project project)
	{
		LinkedList<AlignmentData> datasets = project.getDatasets();

		clear();

		isLoadingProject = true;
		for (AlignmentData data : datasets)
		{
			String str = Text.format(Text.GuiDiag
					.getString("LoadMonitorDialog.gui05"), data.name);
			LoadMonitorDialog.setLabel(str);

			addAlignmentFolder(data);
		}

		tree.setSelectionPath(null);
		isLoadingProject = false;

		setProjectSelectionPath(project.getTreePath());
	}

	public void addAlignmentFolder(AlignmentData data)
	{
		// Create the nodes for the dataset's folder and its children
		DefaultMutableTreeNode dataNode = new DefaultMutableTreeNode(
				new DataSetNodeFolder(data));
		model.insertNodeInto(dataNode, root, root.getChildCount());

		// 1) Add the Alignment Node
		DefaultMutableTreeNode node = null;
		if (data.isReferenceList() == false)
			node = addSequenceSetNode(dataNode, data);
		else
			node = addFileListNode(dataNode, data);

		// 2) Add the Trees Node
		if (data.isReferenceList() == false)
			addTreesNode(dataNode, data);
		// 3) Add the Results Node
		addResultsFolder(dataNode, data);

		// 4) register as changelistener
		data.addChangeListener(this);

		// Update the tree with the new node(s)
		tree.setSelectionPath(new TreePath(node.getPath()));
		tree.scrollPathToVisible(new TreePath(node.getPath()));

		setPanelTitle();
	}

	public void removeSelectedNode()
	{
		DefaultMutableTreeNode node = getSelectedNavNode();
		model.removeNodeFromParent(node);

		int location = splits.getDividerLocation();
		splits.setRightComponent(blankPanel);
		splits.setDividerLocation(location);

		setPanelTitle();
	}

	private DefaultMutableTreeNode addSequenceSetNode(
			DefaultMutableTreeNode parent, AlignmentData data)
	{
		// Create the SequenceSetNode
		SequenceSetNode sNode = new SequenceSetNode(data);
		// Track it in the list of nodes
		seqNodes.add(sNode);

		// Add it to the tree
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(sNode);
		parent.add(node);

		return node;
	}

	private DefaultMutableTreeNode addFileListNode(
			DefaultMutableTreeNode parent, AlignmentData data)
	{
		FileListNode fNode = new FileListNode(data);

		// Add it to the tree
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(fNode);
		parent.add(node);

		return node;
	}

	private void addTreesNode(DefaultMutableTreeNode parent, AlignmentData data)
	{
		TreePaneNode tNode = new TreePaneNode(data);

		DefaultMutableTreeNode node = new DefaultMutableTreeNode(tNode);
		tNode.setTreeNode(node);
		parent.add(node);
	}

	private void addResultsFolder(DefaultMutableTreeNode parent,
			AlignmentData data)
	{
		DefaultMutableTreeNode resultsFolder = new DefaultMutableTreeNode(
				new ResultsNodeFolder(data));
		parent.add(resultsFolder);

		for (AnalysisResult result : data.getResults())
		{
			addResultsNode(resultsFolder, data, result);
		}
	}

	// For each results object, we need to decide whether it is a completed
	// object (and hence to be added to the navpanel, or whether it is a running
	// (and to be added to the JobsPanel).
	public void addResultsNode(DefaultMutableTreeNode parent,
			AlignmentData data, AnalysisResult result)
	{
		if (parent == null)
			parent = getResultsFolderNodeForData(data);

		if (result.status == JobStatus.COMPLETED)
		{
			DefaultMutableTreeNode node = new DefaultMutableTreeNode();

			if (result instanceof TreeResult)
			{
				TreePane pane = getCurrentTreePane(data, false);
				pane.displayTree(data.getSequenceSet(), (TreeResult) result);
				return;
			}

			if (result instanceof PDMResult)
				node
						.setUserObject(new PDMResultsNode(data,
								(PDMResult) result));
			if (result instanceof PDM2Result)
				node.setUserObject(new PDM2ResultsNode(data,
						(PDM2Result) result));
			if (result instanceof HMMResult)
				node
						.setUserObject(new HMMResultsNode(data,
								(HMMResult) result));
			if (result instanceof DSSResult)
				node
						.setUserObject(new DSSResultsNode(data,
								(DSSResult) result));
			if (result instanceof LRTResult)
				node
						.setUserObject(new LRTResultsNode(data,
								(LRTResult) result));
			if (result instanceof CodeMLResult)
				node.setUserObject(new CodeMLResultsNode(data,
						(CodeMLResult) result));

			if (result instanceof ModelTestResult)
			{
				node.setUserObject(new MTResultsNode(data, (ModelTestResult) result));
			}

			if (result instanceof CodonWResult)
			{
				node.setUserObject(new CodonWResultsNode(data,
						(CodonWResult) result));
			}

			if(result instanceof FastMLResult) {
				FastMLResult fres = (FastMLResult)result;
				try
				{
					fres.restoreSeqNames();
					winMain.addNewAlignmentData(fres.alignment);
					data.removeResult(result);
				} catch (RuntimeException e)
				{
					log.warn("There was a problem with this FastMLResult", e);
					MsgBox.msg("A problem occured while reconstructing ancestral sequences", MsgBox.ERR);
				}
				return;
			}

			model.insertNodeInto(node, parent, parent.getChildCount());
			return;
		}

		// If it can't be added as a final result, then it needs to be added as
		// a running job back in the JobsPanel
		WinMain.jobsPanel.createJob(result, data);
	}

	public void removeResultsNode(AlignmentData data, AnalysisResult result)
	{
		DefaultMutableTreeNode resultsFolder = getResultsFolderNodeForData(data);
		DefaultMutableTreeNode node = null;
		for (int i = 0; i < resultsFolder.getChildCount(); i++)
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) resultsFolder
					.getChildAt(i);
			ResultsNode rn = (ResultsNode)n.getUserObject();
			if (rn.result.equals(result))
			{
				node = n;
			}
		}

		if (node != null)
		{
			model.removeNodeFromParent(node);

			int location = splits.getDividerLocation();
			splits.setRightComponent(blankPanel);
			splits.setDividerLocation(location);

			setPanelTitle();
		}
	}

	// This can only be called when NavPanel has allowed the aFilePrint action
	// to be enabled, hence this method can ONLY return an IPrintable object
	public IPrintable getSelectedPrintableNode()
	{
		DefaultMutableTreeNode node = getSelectedNavNode();
		return (IPrintable) node.getUserObject();
	}

	// Searches for the SequenceSetNode associated with the given dataset
	// Can be called from any node within the nav tree (because 'data' is known)
	public AlignmentPanel getCurrentAlignmentPanel(AlignmentData data)
	{
		if (data == null)
			data = getCurrentAlignmentData();

		for (SequenceSetNode node : seqNodes)
			if (node.getAlignmentData() == data)
				return node.getAlignmentPanel();

		return null;
	}

	// Can be called at any time, as all nodes can return the Alignment info
	public AlignmentData getCurrentAlignmentData()
	{
		INode node = (INode) getSelectedNavNode().getUserObject();
		return node.getAlignmentData();
	}

	public AnalysisResult getCurrentAnalysisResult()
	{
		INode node = (INode) getSelectedNavNode().getUserObject();
		if (node instanceof ResultsNode)
			return ((ResultsNode) node).getResult();

		return null;
	}

	// Returns the TreePane associated with the given dataset, and optionally
	// makes it visible in the display
	public TreePane getCurrentTreePane(AlignmentData data, boolean makeVisible)
	{
		DefaultMutableTreeNode node = getCurrentTreePaneNode(data);

		if (makeVisible)
		{
			tree.setSelectionPath(new TreePath(node.getPath()));
			tree.scrollPathToVisible(new TreePath(node.getPath()));
		}

		return ((TreePaneNode) node.getUserObject()).getTreePane();
	}

	// Searches for the TreePaneNode associated with the given dataset
	private DefaultMutableTreeNode getCurrentTreePaneNode(AlignmentData data)
	{
		Enumeration<?> e = root.breadthFirstEnumeration();
		while (e.hasMoreElements())
		{
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
			if (INode.isTreePaneNodeForData(n, data))
				return n;
		}

		return null;
	}

	// Returns the currently selected NAVIGATION tree node, (not Phylo tree)
	private DefaultMutableTreeNode getSelectedNavNode()
	{
		return (DefaultMutableTreeNode) tree.getSelectionModel()
				.getSelectionPath().getLastPathComponent();
	}

	public void valueChanged(TreeSelectionEvent e)
	{
		// Don't respond to events when the tree is being populated as part of
		// a project load/import
		if (isLoadingProject)
			return;

		WinMainMenuBar.setMenusForNavChange();

		DefaultMutableTreeNode n = (DefaultMutableTreeNode) tree
				.getLastSelectedPathComponent();

		getProjectSelectionPath(n);

		if (n == null)
			return;

		Object nodeInfo = n.getUserObject();
		int location = splits.getDividerLocation();

		if (nodeInfo instanceof INode)
		{
			INode iNode = (INode) nodeInfo;
			AlignmentData data = iNode.getAlignmentData();

			// Display it
			splits.setRightComponent(iNode.getPanel());
			iNode.setMenus();

			// And update everything based upon it
			WinMain.rDialog.setAlignmentData(data);
			WinMain.ovDialog.setAlignmentPanel(getCurrentAlignmentPanel(data));
			WinMainTipsPanel.setDisplayedTips(iNode.getTipsKey());
		} else
			splits.setRightComponent(blankPanel);

		// Can the user print now?
		if (nodeInfo instanceof ResultsNode
				&& ((ResultsNode) nodeInfo).isPrintable())
		{
			WinMainMenuBar.aFilePrint.setEnabled(true);
			WinMainMenuBar.aFilePrintPreview.setEnabled(true);
		} else
		{
			WinMainMenuBar.aFilePrint.setEnabled(false);
			WinMainMenuBar.aFilePrintPreview.setEnabled(false);
		}

		splits.setDividerLocation(location);
	}

	// Searches the tree to find the ResultsFolderNode that belongs to the
	// given dataset
	private DefaultMutableTreeNode getResultsFolderNodeForData(AlignmentData d1)
	{
		DefaultMutableTreeNode node = null;

		// Searches each primary child of the root, until a DataSetNodeFolder
		// object is found whose dataset matches. Child *2* is then returned
		// (as this is the Results folder)
		for (int i = 0; i < root.getChildCount(); i++)
		{
			node = (DefaultMutableTreeNode) root.getChildAt(i);

			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof INode)
			{
				AlignmentData d2 = ((INode) nodeInfo).getAlignmentData();
				if (d1 == d2)
					return (DefaultMutableTreeNode) node.getChildAt(2);
			}
		}

		return null;
	}

	public void clearSelection()
	{
		tree.setSelectionPath(null);
	}

	/* Saves the user's current postition in the navigation tree */
	private void getProjectSelectionPath(DefaultMutableTreeNode node)
	{
		int[] treePath = null;

		if (node != null)
		{
			TreeNode[] path = node.getPath();
			treePath = new int[path.length - 1];

			// Stores the index of each node on the path
			for (int i = 0; i < path.length; i++)
				if (i > 0)
					treePath[i - 1] = path[i].getParent().getIndex(path[i]);
		}

		winMain.getProject().setTreePath(treePath);
	}

	/* Restores the user's selection to the navigation tree */
	private void setProjectSelectionPath(int[] treePath)
	{
		if (treePath == null)
			return;

		try
		{
			TreePath path = new TreePath(root);

			TreeNode current = root;
			for (int i = 0; i < treePath.length; i++)
			{
				current = current.getChildAt(treePath[i]);
				path = path.pathByAddingChild(current);
			}

			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);
		}
		catch (Exception e) {}
	}

	public void displayHelp()
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
				.getLastSelectedPathComponent();

		String key = "intro";
		if (node != null)
		{
			Object obj = node.getUserObject();
			if (obj != null && obj instanceof INode)
				key = ((INode) obj).getHelpKey();
		}

		JButton b = new JButton();
		TOPALiHelp.enableHelpOnButton(b, key);
		b.doClick();
	}

	class MyPopupMenuAdapter extends PopupMenuAdapter
	{
		@Override
		protected void handlePopup(int x, int y)
		{

			// Create the menu
			p = new JPopupMenu();

			// And the items we always want in it...
			add(aFileImportDataSet, Icons.IMPORT16, KeyEvent.VK_I, 0, 0, 0,
					false);

			// Then check to see exactly what was clicked on
			TreePath path = tree.getPathForLocation(x, y);

			if (path != null)
			{
				tree.setSelectionPath(path);

				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path
						.getLastPathComponent();
				Object obj = node.getUserObject();

				// For "alignment" nodes
				if (obj instanceof SequenceSetNode)
				{
					add(aAlgnDisplaySummary, Icons.INFO16, KeyEvent.VK_D, 0, 0,
							0, true);
				}

				// For "dataset" nodes
				if (obj instanceof DataSetNodeFolder)
				{
					add(aAlgnRemove, Icons.REMOVE16, KeyEvent.VK_R, 0, 0, 0,
							true);
				}

				if (obj instanceof ResultsNode)
				{
					add(aAnlsRename, KeyEvent.VK_N, 0, 0, 0, true);
					add(aAnlsRemove, Icons.REMOVE16, KeyEvent.VK_R, 0, 0, 0,
							false);
				}
			}
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("result"))
		{
			//a new AnalysisResult has been added:
			if(evt.getNewValue()!=null && evt.getOldValue()==null)
			{
				addResultsNode(null, (AlignmentData) evt.getSource(),
						(AnalysisResult) evt.getNewValue());
			}

			// an AnalysisResult has been removed:
			if (evt.getNewValue() == null && evt.getOldValue()!=null)
			{
				removeResultsNode((AlignmentData) evt.getSource(),
						(AnalysisResult) evt.getOldValue());
			}
		}

	}

}