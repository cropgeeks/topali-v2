// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.mrbayes;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.utils.Utils;

public class MrBayesDialog extends JDialog implements ActionListener
{
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	JTabbedPane tabs;
	MrBayesBasicPanel basic;
	AdvancedMrBayes advanced;
	AdvancedCDNAMrBayes advancedCDNA;

	WinMain winMain;
	AlignmentData data;
	MBTreeResult result;

	public MrBayesDialog(WinMain winMain, AlignmentData data, TreeResult result) {
		super(winMain, "MrBayes Settings", true);

		this.winMain = winMain;
		this.data = data;
		this.result = (MBTreeResult)result;

		init();

		if(result!=null)
		    initPrefResult((MBTreeResult)result);

		pack();
//		setSize(getWidth(),370);
		setLocationRelativeTo(winMain);
		setResizable(false);
	}

	private void init() {
		this.getContentPane().setLayout(new BorderLayout());

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "mrbayes");
		this.getContentPane().add(bp, BorderLayout.SOUTH);

		tabs = new JTabbedPane();
		basic = new MrBayesBasicPanel();
		basic.bCodonpos.addActionListener(this);
		boolean div3 = (((data.getActiveRegionE()-data.getActiveRegionS()+1)%3)==0);
		basic.bCodonpos.setEnabled(div3);
		basic.bOnemodel.addActionListener(this);
		advanced = new AdvancedMrBayes(data);
		advancedCDNA = new AdvancedCDNAMrBayes(data.getSequenceSet());

		tabs.add(basic, 0);
		if(Prefs.mb_type==1 && div3) {
			tabs.add(new JScrollPane(advancedCDNA), 1);
			basic.bCodonpos.setSelected(true);
		}
		else {
			tabs.add(new JScrollPane(advanced), 1);
			basic.bOnemodel.setSelected(true);
		}

		tabs.setTitleAt(0, "Analysis");
		tabs.setTitleAt(1, "Advanced");

		this.getContentPane().add(tabs, BorderLayout.CENTER);
	}

	private void initPrefResult(MBTreeResult res) {
	    tabs.remove(1);
	    if(res.partitions.size()>1) {
		tabs.add(new JScrollPane(advancedCDNA), 1);
		basic.bCodonpos.setSelected(true);
		advancedCDNA.initPrevResult(res);
	    }
	    else {
		tabs.add(new JScrollPane(advanced), 1);
		basic.bOnemodel.setSelected(true);
		advanced.initPrevResult(res);
	    }
	    tabs.setTitleAt(0, "Analysis");
	    tabs.setTitleAt(1, "Advanced");
	}

	private void ok(boolean remote) {
		if(basic.bOnemodel.isSelected()) {
			this.result = advanced.onOK();
			Prefs.mb_type = 0;
		}
		else if(basic.bCodonpos.isSelected()) {
			this.result = advancedCDNA.onOk();
			Prefs.mb_type = 1;
		}

		result.isRemote = remote;
		result.setPartitionStart(data.getActiveRegionS());
		result.setPartitionEnd(data.getActiveRegionE());
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.guiName = "#"+runNum+" Tree (MrBayes)";
		result.jobName = "MrBayes Tree Estimation";

		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();

		if (SysPrefs.isWindows)
			result.mbPath = Utils.getLocalPath() + "\\mb.exe";
		else
			result.mbPath = Utils.getLocalPath() + "/mrbayes/mb";

		setVisible(false);
	}

	public MBTreeResult getResult() {
		return this.result;
	}


	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==basic.bCodonpos && basic.bCodonpos.isSelected()) {
			tabs.remove(1);
			tabs.add(new JScrollPane(advancedCDNA), 1);
			tabs.setTitleAt(0, "Analysis");
			tabs.setTitleAt(1, "Advanced");
		}
		else if(e.getSource()==basic.bOnemodel && basic.bOnemodel.isSelected()) {
			tabs.remove(1);
			tabs.add(new JScrollPane(advanced), 1);
			tabs.setTitleAt(0, "Analysis");
			tabs.setTitleAt(1, "Advanced");
		}

		else if (e.getSource() == bCancel) {
			this.result = null;
			setVisible(false);
		}

		else if (e.getSource() == bRun)
			ok((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);

		else if(e.getSource() == bDefault) {
			advanced.setDefaults();
			advancedCDNA.setDefaults();
			basic.bCodonpos.setSelected(false);
			basic.bOnemodel.setSelected(true);
			tabs.remove(1);
			tabs.add(new JScrollPane(advanced), 1);
			tabs.setTitleAt(1, "Advanced");
		}

	}


}
