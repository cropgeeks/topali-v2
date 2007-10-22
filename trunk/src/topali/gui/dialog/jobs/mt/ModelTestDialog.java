// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs.mt;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import topali.data.*;
import topali.data.models.*;
import topali.gui.*;
import topali.var.Utils;

public class ModelTestDialog extends JDialog implements ActionListener
{
	public static final String MRBAYES = "MrBayes";
	public static final String PHYML = "PhyML";
	
	AlignmentData data;
	SequenceSet ss;
	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();
	JComboBox selection;
	ModelTestResult result;
	
	public ModelTestDialog(AlignmentData data, ModelTestResult res) {
		super(TOPALi.winMain, "Model Selection", true);
		this.data = data;
		this.ss = data.getSequenceSet();
		init();

		if(res!=null) {
			this.result = res;
			selection.setSelectedItem(res.selection);
		}
		
		pack();
		setLocationRelativeTo(TOPALi.winMain);
	}
	
	private void init() {
		JPanel p1 = new JPanel();
		selection = new JComboBox(new String[]{PHYML, MRBAYES});
		
		p1.add(new JLabel("Test Models for:"));
		p1.add(selection);

		this.add(p1, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "modelselection");
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource().equals(bRun)) {
			boolean remote = (e.getModifiers() & ActionEvent.CTRL_MASK) == 0;
			result = new ModelTestResult();
			result.selection = (String)selection.getSelectedItem();
			
			List<Model> availModels = null;
			if(selection.getSelectedItem().equals(PHYML)) {
				availModels = ModelManager.getInstance().listPhymlModels(ss.getParams().isDNA());
			}
			else if(selection.getSelectedItem().equals(MRBAYES)) { 
				availModels = ModelManager.getInstance().listMrBayesModels(ss.getParams().isDNA());
			}
			ArrayList<Model> models = new ArrayList<Model>();
			for(Model m : availModels) {
					Model m1 = ModelManager.getInstance().generateModel(m.getName(), false, false);
					Model m2 = ModelManager.getInstance().generateModel(m.getName(), true, false);
					Model m3 = ModelManager.getInstance().generateModel(m.getName(), false, true);
					Model m4 = ModelManager.getInstance().generateModel(m.getName(), true, true);
					models.add(m1);
					models.add(m2);
					models.add(m3);
					models.add(m4);
			}
			result.models = models;

			result.isRemote = remote;
			result.selectedSeqs = ss.getSelectedSequenceSafeNames();
			
			int runNum = data.getTracker().getMtRunCount() + 1;
			data.getTracker().setMtRunCount(runNum);
			result.guiName = "Model Selection " + runNum;
			result.jobName = "Model Selection on " + data.name + " ("
					+ ss.getSelectedSequences().length + "/" + ss.getSize()
					+ " sequences)";

			this.setVisible(false);
		}
		else if(e.getSource().equals(bCancel)) {
			result = null;
			this.setVisible(false);
		}
		else if(e.getSource().equals(bDefault)) {
			selection.setSelectedIndex(0);
		}
	}

	public ModelTestResult getResult() {
		return result;
	}
}
