package topali.gui.dialog.region;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import topali.data.AlignmentAnnotations;
import topali.data.AlignmentData;
import topali.data.CDSAnnotations;
import topali.data.PartitionAnnotations;
import topali.data.RegionAnnotations;
import topali.data.RegionAnnotations.Region;
import topali.gui.Icons;
import topali.gui.Prefs;
import topali.gui.TOPALiHelp;
import topali.gui.Utils;
import topali.gui.WinMain;
import topali.gui.WinMainMenuBar;
import topali.gui.dialog.ExportDialog;
import doe.MsgBox;

public class RegionDialog extends JDialog implements ActionListener, ListSelectionListener, ItemListener {

	//Holds all currently supported RegionAnnotation classes
	private final Class[] supportedAnnotations = new Class[] {PartitionAnnotations.class, CDSAnnotations.class};
	
	private WinMain winMain;
	private AlignmentData alignData;
	private RegionAnnotations annotations;
	
	//GUI stuff
	private TreePreviewPanel treePanel = new TreePreviewPanel(winMain, this);
	private JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	private JList regionList;
	private DefaultListModel regionModel;
	private JButton bExport, bTree, bPartition;
	private JButton bNew, bEdit, bRemove, bRemoveAll;
	private JButton bHelp, bClose;	
	private JComboBox annoType;
	
	public RegionDialog() {
		buildGui();
	}
	
	public RegionDialog(WinMain winMain) {
		this.winMain = winMain;
		buildGui();
		
		pack();
		setResizable(false);
		if (Prefs.gui_pdialog_x == -1)
			setLocationRelativeTo(winMain);
		else
			setLocation(Prefs.gui_pdialog_x, Prefs.gui_pdialog_y);
	}	
	
	public void setAlignmentData(AlignmentData data) {
		this.alignData = data;
		treePanel.setAlignmentData(data);
		if(data!=null) {
			setTitle("Current Regions - "+data.name);
			annoType.setSelectedItem(supportedAnnotations[0]);
			annotations = (RegionAnnotations)data.getTopaliAnnotations().getAnnotations(supportedAnnotations[0]);
		}
		else {
			setTitle("Current Regions");
		}
		refreshList();
	}
	
	public void addCurrentRegion(Class type)
	{
		addRegion(alignData.getActiveRegionS(), alignData.getActiveRegionE(), type);
	}
	
//	 Adds a new partition to the current list
	public boolean addRegion(int start, int end, Class type)
	{
		annotations = (RegionAnnotations)alignData.getTopaliAnnotations().getAnnotations(type);
		
		// Check it doesn't already exist
		Region r = new Region(start, end);
		
		if (annotations.contains(r))
		{
			MsgBox.msg("The partition from nucleotide " + start + " to "
				+ "nucleotide " + end + " already exists.", MsgBox.ERR);
			return false;
		}
	
		// Add it
		String selection = getAnnotationLabel(type);
		annoType.setSelectedItem(selection);
		refreshList();
		
		int position = annotations.addRegion(r);
		regionModel.add(position, r);
		
		// Deselect all...
		regionList.setSelectedIndices(new int[] { position });
		// ...then select the new element
		regionList.ensureIndexIsVisible(regionList.getSelectedIndex());
				
		// Ensure the dialog is visible
		setVisible(true);
		
		return true;
	}
	
	public void refreshList()
	{
		regionModel.clear();
		
		if(annotations!=null) {
			for (Region r: annotations)
				regionModel.addElement(r);
		}
		
		checkList();
	}

	public void updateTreePreview(boolean repaintAll)
	{
//		 Can only create a tree if a single item is selected in the list
		if (regionList.getSelectedIndices().length != 1)
		{
			treePanel.clearTree();
			return;
		}
	
		// Get the partition details
		Region r =
			(Region) regionList.getSelectedValue();
		
		alignData.setActiveRegion(r.getS(), r.getE());
		
		if (repaintAll)
			winMain.repaintDisplay();
			
		WinMainMenuBar.aFileSave.setEnabled(true);
		// Create the tree
		treePanel.createTree(r.getS(), r.getE());
	}

	public void doExport()
	{
		new ExportDialog(winMain, alignData, annotations, regionList.getSelectedIndices());
	}
	
	public void exit()
	{
		Prefs.gui_pdialog_x  = getLocation().x;
		Prefs.gui_pdialog_y  = getLocation().y;
		Prefs.gui_pdialog_splitter = splitter.getDividerLocation();
	}
	
