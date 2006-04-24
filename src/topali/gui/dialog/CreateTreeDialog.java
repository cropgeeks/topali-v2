/*
 * CreateTreeDialog.java
 *
 * Created on 23 November 2005, 16:29
 */

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import pal.alignment.*;

import topali.data.*;
import topali.gui.*;

import doe.*;

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
		
		bayesPanel = new MrBayesSettingsPanel();
		bayesPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		tabs = new JTabbedPane();
		tabs.add(basicPanel, "Basic");
		tabs.add(bayesPanel, "Advanced");		
		
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
		else
			result = new MBTreeResult();
		
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
		
		alignment = ss.getAlignment(
			indices, result.getPartitionStart(), result.getPartitionEnd(), true);
		
		return result;
	}
	
	public Alignment getAlignment()
		{ return alignment; }
	
	private void initTreeResult(TreeResult tr)
	{
		// Current partition information
		PartitionAnnotations pAnnotations =
			data.getTopaliAnnotations().getPartitionAnnotations();
		
		tr.setPartitionStart(pAnnotations.getCurrentStart());
		tr.setPartitionEnd(pAnnotations.getCurrentEnd());
					
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		
		result.guiName = "Tree " + runNum;
		result.jobName = "Tree Estimation " + runNum + " on " + data.name + " ("
			+ ss.getSelectedSequences().length + "/" + ss.getSize()
			+ " sequences)";
		
		if (Prefs.gui_tree_method == 1)
			initMBTreeResult((MBTreeResult)tr);
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
			tr.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
	}
	
	static class BasicTreePanel extends JPanel
	{
		private JRadioButton rMethod0, rMethod1, rSelectAll, rSelectCurrent;
		
		BasicTreePanel()
		{
			rMethod0 = new JRadioButton("Jukes cantor + uniform rates model/neighbor joining", Prefs.gui_tree_method == 0);
			rMethod0.setMnemonic(KeyEvent.VK_J);
			rMethod1 = new JRadioButton("Bayesian phylogenetic analysis (using MrBayes)", Prefs.gui_tree_method == 1);
			rMethod1.setMnemonic(KeyEvent.VK_B);			
			ButtonGroup g1 = new ButtonGroup();
			g1.add(rMethod0);
			g1.add(rMethod1);
			
			rSelectAll = new JRadioButton("Use all sequences in the alignment", Prefs.gui_tree_useall);
			rSelectAll.setMnemonic(KeyEvent.VK_A);
			rSelectCurrent = new JRadioButton("Use currently selected sequences only", !Prefs.gui_tree_useall);
			rSelectCurrent.setMnemonic(KeyEvent.VK_C);			
			ButtonGroup g2 = new ButtonGroup();
			g2.add(rSelectAll);
			g2.add(rSelectCurrent);
			
			JPanel p1 = new JPanel(new GridLayout(2, 1, 5, 0));
			p1.setBorder(BorderFactory.createTitledBorder("Tree creation method:"));
			p1.add(rMethod0);
			p1.add(rMethod1);
			JPanel p2 = new JPanel(new GridLayout(2, 1, 5, 0));
			p2.setBorder(BorderFactory.createTitledBorder("Sequence selection:"));
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
			if (rMethod1.isSelected())
				Prefs.gui_tree_method = 1;
				
			Prefs.gui_tree_useall = rSelectAll.isSelected();
		}
	}
}