// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree;

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

	private TreeDialogPanel basicPanel;

	private AdvancedMrBayes bayesPanel;

	private AdvancedPhyML phymlPanel;

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

	public void switchMethod() {
		if(tabs==null)
			return;

		if(Prefs.gui_tree_method==0) {
			tabs.setEnabledAt(1, false);
		}
		else if(Prefs.gui_tree_method==1) {
			tabs.remove(1);
			tabs.add(phymlPanel, "Advanced");
			tabs.setEnabledAt(1, true);
		}
		else if(Prefs.gui_tree_method==2) {
			tabs.remove(1);
			tabs.add(bayesPanel, "Advanced");
			tabs.setEnabledAt(1, true);
		}
		else if(Prefs.gui_tree_method==3) {
			tabs.setEnabledAt(1, false);
		}
		validate();
		repaint();
	}
	
	private JComponent createControls()
	{
		basicPanel = new TreeDialogPanel(this, ss);
		basicPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		bayesPanel = new AdvancedMrBayes(data.getSequenceSet(), mbResult);

		phymlPanel = new AdvancedPhyML(data.getSequenceSet(), phymlResult);

		tabs = new JTabbedPane();
		tabs.add(basicPanel, "Basic");
		if(Prefs.gui_tree_method==0 || Prefs.gui_tree_method==3) {
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
		else if(Prefs.gui_tree_method == 3) {
			mbResult.isCDNA = true;
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
		if(Prefs.gui_tree_method==0) {
			if(ss.getParams().isDNA())
				result.guiName = "F84+G Tree" + runNum;
			else
				result.guiName = "WAG+G Tree" + runNum;
			result.jobName = "Tree Estimation";
		}
		else if(Prefs.gui_tree_method==1) {
			result.guiName = "ML Tree "+runNum;
			result.jobName = "PhyML Tree Estimation ";
		}
		else if(Prefs.gui_tree_method==2 || Prefs.gui_tree_method==3) {
			result.guiName = "MrBayes Tree " + runNum;
			result.jobName = "MrBayes Tree Estimation ";
		}

		result.jobName += runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		if(Prefs.gui_tree_method==1)
			initPhymlTreeResult((PhymlResult)tr);

		if (Prefs.gui_tree_method == 2 || Prefs.gui_tree_method == 3)
			initMBTreeResult((MBTreeResult) tr);

	}

	private void initPhymlTreeResult(PhymlResult tr) {
		//Path to Phyml
		if (Prefs.isWindows)
			tr.phymlPath = Utils.getLocalPath() + "\\phyml_win32.exe";
		else if(Prefs.isMacOSX)
			tr.phymlPath = Utils.getLocalPath() + "/phyml/phyml_macOSX";
		else if (Prefs.isLinux)
			tr.phymlPath = Utils.getLocalPath() + "/phyml/phyml_linux";
		else
			tr.phymlPath = Utils.getLocalPath() + "/phyml/phyml_sunOS";

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
			tr.mbPath = Utils.getLocalPath() + "\\mb.exe";
		else
			tr.mbPath = Utils.getLocalPath() + "/mrbayes/mb";

		// Use all sequences, or just those selected?
		if (Prefs.gui_tree_useall)
			tr.selectedSeqs = data.getSequenceSet().getAllSequenceSafeNames();
		else
			tr.selectedSeqs = data.getSequenceSet()
					.getSelectedSequenceSafeNames();

		tr.isCDNA = Prefs.gui_tree_method == 3;
	}
}