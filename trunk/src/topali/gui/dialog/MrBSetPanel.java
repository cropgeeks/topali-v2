// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;

import javax.swing.*;

import topali.data.*;

import java.awt.Insets;

public class MrBSetPanel extends JPanel
{

	private static final long serialVersionUID = 1L;
	private JLabel label = null;
	private JPanel panel = null;
	private JLabel l1 = null;
	private JLabel l2 = null;
	private JLabel l3 = null;
	private JLabel l4 = null;
	private JLabel l5 = null;
	private JLabel l6 = null;
	private JLabel l7 = null;
	private JComboBox code = null;
	private JComboBox model = null;
	private JCheckBox sites = null;
	private JCheckBox gamma = null;
	private JSpinner ngen = null;
	private JSpinner sample = null;
	private JPanel subpanel = null;
	private JSpinner burnin = null;
	private JLabel l8 = null;
	
	SequenceSet ss;
	MBTreeResult result;
	
	/**
	 * This is the default constructor
	 */
	public MrBSetPanel(SequenceSet ss, MBTreeResult result)
	{
		super();
		this.ss = ss;
		this.result = result;
		initialize();
		setDefaults();
	}

	private void setDefaults() {
		SequenceSetParams params = ss.getParams();
		
		ComboBoxModel cm = new DefaultComboBoxModel(SequenceSetParams.availCodes);
		this.code.setModel(cm);
		String c = params.getGeneticCode();
		for(int i=0; i<SequenceSetParams.availCodes.length; i++)
			if(SequenceSetParams.availCodes[i].equals(c)) {
				this.code.setSelectedIndex(i);
				break;
			}
		
		String[] models = ss.isDNA() ?  SequenceSetParams.availDNAModels : SequenceSetParams.availAAModels;
		cm = new DefaultComboBoxModel(models);
		this.model.setModel(cm);
		String m = params.getModel();
		for(int i=0; i<models.length; i++)
			if(models[i].equals(m)) {
				this.model.setSelectedIndex(i);
				break;
			}
		
		
		this.gamma.setSelected(params.isModelGamma());
		this.sites.setSelected(params.isModelInv());
		
		SpinnerNumberModel mNGen = new SpinnerNumberModel(result.nGen, 10000, 500000, 10000);
		this.ngen.setModel(mNGen);
		
		SpinnerNumberModel mFreq = new SpinnerNumberModel(result.sampleFreq, 1, 1000, 1);
		this.sample.setModel(mFreq);
		
		SpinnerNumberModel mBurn = new SpinnerNumberModel(result.burnin, 0.01, 0.99, 0.01);
		this.burnin.setModel(mBurn);
	}
	
	private void onOk() {
		ss.getParams().setGeneticCode((String)code.getSelectedItem());
		ss.getParams().setModel((String)model.getSelectedItem());
		ss.getParams().setModelGamma(gamma.isSelected());
		ss.getParams().setModelInv(sites.isSelected());
		
		result.burnin = (Double)burnin.getValue();
		result.nGen = (Integer)ngen.getValue();
		result.sampleFreq = (Integer)sample.getValue();
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		label = new JLabel();
		label.setText("Advanced MrBayes Settings");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		this.setSize(437, 273);
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		this.add(label, BorderLayout.NORTH);
		this.add(getPanel(), BorderLayout.CENTER);
	}

