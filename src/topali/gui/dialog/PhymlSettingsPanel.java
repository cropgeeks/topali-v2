// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;

public class PhymlSettingsPanel extends JPanel implements ItemListener
{
	private JLabel l2 = null;
	private JLabel l3 = null;
	private JLabel l4 = null;
	private JLabel l5 = null;
	private JLabel l6 = null;
	private JLabel l7 = null;
	private JComboBox model = null;
	private JCheckBox sites = null;
	private JCheckBox gamma = null;
	private JCheckBox optTop = null;
	private JCheckBox optBranch = null;
	private JSpinner nBoot = null;
	
	SequenceSet ss;
	PhymlResult result;
	
	/**
	 * This is the default constructor
	 */
	public PhymlSettingsPanel(SequenceSet ss, PhymlResult result)
	{
		super();
		this.ss = ss;
		this.result = result;
		initialize();
		setDefaults();
	}

	private void setDefaults() {
		SequenceSetParams params = ss.getParams();
		
		String[] models = ss.isDNA() ?  SequenceSetParams.availDNAModels : SequenceSetParams.availAAModels;
		ComboBoxModel cm = new DefaultComboBoxModel(models);
		this.model.setModel(cm);
		String m = params.getModel();
		for(int i=0; i<models.length; i++)
			if(models[i].equals(m)) {
				this.model.setSelectedIndex(i);
				break;
			}
		
		
		this.gamma.setSelected(params.isModelGamma());
		this.sites.setSelected(params.isModelInv());
		
		this.optBranch.setSelected(result.optBranchPara);
		this.optTop.setSelected(result.optTopology);
		
		SpinnerNumberModel mNBoot = new SpinnerNumberModel(result.bootstrap, 0, 1000, 10);
		this.nBoot.setModel(mNBoot);
	}
	
