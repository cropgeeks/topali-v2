// (C) 2003-2004 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import doe.*;

import topali.data.*;
import topali.gui.*;

public class PartitionDialog extends JDialog
	implements ActionListener, ListSelectionListener
{
	private WinMain winMain;
	
	// The current AlignmentData object that the partitions are taken from
	private AlignmentData data;
	private PartitionAnnotations pAnnotations;
	
	// Tree preview panel
	private TreePreviewPanel treePanel = new TreePreviewPanel(winMain, this);
	
	// Splits the partition list from the preview pane
	private JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	
	private JList regionList;
	private DefaultListModel regionModel;
	private JButton bExport, bTree, bPartition;
	private JButton bNew, bEdit, bRemove, bRemoveAll;
	private JButton bHelp, bClose;	
	
	public PartitionDialog(WinMain winMain)
	{
		super(winMain, "", false);
		this.winMain = winMain;
		
		createControls();
		
		pack();
		setResizable(false);
		if (Prefs.gui_pdialog_x == -1)
			setLocationRelativeTo(winMain);
		else
			setLocation(Prefs.gui_pdialog_x, Prefs.gui_pdialog_y);
	}
	
	// Called when TOPALi is closing down
	public void exit()
	{
		Prefs.gui_pdialog_x  = getLocation().x;
		Prefs.gui_pdialog_y  = getLocation().y;
		Prefs.gui_pdialog_splitter = splitter.getDividerLocation();
	}
	
	private void createControls()
	{
		// Create buttons
		bRemoveAll = new JButton("Remove All");
		bRemoveAll.setMnemonic(KeyEvent.VK_M);
		bRemoveAll.addActionListener(this);
		bEdit = new JButton("Edit...");
		bEdit.setMnemonic(KeyEvent.VK_E);
		bEdit.addActionListener(this);
		bRemove = new JButton("Remove");
		bRemove.setMnemonic(KeyEvent.VK_R);
		bRemove.addActionListener(this);
		bClose = new JButton("Close");
		bClose.addActionListener(this);
		bExport = new JButton("Export...");
		bExport.addActionListener(this);
//		bExport.setText("Export...");
		bExport.setMnemonic(KeyEvent.VK_X);
		bTree = new JButton(WinMainMenuBar.aAnlsCreateTree);
		bTree.setText("Create Tree...");
		bTree.setMnemonic(KeyEvent.VK_C);
		bNew = new JButton("New...");
		bNew.setMnemonic(KeyEvent.VK_N);
		bNew.addActionListener(this);
		bPartition = new JButton(WinMainMenuBar.aAnlsPartition);
		bPartition.setText("Auto Partition...");
		bPartition.setMnemonic(KeyEvent.VK_A);
		bHelp = TOPALiHelp.getHelpButton("partitions_dialog");
		
		// Tooltips
		bRemoveAll.setToolTipText("Remove all partitions from list");
		bEdit.setToolTipText("Edit selected partition");
		bRemove.setToolTipText("Remove selected partition from list");
		bExport.setToolTipText("Export selected partition to disk");
		bTree.setToolTipText("Estimate phylogenetic tree from partition");
		bNew.setToolTipText("Manually specify a new partition");
		bPartition.setToolTipText("Automatically create partitions based on analysis "
			+ "results");
		
		
		JPanel p1 = new JPanel(new GridLayout(12, 1, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bNew);
		p1.add(bEdit);
		p1.add(bPartition);
		p1.add(bRemove);
		p1.add(bRemoveAll);
		p1.add(new JLabel());
		p1.add(bExport);
		p1.add(bTree);		
		// Blank spaces
		p1.add(new JLabel()); p1.add(new JLabel());		
		p1.add(bHelp);
		p1.add(bClose);
		
		JPanel p1A = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		p1A.add(p1);
		
		regionModel = new DefaultListModel();
		regionList = new JList(regionModel);
		regionList.addListSelectionListener(this);
		regionList.setToolTipText("Lists all currently defined partitions");
		JScrollPane sp = new JScrollPane(regionList);
//		sp.setPreferredSize(new Dimension(200, 150));
		sp.setBorder(BorderFactory.createLineBorder(Icons.blueBorder));
		JPanel p2a = new JPanel(new BorderLayout());
		p2a.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(2, 10, 10, 10),
				BorderFactory.createTitledBorder("Partition list:")),
			BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		p2a.add(sp);
		
		
		addListeners();
		
		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.add(treePanel);
		p2.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(2, 10, 10, 10),
				BorderFactory.createTitledBorder("Phylogenetic tree preview "
					+ "(approx JC/NJ):")),
			BorderFactory.createEmptyBorder(0, 5, 5, 5)));
		
		splitter.setDividerSize(5);
		splitter.setTopComponent(p2a);
		splitter.setBottomComponent(p2);
		if (Prefs.gui_pdialog_splitter == -1)
			splitter.setDividerLocation(0.5);
		else
			splitter.setDividerLocation(Prefs.gui_pdialog_splitter);
		
		setLayout(new BorderLayout());		
		add(splitter, BorderLayout.CENTER);
		add(p1A, BorderLayout.EAST);
		
		checkList();
	}
	
	private void addListeners()
	{
		// A double-click should call editPartition()
		regionList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
				{
					int index = regionList.getSelectedIndex();
					if (index != -1)
						editPartition();
				}
			}
		});
		
		// As should pressing ENTER. DELETE should do the obvious too
		regionList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DELETE)
				{
					if (regionList.getSelectedIndices().length > 0)
						bRemove.doClick();
				}
				else if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					int index = regionList.getSelectedIndex();
					if (index != -1)
						editPartition();
				}
			}
		});
		
		Utils.addCloseHandler(this, bClose);
	}
	
	// Confirms the status of the list and adjusts the enabled/disabled states
	// of the buttons. Also calls updateTreePreview() which will decide if the
	// tree needs repainting too
	private void checkList()
	{		
		bNew.setEnabled(data != null);
		
		if (regionList.getSelectedIndices().length == 0)
			bRemove.setEnabled(false);
		else
			bRemove.setEnabled(true);
		
		if (regionList.getSelectedIndices().length == 1)
		{
			bEdit.setEnabled(true);
			bTree.setEnabled(true);
		}
		else
		{
			bEdit.setEnabled(false);			
			bTree.setEnabled(false);
		}
	
		if (regionModel.size() == 0)
		{
			bExport.setEnabled(false);
			bRemoveAll.setEnabled(false);
		}
		else
		{
			bExport.setEnabled(true);
			bRemoveAll.setEnabled(true);
		}
	}
	
	// Updates the dialog whenever a different alignment has been selected
	public void setAlignmentData(AlignmentData newData)
	{
		// If the same alignment has been selected, do nothing
		if (newData == data) return;
		
		data = newData;
		treePanel.setAlignmentData(newData);
		regionModel.clear();
		
		if (data == null)
		{
			setTitle("Current Partitions");
			checkList();
			return;
		}
		
		// Add the partitions to the list
		pAnnotations = data.getTopaliAnnotations().getPartitionAnnotations();
		for (RegionAnnotations.Region r: pAnnotations)
			regionModel.addElement(r);

				
		// Reselect the last selected index in the list
//		if (pInfo.selected >= 0 && regionModel.size() > 0)
//			regionList.setSelectedIndex(pInfo.selected);
				
		setTitle("Current Partitions - " + data.name);
		checkList();
	}
	
	public void refreshPartitionList()
	{
		regionModel.clear();
		for (RegionAnnotations.Region r: pAnnotations)
			regionModel.addElement(r);
		
		checkList();
		setVisible(true);
	}
	
	public void addCurrentPartition()
	{
		int start = pAnnotations.getCurrentStart();
		int end = pAnnotations.getCurrentEnd();
		
		addPartition(start, end);
	}
			
	// Adds a new partition to the current list
	public boolean addPartition(int start, int end)
	{
		// Check it doesn't already exist
		RegionAnnotations.Region r = new RegionAnnotations.Region(start, end);

		if (pAnnotations.contains(r))
		{
			MsgBox.msg("The partition from nucleotide " + start + " to "
				+ "nucleotide " + end + " already exists.", MsgBox.ERR);
			return false;
		}
	
		// Add it
		int position = pAnnotations.addRegion(r);
		regionModel.add(position, r);
		
		// Deselect all...
		regionList.setSelectedIndices(new int[] { position });
		// ...then select the new element
		regionList.ensureIndexIsVisible(regionList.getSelectedIndex());
				
		// Ensure the dialog is visible
		setVisible(true);
		return true;
	}
	
	// Decides if the tree preview needs repainting.
	public void updateTreePreview(boolean repaintAll)
	{
		// Can only create a tree if a single item is selected in the list
		if (regionList.getSelectedIndices().length != 1)
		{
			treePanel.clearTree();
			return;
		}
	
		// Get the partition details
		RegionAnnotations.Region r =
			(RegionAnnotations.Region) regionList.getSelectedValue();
		pAnnotations.setCurrentPartition(r.getS(), r.getE());
//		pInfo.selected = regionList.getSelectedIndex();
		
		if (repaintAll)
			winMain.repaintDisplay();
			
		WinMainMenuBar.aFileSave.setEnabled(true);
		// Create the tree
		treePanel.createTree(r.getS(), r.getE());
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		checkList();
		
		if (e.getValueIsAdjusting() == false)
			updateTreePreview(true);			
	}
	
	// Respond to button clicks...
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClose)
			setVisible(false);
			
		else if (e.getSource() == bRemoveAll)
		{
			regionModel.clear();
			pAnnotations.deleteAll();
			pAnnotations.resetCurrentPartition();
			
			// Rehighlight everything			
			winMain.repaintDisplay();
		}
		
		else if (e.getSource() == bRemove)
		{
			int[] indices = regionList.getSelectedIndices();
			for (int i = indices.length-1; i >= 0; i--)
			{
				pAnnotations.remove(indices[i]);
				regionModel.remove(indices[i]);
			}
			
			// Rehighlight everything
			pAnnotations.resetCurrentPartition();
			winMain.repaintDisplay();
		}
		
		else if (e.getSource() == bEdit)
			editPartition();
					
		else if (e.getSource() == bNew)
			newPartition();
		
		// Call the export dialog (also telling it which partitions are selected)
		else if (e.getSource() == bExport)
			doExport();
	}
	
	public void doExport()
	{
		new ExportDialog(winMain, data, regionList.getSelectedIndices());
	}
	
	// Uses the edit dialog to create a new partition
	private void newPartition()
	{
		int length = data.getSequenceSet().getLength();
		
		new EditDialog(new RegionAnnotations.Region(1, length), this);
	}
	
	// Uses the edit dialog to edit an existing partition
	private void editPartition()
	{
		RegionAnnotations.Region r =
			(RegionAnnotations.Region) regionList.getSelectedValue();

		new EditDialog(r, this);
	}
	
	
	// Defines a dialog that can be used to edit the nucleotide range.
	class EditDialog extends JDialog
		implements ActionListener, ChangeListener
	{
		private RegionAnnotations.Region r;
		
		private JSpinner spinStart, spinEnd;
		private SpinnerNumberModel modelStart, modelEnd;
		private JButton bOK, bCancel;
		
		EditDialog(RegionAnnotations.Region r, PartitionDialog parent)
		{
			super(parent, "Edit Partition", true);
			this.r = r;
			
			// Create the spinner controls
			int seqLength = data.getSequenceSet().getLength();
			modelStart = new SpinnerNumberModel(r.getS(), 1, r.getE(), 1);
			modelEnd = new SpinnerNumberModel(r.getE(), r.getS(), seqLength, 1);
			
			spinStart = new JSpinner(modelStart);
			spinStart.addChangeListener(this);
			spinEnd = new JSpinner(modelEnd);
			spinEnd.addChangeListener(this);
			
			// Create button controls
			bOK = new JButton("OK");
			bOK.addActionListener(this);
			bCancel = new JButton("Cancel");
			bCancel.addActionListener(this);
			
			JLabel sLabel = new JLabel("Starting nucleotide: ");
			sLabel.setDisplayedMnemonic(KeyEvent.VK_S);
			sLabel.setLabelFor(spinStart);
			JLabel eLabel = new JLabel("Ending nucleotide: ");
			eLabel.setDisplayedMnemonic(KeyEvent.VK_E);
			eLabel.setLabelFor(spinEnd);
						
			
			JPanel p1 = new JPanel(new GridLayout(2, 2, 5, 5));
			p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			p1.add(sLabel);
			p1.add(spinStart);
			p1.add(eLabel);
			p1.add(spinEnd);
			
			JPanel p2 = new JPanel(new GridLayout(1, 2, 5, 5));
			p2.add(bOK);
			p2.add(bCancel);
			
			JPanel p3 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			p3.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
			p3.add(p2);
			
			getContentPane().setLayout(new BorderLayout(5, 5));
			getContentPane().add(p1, BorderLayout.CENTER);
			getContentPane().add(p3, BorderLayout.SOUTH);
			
			Utils.addCloseHandler(this, bCancel);
			
			pack();
			setLocationRelativeTo(parent);
			setResizable(false);
			setVisible(true);
		}
				
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() == bOK)
			{
				// We first attempt to remove the partition (so that an EDIT
				// is OK but an ADD is not (on the same data)... 
				if (regionModel.removeElement(r))
					pAnnotations.remove(r.getS(), r.getE());

				r.setS(modelStart.getNumber().intValue());
				r.setE(modelEnd.getNumber().intValue());
					
				// ...so it can be readded (or added) at the correct sorted pos
				if (!addPartition(r.getS(), r.getE()))
					return;
				
				WinMainMenuBar.aFileSave.setEnabled(true);
				setVisible(false);
			}
			
			else if (e.getSource() == bCancel)
				setVisible(false);
		}
		
		public void stateChanged(ChangeEvent e)
		{
			// Starting nucleotide spinner
			if (e.getSource() == spinStart)
				modelEnd.setMinimum(modelStart.getNumber().intValue());
			
			// Ending nucleotide spinner
			else
				modelStart.setMaximum(modelEnd.getNumber().intValue());
		}
	}
}
