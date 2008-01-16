// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.phyml;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.*;
import topali.var.utils.Utils;

public class PhymlDialog extends JDialog implements ActionListener
{
	
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	AdvancedPhyML advanced;
	
	WinMain winMain;
	AlignmentData data;
	PhymlResult result;
	
	public PhymlDialog(WinMain winMain, AlignmentData data, TreeResult result) {
		super(winMain,"PhyML Settings", true);
		
		this.winMain = winMain;
		this.data = data;
		this.result = (PhymlResult)result;
		
		init();
		
		pack();
		setSize(360,250);
		setLocationRelativeTo(winMain);
		setResizable(false);
	}
	
	public PhymlResult getResult() {
		return this.result;
	}
	
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		
		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "phyml");
		this.getContentPane().add(bp, BorderLayout.SOUTH);

		advanced = new AdvancedPhyML(data.getSequenceSet(), result);
		this.getContentPane().add(new JScrollPane(advanced), BorderLayout.CENTER);
	}
	
	private void ok(boolean remote) {
		result = advanced.onOK();
		result.isRemote = remote;
		result.setPartitionStart(data.getActiveRegionS());
		result.setPartitionEnd(data.getActiveRegionE());
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.guiName = "#"+runNum+" Tree (PhyML)";
		result.jobName = "PhyML Tree Estimation";
		
		//Path to Phyml
		if (SysPrefs.isWindows)
			result.phymlPath = Utils.getLocalPath() + "\\phyml_win32.exe";
		else if(SysPrefs.isMacOSX)
			result.phymlPath = Utils.getLocalPath() + "/phyml/phyml_macOSX";
		else if (SysPrefs.isLinux)
			result.phymlPath = Utils.getLocalPath() + "/phyml/phyml_linux";
		else
			result.phymlPath = Utils.getLocalPath() + "/phyml/phyml_sunOS";
		
		setVisible(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		
		if (e.getSource() == bCancel) {
			this.result = null;
			setVisible(false);
		}

		else if (e.getSource() == bRun)
			ok((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
		
		else if(e.getSource() == bDefault) {
			advanced.setDefaults();
		}
		
	}
}