	/**
	 * This method initializes panel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanel()
	{
		if (panel == null)
		{
			GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
			gridBagConstraints13.gridx = 1;
			gridBagConstraints13.anchor = GridBagConstraints.WEST;
			gridBagConstraints13.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints13.gridy = 6;
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints12.gridy = 5;
			gridBagConstraints12.weightx = 1.0;
			gridBagConstraints12.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints12.anchor = GridBagConstraints.WEST;
			gridBagConstraints12.gridx = 1;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints11.gridy = 4;
			gridBagConstraints11.weightx = 1.0;
			gridBagConstraints11.anchor = GridBagConstraints.WEST;
			gridBagConstraints11.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints11.gridx = 1;
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.gridx = 1;
			gridBagConstraints10.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints10.anchor = GridBagConstraints.WEST;
			gridBagConstraints10.gridy = 3;
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 1;
			gridBagConstraints9.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints9.anchor = GridBagConstraints.WEST;
			gridBagConstraints9.gridy = 2;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints8.gridy = 1;
			gridBagConstraints8.weightx = 1.0;
			gridBagConstraints8.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints8.anchor = GridBagConstraints.WEST;
			gridBagConstraints8.gridx = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints7.gridy = 0;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.anchor = GridBagConstraints.WEST;
			gridBagConstraints7.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints7.gridx = 1;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints6.weightx = 1.0;
			gridBagConstraints6.anchor = GridBagConstraints.EAST;
			gridBagConstraints6.gridy = 6;
			l7 = new JLabel();
			l7.setText("Burnin:");
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.anchor = GridBagConstraints.EAST;
			gridBagConstraints5.gridy = 5;
			l6 = new JLabel();
			l6.setText("Sample Frequency:");
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.anchor = GridBagConstraints.EAST;
			gridBagConstraints4.gridy = 4;
			l5 = new JLabel();
			l5.setText("nGenerations:");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.anchor = GridBagConstraints.EAST;
			gridBagConstraints3.gridy = 3;
			l4 = new JLabel();
			l4.setText("Gamma");
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.weightx = 1.0;
			gridBagConstraints2.weighty = 0.0;
			gridBagConstraints2.anchor = GridBagConstraints.EAST;
			gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints2.gridy = 2;
			l3 = new JLabel();
			l3.setText("Invariant Sites");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.anchor = GridBagConstraints.EAST;
			gridBagConstraints1.gridy = 1;
			l2 = new JLabel();
			l2.setText("Substituion Model:");
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.anchor = GridBagConstraints.EAST;
			gridBagConstraints.gridy = 0;
			l1 = new JLabel();
			l1.setText("Genetic Code:");
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			panel.add(l1, gridBagConstraints);
			panel.add(l2, gridBagConstraints1);
			panel.add(l3, gridBagConstraints2);
			panel.add(l4, gridBagConstraints3);
			panel.add(l5, gridBagConstraints4);
			panel.add(l6, gridBagConstraints5);
			panel.add(l7, gridBagConstraints6);
			panel.add(getCode(), gridBagConstraints7);
			panel.add(getModel(), gridBagConstraints8);
			panel.add(getSites(), gridBagConstraints9);
			panel.add(getGamma(), gridBagConstraints10);
			panel.add(getNgen(), gridBagConstraints11);
			panel.add(getSample(), gridBagConstraints12);
			panel.add(getSubpanel(), gridBagConstraints13);
		}
		return panel;
	}

	/**
	 * This method initializes code	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getCode()
	{
		if (code == null)
		{
			code = new JComboBox();
		}
		return code;
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
		}
		return gamma;
	}

	/**
	 * This method initializes ngen	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JSpinner getNgen()
	{
		if (ngen == null)
		{
			ngen = new JSpinner();
		}
		return ngen;
	}

	/**
	 * This method initializes sample	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JSpinner getSample()
	{
		if (sample == null)
		{
			sample = new JSpinner();
		}
		return sample;
	}

	/**
	 * This method initializes subpanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getSubpanel()
	{
		if (subpanel == null)
		{
			GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
			gridBagConstraints15.gridx = 1;
			gridBagConstraints15.gridy = 0;
			l8 = new JLabel();
			l8.setText("%");
			GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
			gridBagConstraints14.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints14.weightx = 1.0;
			subpanel = new JPanel();
			subpanel.setLayout(new GridBagLayout());
			subpanel.add(getBurnin(), gridBagConstraints14);
			subpanel.add(l8, gridBagConstraints15);
		}
		return subpanel;
	}

	/**
	 * This method initializes burnin	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JSpinner getBurnin()
	{
		if (burnin == null)
		{
			burnin = new JSpinner();
		}
		return burnin;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
