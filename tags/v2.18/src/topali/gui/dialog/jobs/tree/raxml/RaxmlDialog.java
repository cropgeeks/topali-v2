// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.tree.raxml;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.Utils;

public class RaxmlDialog extends JDialog implements ActionListener
{
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	
	WinMain winMain;
	AlignmentData data;
	RaxmlResult result;
	
	JTabbedPane tabs;
	RaxmlBasicPanel basic;
	RaxmlAdvancedPanel advanced;
	RaxmlCDNAAdvancedPanel cdnaadvanced;
	
	public RaxmlDialog(WinMain winMain, AlignmentData data, TreeResult result) {
		super(winMain, "RaxML Settings", true);
		this.winMain = winMain;
		this.data = data;
		
		if(result!=null)
			this.result = (RaxmlResult)result;
		
		init();
		
		pack();
		setSize(360,350);
		setLocationRelativeTo(winMain);
		setResizable(false);
	}
	
	private void init() {
		this.getContentPane().setLayout(new BorderLayout());
		
		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "raxml");
		this.getContentPane().add(bp, BorderLayout.SOUTH);
		
		tabs = new JTabbedPane();
		basic = new RaxmlBasicPanel();
		basic.bCodonpos.addActionListener(this);
		boolean div3 = (((data.getActiveRegionE()-data.getActiveRegionS()+1)%3)==0);
		basic.bCodonpos.setEnabled(div3);
		basic.bOnemodel.addActionListener(this);
		advanced = new RaxmlAdvancedPanel(data);
		if(result!=null)
			advanced.initPrevResult(result);
		cdnaadvanced = new RaxmlCDNAAdvancedPanel(data);
		if(result!=null)
			cdnaadvanced.initPrevResult(result);
		
		tabs.add(basic, 0);
		if(Prefs.rax_type==1 && div3)  {
			tabs.add(new JScrollPane(cdnaadvanced), 1);
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
	
	private void ok(boolean remote) {
		if(basic.bOnemodel.isSelected()) {
			this.result = advanced.onOK();
			Prefs.rax_type = 0;
		}
		else if(basic.bCodonpos.isSelected()) {
			this.result = cdnaadvanced.onOK();
			Prefs.rax_type = 1;
		}
		
		result.isRemote = remote;
		result.setPartitionStart(data.getActiveRegionS());
		result.setPartitionEnd(data.getActiveRegionE());
		int runNum = data.getTracker().getTreeRunCount() + 1;
		data.getTracker().setTreeRunCount(runNum);
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.guiName = "#"+runNum+" Tree (RaxML)";
		result.jobName = "RaxML Tree Estimation";
		
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		
		if (Prefs.isWindows)
			result.raxmlPath = Utils.getLocalPath() + "\\raxml.exe";
		else
			result.raxmlPath = Utils.getLocalPath() + "/raxml/raxmlHPC";
		
		setVisible(false);
	}
	
	public RaxmlResult getResult() {
		return this.result;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==basic.bCodonpos && basic.bCodonpos.isSelected()) {
			tabs.remove(1);
			tabs.add(new JScrollPane(cdnaadvanced), 1);
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
			cdnaadvanced.setDefaults();
			basic.bCodonpos.setSelected(false);
			basic.bOnemodel.setSelected(true);
			tabs.remove(1);
			tabs.add(new JScrollPane(advanced), 1);
			tabs.setTitleAt(1, "Advanced");
		}
		
	}
	
}