	/**
	 * Get the label of certain RegionAnnotation class
	 * @param c
	 * @return
	 */
	private String getAnnotationLabel(Class c) {
		try {
			for(Class c2: supportedAnnotations) {
				if(c2.equals(c)) {
					RegionAnnotations tmp = (RegionAnnotations)c.newInstance();
					return tmp.getLabel();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get the RegionAnnotation class matching a certain label
	 * @param name
	 * @return
	 */
	private Class getAnnotationClass(String name) {
		Class result = null;
		try {
			for(Class c: supportedAnnotations) {
				RegionAnnotations tmp = (RegionAnnotations)c.newInstance();
				if(tmp.getLabel().equals(name)) {
					result = c;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private void checkList() {
		bNew.setEnabled(alignData != null);
		
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
	
	private void editRegion() {
		Region r =
			(Region) regionList.getSelectedValue();

		EditRegionDialog dialog = new EditRegionDialog(r, this, alignData.getSequenceSet().getLength());
		
		if(dialog.newRegion!=null) {
			regionModel.removeElement(r);
			annotations.remove(r.getS(), r.getE());
			annotations.addRegion(dialog.newRegion);
			WinMainMenuBar.aFileSave.setEnabled(true);
		}
		
		refreshList();
	}
	
//	 Uses the edit dialog to create a new partition
	private void newPartition()
	{
		int length = alignData.getSequenceSet().getLength();
		
		EditRegionDialog dialog = new EditRegionDialog(new Region(1, length), this, length);
		if(dialog.newRegion!=null) {
			annotations.addRegion(dialog.newRegion);
			WinMainMenuBar.aFileSave.setEnabled(true);
		}
		
		refreshList();
	}
	
	private void buildGui() {
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
		
		
		String[] annoLabels = new String[supportedAnnotations.length];
		for(int i=0; i<supportedAnnotations.length; i++)
			annoLabels[i] = getAnnotationLabel(supportedAnnotations[i]);
		annoType = new JComboBox(annoLabels);
		annoType.addItemListener(this);
		
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
				BorderFactory.createTitledBorder("Region list:")),
			BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		p2a.add(sp, BorderLayout.CENTER);
		
		JPanel cboxPanel = new JPanel();
		cboxPanel.add(new JLabel("Show/Edit"));
		cboxPanel.add(annoType);
		cboxPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel p3 = new JPanel(new BorderLayout());
		p3.add(p2a, BorderLayout.CENTER);
		p3.add(cboxPanel, BorderLayout.NORTH);
		
		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		treePanel.setPreferredSize(new Dimension(300,200));
		p2.add(treePanel);
		p2.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(2, 10, 10, 10),
				BorderFactory.createTitledBorder("Phylogenetic tree preview "
					+ "(approx JC/NJ):")),
			BorderFactory.createEmptyBorder(0, 5, 5, 5)));
		
		splitter.setDividerSize(5);
		splitter.setTopComponent(p3);
		splitter.setBottomComponent(p2);
		if (Prefs.gui_pdialog_splitter == -1)
			splitter.setDividerLocation(0.5);
		else
			splitter.setDividerLocation(Prefs.gui_pdialog_splitter);
		
		setLayout(new BorderLayout());		
		add(splitter, BorderLayout.CENTER);
		add(p1A, BorderLayout.EAST);
		
//		 A double-click should call editPartition()
		regionList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
				{
					int index = regionList.getSelectedIndex();
					if (index != -1)
						editRegion();
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
						editRegion();
				}
			}
		});
		
		Utils.addCloseHandler(this, bClose);
		
		checkList();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bClose)
			setVisible(false);
			
		else if (e.getSource() == bRemoveAll)
		{
			regionModel.clear();
			annotations.deleteAll();
			alignData.setActiveRegion(-1, -1);
			
			// Rehighlight everything			
			winMain.repaintDisplay();
		}
		
		else if (e.getSource() == bRemove)
		{
			int[] indices = regionList.getSelectedIndices();
			for (int i = indices.length-1; i >= 0; i--)
			{
				annotations.remove(indices[i]);
				regionModel.remove(indices[i]);
			}
			
			// Rehighlight everything
			alignData.setActiveRegion(-1, -1);
			winMain.repaintDisplay();
		}
		
		else if (e.getSource() == bEdit)
			editRegion();
					
		else if (e.getSource() == bNew)
			newPartition();
		
		// Call the export dialog (also telling it which partitions are selected)
		else if (e.getSource() == bExport)
			doExport();
	}

	
	public void valueChanged(ListSelectionEvent e) {
		checkList();	
		if (e.getValueIsAdjusting() == false)
			updateTreePreview(true);
	}
	
	public void itemStateChanged(ItemEvent e) {
		String name = (String)annoType.getSelectedItem();
		Class type = getAnnotationClass(name);

		AlignmentAnnotations tmp = alignData.getTopaliAnnotations().getAnnotations(type);
		if(tmp!=null)
			annotations = (RegionAnnotations) tmp;
		
		refreshList();
	}
	
	public static void main(String[] args) {
		RegionDialog d = new RegionDialog();
		d.setVisible(true);
	}
}
