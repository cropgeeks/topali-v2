// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import pal.alignment.Alignment;
import topali.data.*;
import topali.gui.*;
import topali.var.Utils;
import doe.MsgBox;

public class CreateTreeDialog extends JDialog implements ActionListener
{
	private AlignmentData data;

	private SequenceSet ss;

	private Alignment alignment;

	private TreeResult result;

	private JButton bOK, bCancel;

	private JTabbedPane tabs;

	private BasicTreePanel basicPanel;

	private MrBayesSettingsPanel bayesPanel;
	
	private PhymlSettingsPanel phymlPanel;
	
	MBTreeResult mbResult = new MBTreeResult();
	
	PhymlResult phymlResult = new PhymlResult();
	
	public CreateTreeDialog(WinMain winMain, AlignmentData data)
	{
		super(winMain, "Estimate New Tree", true);
		this.data = data;

		ss = data.getSequenceSet();
		
		setLayout(new BorderLayout());
		add(createControls());
		add(getButtons(), BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bOK);
		Utils.addCloseHandler(this, bCancel);

		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private JComponent createControls()
	{
		basicPanel = new BasicTreePanel();
		basicPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		bayesPanel = new MrBayesSettingsPanel(data.getSequenceSet(), mbResult);
		
		phymlPanel = new PhymlSettingsPanel(data.getSequenceSet(), phymlResult);
		
		tabs = new JTabbedPane();
		tabs.add(basicPanel, "Basic");
		if(Prefs.gui_tree_method==0) {
			tabs.add(new JPanel(), "Advanced");
			tabs.setEnabledAt(1, false);
		}
		else if(Prefs.gui_tree_method==1) {
			tabs.add(phymlPanel, "Advanced");
		}
		else if(Prefs.gui_tree_method==2) {
			tabs.add(bayesPanel, "Advanced");
		}

		return tabs;
	}

	private JPanel getButtons()
	{
		bOK = new JButton(Text.Gui.getString("ok"));
		bCancel = new JButton(Text.Gui.getString("cancel"));

		return Utils.getButtonPanel(this, bOK, bCancel, "estimate_tree");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bOK)
			onOK((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
	}

	private void onOK(boolean makeRemote)
	{
		basicPanel.onOK();
		
		
		if (Prefs.gui_tree_method == 0)
			result = new TreeResult();
		else if(Prefs.gui_tree_method == 1) {
			phymlPanel.onOK();
			result = phymlResult;
		}
		else if(Prefs.gui_tree_method == 2) {
			bayesPanel.onOK();
			result = mbResult;
		}

		result.isRemote = makeRemote;
		initTreeResult(result);
		setVisible(false);
	}

	// Returns the TreeResult object, and also sets up the PAL alignment used
	// to make the tree if a local JC tree is being created.
	public TreeResult getTreeResult()
	{
		if (result == null)
			return null;

		int[] indices = null;
		if (Prefs.gui_tree_useall)
			indices = ss.getAllSequences();
		else
			indices = ss.getSelectedSequences();

		if (indices.length < 3)
		{
			MsgBox.msg("You must have at least 3 sequences selected to create "
					+ "a phylogenetic tree.", MsgBox.ERR);
			return null;
		}

		alignment = ss.getAlignment(indices, result.getPartitionStart(), result
				.getPartitionEnd(), true);

		return result;
	}

	public Alignment getAlignment()
	{
		return alignment;
	}

	private void initTreeResult(TreeResult tr)
	{
		// Current partition information
		tr.setPartitionStart(data.getActiveRegionS());
		tr.setPartitionEnd(data.getActiveRegionE());

		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		if(Prefs.gui_tree_method==0)
			result.guiName = "F84+G Tree " + runNum;
		else if(Prefs.gui_tree_method==1)
			result.guiName = "ML Tree "+runNum;
		else if(Prefs.gui_tree_method==2)
			result.guiName = "MrBayes Tree " + runNum;
		
		result.jobName = "Tree Estimation " + runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";
		
		if(Prefs.gui_tree_method==1)
			initPhymlTreeResult((PhymlResult)tr);

		if (Prefs.gui_tree_method == 2)
			initMBTreeResult((MBTreeResult) tr);
	}

	private void initPhymlTreeResult(PhymlResult tr) {
		//Path to Phyml
		if (Prefs.isWindows)
			tr.phymlPath = Utils.getLocalPath() + "phyml\\phyml_win32.exe";
		else if(Prefs.isMacOSX)
			tr.phymlPath = Utils.getLocalPath() + "phyml/phyml_macOSX";
		else
			tr.phymlPath = Utils.getLocalPath() + "phyml/phyml_linux";
		
		//Use all sequences, or just those selected?
		if (Prefs.gui_tree_useall)
			tr.selectedSeqs = data.getSequenceSet().getAllSequenceSafeNames();
		else
			tr.selectedSeqs = data.getSequenceSet()
					.getSelectedSequenceSafeNames();
	}
	
	private void initMBTreeResult(MBTreeResult tr)
	{
		// Path to MrBayes
		if (Prefs.isWindows)
			tr.mbPath = Utils.getLocalPath() + "mb.exe";
		else
			tr.mbPath = Utils.getLocalPath() + "mrbayes/mb";

		// Use all sequences, or just those selected?
		if (Prefs.gui_tree_useall)
			tr.selectedSeqs = data.getSequenceSet().getAllSequenceSafeNames();
		else
			tr.selectedSeqs = data.getSequenceSet()
					.getSelectedSequenceSafeNames();
	}

	class BasicTreePanel extends JPanel implements ChangeListener
	{
		private JRadioButton rMethod0, rMethod1, rMethod2, rSelectAll, rSelectCurrent;

		BasicTreePanel()
		{
			rMethod0 = new JRadioButton(
					"F84+Gamma/neighbor joining (fast, approximate)",
					Prefs.gui_tree_method == 0);
			rMethod0.addChangeListener(this);
			rMethod0.setMnemonic(KeyEvent.VK_F);
			rMethod1 = new JRadioButton("Maximum Likelihood (using PhyML) (moderate)", Prefs.gui_tree_method == 1);
			rMethod1.addChangeListener(this);
			rMethod1.setMnemonic(KeyEvent.VK_M);
			rMethod2 = new JRadioButton(
					"Bayesian phylogenetic analysis (using MrBayes) (sophisticated)",
					Prefs.gui_tree_method == 2);
			rMethod2.addChangeListener(this);
			rMethod2.setMnemonic(KeyEvent.VK_B);
			ButtonGroup g1 = new ButtonGroup();
			g1.add(rMethod0);
			g1.add(rMethod1);
			g1.add(rMethod2);

			rSelectAll = new JRadioButton("Use all sequences in the alignment",
					Prefs.gui_tree_useall);
			rSelectAll.setMnemonic(KeyEvent.VK_A);
			rSelectCurrent = new JRadioButton(
					"Use currently selected sequences only",
					!Prefs.gui_tree_useall);
			rSelectCurrent.setMnemonic(KeyEvent.VK_C);
			ButtonGroup g2 = new ButtonGroup();
			g2.add(rSelectAll);
			g2.add(rSelectCurrent);

			JPanel p1 = new JPanel(new GridLayout(3, 1, 5, 0));
			p1.setBorder(BorderFactory
					.createTitledBorder("Tree creation method:"));
			p1.add(rMethod0);
			p1.add(rMethod1);
			p1.add(rMethod2);
			JPanel p2 = new JPanel(new GridLayout(2, 1, 5, 0));
			p2.setBorder(BorderFactory
					.createTitledBorder("Sequence selection:"));
			p2.add(rSelectAll);
			p2.add(rSelectCurrent);

			setLayout(new GridLayout(2, 1, 5, 5));
			add(p1);
			add(p2);
		}

		void onOK()
		{
			if (rMethod0.isSelected())
				Prefs.gui_tree_method = 0;
			if(rMethod1.isSelected())
				Prefs.gui_tree_method = 1;
			if (rMethod2.isSelected())
				Prefs.gui_tree_method = 2;
			
			Prefs.gui_tree_useall = rSelectAll.isSelected();
		}

		public void stateChanged(ChangeEvent e)
		{
			if(tabs==null)
				return;
			
			if(rMethod0.isSelected()) {
				tabs.setEnabledAt(1, false);
			}
			else if(rMethod1.isSelected()) {
				tabs.remove(1);
				tabs.add(phymlPanel, "Advanced");
				tabs.setEnabledAt(1, true);
			}
			else if(rMethod2.isSelected()) {
				tabs.remove(1);
				tabs.add(bayesPanel, "Advanced");
				tabs.setEnabledAt(1, true);
			}
			validate();
			repaint();
		}
		
		
	}
}