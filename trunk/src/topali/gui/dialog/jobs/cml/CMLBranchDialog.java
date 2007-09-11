// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.cml;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import pal.alignment.SimpleAlignment;
import pal.tree.Tree;

import topali.analyses.TreeCreator;
import topali.data.*;
import topali.gui.*;
import topali.var.*;

public class CMLBranchDialog extends JDialog implements ActionListener
{

	BranchModelPanel bp;
	
	public JButton bRun, bCancel, bDefault, bHelp;
	
	WinMain winMain;
	AlignmentData data;
	public CodeMLResult result;
	
	String tree;
	
	public CMLBranchDialog(WinMain winMain, AlignmentData data, CodeMLResult result) {
		super(winMain, "Positive Selection - Branch Models", false);
		
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
		
		bRun = new JButton("Run");
		bRun.setEnabled(false);
		bRun.addActionListener(this);
		bCancel = new JButton(Text.Gui.getString("cancel"));
		bCancel.addActionListener(this);
		bDefault = new JButton(Text.Gui.getString("defaults"));
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("codeml_branch_help");
		JPanel p1 = new JPanel(new GridLayout(1, 4, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bRun);
		p1.add(bDefault);
		p1.add(bCancel);
		p1.add(bHelp);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		this.getContentPane().add(p2, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}

	private void createTree() {
		SequenceSet ss = data.getSequenceSet();
		SimpleAlignment alignment = ss.getAlignment(ss.getSelectedSequences(), data.getActiveRegionS(), data.getActiveRegionE(), false);
		TreeCreator tc = new TreeCreator(alignment, data.getSequenceSet().isDNA());
		Tree tree = tc.getTree(false, false);
		this.tree = tree.toString();
		this.tree = this.tree.replaceAll(";", "");
		this.tree = NHTreeUtils.removeBranchLengths(this.tree);
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
		
		if (Prefs.isWindows)
			result.codemlPath = Utils.getLocalPath() + "codeml.exe";
		else
			result.codemlPath = Utils.getLocalPath() + "codeml/codeml";

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = remote;
		
		result.hypos.clear();
		int size =  bp.model.getSize();
		for(int i=0; i<size; i++) {
			String tree = (String)bp.model.getElementAt(i);
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
		result.guiName = "CodeML Result " + runNum;
		result.jobName = "CodeML Analysis " + runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		winMain.submitJob(data, result);
	}
	
}
