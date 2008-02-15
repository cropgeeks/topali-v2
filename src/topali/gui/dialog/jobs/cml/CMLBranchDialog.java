// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.cml;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import pal.alignment.SimpleAlignment;
import pal.tree.Tree;
import topali.analyses.TreeCreatorThread;
import topali.data.*;
import topali.gui.WinMain;
import topali.var.SysPrefs;
import topali.var.utils.*;

public class CMLBranchDialog extends JDialog implements ActionListener
{

	BranchModelPanel bp;
	
	public JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	
	WinMain winMain;
	public AlignmentData data;
	public CodeMLResult result;
	
	String tree;
	
	public CMLBranchDialog(WinMain winMain, AlignmentData data, CodeMLResult result) {
		super(winMain, "Positive Selection - Branch Models (under test)", false);
		
		this.winMain = winMain;
		this.data = data;
		this.result = result;
		
		createTree();
		init();
		
		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		
		if(this.result==null)
			this.result = new CodeMLResult(CodeMLResult.TYPE_BRANCHMODEL);
		
	}
	
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		bp = new BranchModelPanel(this);
		if(result==null)
			bp.setH0(this.tree);
		this.getContentPane().add(bp, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "cmlbranch_settings");
		getContentPane().add(bp, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}

	private void createTree() {
		
		SequenceSet ss = data.getSequenceSet();
		SimpleAlignment alignment = ss.getAlignment(ss.getSelectedSequences(), data.getActiveRegionS(), data.getActiveRegionE(), false);
		
//		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
//		Tree tree = new NeighborJoiningTree(dm);
//		this.tree = tree.toString();
//		this.tree = this.tree.replaceAll(";", "");
//		this.tree = NHTreeUtils.removeBranchLengths(this.tree);
		
		TreeCreatorThread tc = new TreeCreatorThread(alignment, data.getSequenceSet().getParams().isDNA(), false);
		Tree tree = tc.getTree();
		this.tree = tree.toString();
		this.tree = this.tree.replaceAll(";", "");
		this.tree = TreeUtils.removeBranchLengths(this.tree);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel) {
			setVisible(false);
		}

		else if (e.getSource() == bRun)
		{
			setVisible(false);
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;
			runAction(remote);
		}

		else if (e.getSource() == bDefault) {
			bRun.setEnabled(false);
			bp.setDefaults();
		}
	}
	
	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		if(!b) {
			if(bp.atv!=null) {
				bp.atv.close();
			}
		}
	}
	
	private void runAction(boolean remote) {
		setVisible(false);
		
		SequenceSet ss = data.getSequenceSet();
		
		if (SysPrefs.isWindows)
			result.codemlPath = Utils.getLocalPath() + "codeml.exe";
		else
			result.codemlPath = Utils.getLocalPath() + "codeml/codeml";

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = remote;
		
		result.hypos.clear();
		int size =  bp.model.getSize();
		for(int i=0; i<size; i++) {
			String tree = (String)bp.model.getElementAt(i);
			tree = Utils.getSafenameTree(tree, ss);
			CMLHypothesis hypo = new CMLHypothesis();
			hypo.tree = tree;
			if(i==0)
				hypo.model = 0;
			else
				hypo.model = 2;
			result.hypos.add(hypo);
		}
		
		int runNum = data.getTracker().getCodeMLRunCount() + 1;
		data.getTracker().setCodeMLRunCount(runNum);
		result.guiName = "PAML Branch " + runNum;
		result.jobName = "PAML/CodeML Analysis " + runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		winMain.submitJob(data, result);
	}
	
}