	public void onOK() {
		ss.getParams().setModel((String)model.getSelectedItem());
		ss.getParams().setModelGamma(gamma.isSelected());
		ss.getParams().setModelInv(sites.isSelected());
		
		result.bootstrap = (Integer)nBoot.getValue();
		result.optTopology = optTop.isSelected();
		result.optBranchPara = optBranch.isSelected();
	}
	
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder("Advanced Phyml Settings"));
		
		GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
		gridBagConstraints13.gridx = 1;
		gridBagConstraints13.anchor = GridBagConstraints.WEST;
		gridBagConstraints13.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints13.weightx = 1.0;
		gridBagConstraints13.gridy = 5;
		GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
		gridBagConstraints12.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints12.gridy = 4;
		gridBagConstraints12.weightx = 1.0;
		gridBagConstraints12.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints12.anchor = GridBagConstraints.WEST;
		gridBagConstraints12.gridx = 1;
		GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
		gridBagConstraints11.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints11.gridy = 3;
		gridBagConstraints11.weightx = 1.0;
		gridBagConstraints11.anchor = GridBagConstraints.WEST;
		gridBagConstraints11.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints11.gridx = 1;
		GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
		gridBagConstraints10.gridx = 1;
		gridBagConstraints10.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints10.anchor = GridBagConstraints.WEST;
		gridBagConstraints10.weightx = 1.0;
		gridBagConstraints10.gridy = 2;
		GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
		gridBagConstraints9.gridx = 1;
		gridBagConstraints9.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints9.anchor = GridBagConstraints.WEST;
		gridBagConstraints9.weightx = 1.0;
		gridBagConstraints9.gridy = 1;
		GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
		gridBagConstraints8.fill = GridBagConstraints.VERTICAL;
		gridBagConstraints8.gridy = 0;
		gridBagConstraints8.weightx = 1.0;
		gridBagConstraints8.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints8.anchor = GridBagConstraints.WEST;
		gridBagConstraints8.gridx = 1;
		GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
		gridBagConstraints6.gridx = 0;
		gridBagConstraints6.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints6.weightx = 0.1;
		gridBagConstraints6.anchor = GridBagConstraints.EAST;
		gridBagConstraints6.gridy = 5;
		l7 = new JLabel();
		l7.setText("Bootstrap:");
		GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
		gridBagConstraints5.gridx = 0;
		gridBagConstraints5.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints5.weightx = 0.1;
		gridBagConstraints5.anchor = GridBagConstraints.EAST;
		gridBagConstraints5.gridy = 4;
		l6 = new JLabel();
		l6.setText("Optimize branch length/rate para.?");
		GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
		gridBagConstraints4.gridx = 0;
		gridBagConstraints4.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints4.weightx = 0.1;
		gridBagConstraints4.anchor = GridBagConstraints.EAST;
		gridBagConstraints4.gridy = 3;
		l5 = new JLabel();
		l5.setText("Optimize Topology?");
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 0;
		gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints3.weightx = 0.1;
		gridBagConstraints3.anchor = GridBagConstraints.EAST;
		gridBagConstraints3.gridy = 2;
		l4 = new JLabel();
		l4.setText("Gamma");
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.gridx = 0;
		gridBagConstraints2.weightx = 0.1;
		gridBagConstraints2.weighty = 0.0;
		gridBagConstraints2.anchor = GridBagConstraints.EAST;
		gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints2.gridy = 1;
		l3 = new JLabel();
		l3.setText("Invariant Sites");
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.gridx = 0;
		gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
		gridBagConstraints1.weightx = 0.1;
		gridBagConstraints1.anchor = GridBagConstraints.EAST;
		gridBagConstraints1.gridy = 0;
		l2 = new JLabel();
		l2.setText("Substituion Model:");
		this.add(l2, gridBagConstraints1);
		this.add(l3, gridBagConstraints2);
		this.add(l4, gridBagConstraints3);
		this.add(l7, gridBagConstraints4);
		this.add(l5, gridBagConstraints5);
		this.add(l6, gridBagConstraints6);
		this.add(getModel(), gridBagConstraints8);
		this.add(getSites(), gridBagConstraints9);
		this.add(getGamma(), gridBagConstraints10);
		this.add(getNBoot(), gridBagConstraints11);
		this.add(getOptTop(), gridBagConstraints12);
		this.add(getOptBranch(), gridBagConstraints13);
	}

	/**
	 * This method initializes model	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getModel()
	{
		if (model == null)
		{
			model = new JComboBox();
			model.setToolTipText("Substitution model to use");
		}
		return model;
	}

	/**
	 * This method initializes sites	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getSites()
	{
		if (sites == null)
		{
			sites = new JCheckBox();
			sites.setToolTipText("Allow invariant sites");
		}
		return sites;
	}

	/**
	 * This method initializes gamma	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getGamma()
	{
		if (gamma == null)
		{
			gamma = new JCheckBox();
			gamma.setToolTipText("Use gamma distribution");
		}
		return gamma;
	}

	/**
	 * This method initializes ngen	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JSpinner getNBoot()
	{
		if (nBoot == null)
		{
			nBoot = new JSpinner();
			nBoot.setToolTipText("Number of bootstrap runs");
		}
		return nBoot;
	}
	
	private JCheckBox getOptTop() {
		if(optTop==null){
			optTop = new JCheckBox();
			optTop.addItemListener(this);
			optTop.setToolTipText("Optimize Tree Topology (forces optimization of branch lengths and rate parameters)");
		}
		return optTop;
	}

	private JCheckBox getOptBranch() {
		if(optBranch==null){
			optBranch = new JCheckBox();
			optBranch.addItemListener(this);
			optBranch.setToolTipText("Optimize branch lengths and rate parameters");
		}
		return optBranch;
	}

	public void itemStateChanged(ItemEvent e)
	{
		if(optTop.isSelected()) {
			optBranch.setSelected(true);
		}
	}
	
	
}
