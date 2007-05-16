// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;

import javax.swing.*;

import topali.data.SequenceSet;
import topali.data.SequenceSetParams;

public class ModelDialog extends JDialog
{

	private JPanel jContentPane = null;

	private JPanel panel = null;

	private JLabel l1 = null;

	private JLabel l2 = null;

	private JComboBox cbCode = null;

	private JComboBox cbModel = null;

	private JLabel l3 = null;

	private JCheckBox cbInv = null;

	private JCheckBox cbGamma = null;

	private JLabel l4 = null;

	private JPanel panel2 = null;

	private JButton bOk = null;

	private JButton bCancel = null;

	SequenceSet ss;
	SequenceSetParams para;
	
	/**
	 * @param owner
	 */
	public ModelDialog(Frame owner, SequenceSet ss)
	{
		super(owner);
		this.ss = ss;
		this.para = ss.getParams();
		initialize();
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(300, 200);
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane()
	{
		if (jContentPane == null)
		{
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getPanel(), BorderLayout.CENTER);
			jContentPane.add(getPanel2(), BorderLayout.SOUTH);
		}
		return jContentPane;
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
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 0;
			gridBagConstraints7.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints7.anchor = GridBagConstraints.EAST;
			gridBagConstraints7.gridy = 3;
			l4 = new JLabel();
			l4.setText("Gamma:");
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 1;
			gridBagConstraints6.anchor = GridBagConstraints.WEST;
			gridBagConstraints6.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints6.gridy = 3;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 1;
			gridBagConstraints5.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints5.anchor = GridBagConstraints.WEST;
			gridBagConstraints5.gridy = 2;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints4.anchor = GridBagConstraints.EAST;
			gridBagConstraints4.gridy = 2;
			l3 = new JLabel();
			l3.setText("Inv. Sites:");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.NONE;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.NONE;
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.weightx = 1.0;
			gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints2.anchor = GridBagConstraints.WEST;
			gridBagConstraints2.gridx = 1;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.weighty = 0.0;
			gridBagConstraints1.anchor = GridBagConstraints.EAST;
			gridBagConstraints1.gridy = 1;
			l2 = new JLabel();
			l2.setText("Subst. Model:");
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 0.0;
			gridBagConstraints.anchor = GridBagConstraints.EAST;
			gridBagConstraints.gridy = 0;
			l1 = new JLabel();
			l1.setText("Genetic Code:");
			panel = new JPanel();
			panel.setLayout(new GridBagLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			panel.add(l1, gridBagConstraints);
			panel.add(l2, gridBagConstraints1);
			panel.add(getCbCode(), gridBagConstraints2);
			panel.add(getCbModel(), gridBagConstraints3);
			panel.add(l3, gridBagConstraints4);
			panel.add(getCbInv(), gridBagConstraints5);
			panel.add(getCbGamma(), gridBagConstraints6);
			panel.add(l4, gridBagConstraints7);
		}
		return panel;
	}

	/**
	 * This method initializes cbCode	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getCbCode()
	{
		if (cbCode == null)
		{
			cbCode = new JComboBox(SequenceSetParams.availCodes);
			
			String code = para.getGeneticCode();
			for(int i=0; i<SequenceSetParams.availCodes.length; i++)
				if(SequenceSetParams.availCodes[i].equals(code)) {
					cbCode.setSelectedIndex(i);
					break;
				}
		}
		return cbCode;
	}

	/**
	 * This method initializes cbModel	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getCbModel()
	{
		if (cbModel == null)
		{
		
			String[] models = ss.isDNA() ?  SequenceSetParams.availDNAModels : SequenceSetParams.availAAModels;
			cbModel = new JComboBox(models);
			
			String model = para.getModel();
			for(int i=0; i<models.length; i++)
				if(models[i].equals(model)) {
					cbModel.setSelectedIndex(i);
					break;
				}
		}
		return cbModel;
	}

	/**
	 * This method initializes cbInv	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getCbInv()
	{
		if (cbInv == null)
		{
			cbInv = new JCheckBox();
			if(para.isModelInv())
				cbInv.setSelected(true);
		}
		return cbInv;
	}

	/**
	 * This method initializes cbGamma	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getCbGamma()
	{
		if (cbGamma == null)
		{
			cbGamma = new JCheckBox();
			if(para.isModelGamma())
				cbGamma.setSelected(true);
		}
		return cbGamma;
	}

	/**
	 * This method initializes panel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanel2()
	{
		if (panel2 == null)
		{
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridy = 0;
			gridBagConstraints9.insets = new Insets(4, 2, 4, 2);
			gridBagConstraints9.gridx = 0;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 1;
			gridBagConstraints8.insets = new Insets(4, 2, 4, 2);
			gridBagConstraints8.gridy = 0;
			panel2 = new JPanel();
			panel2.setLayout(new GridBagLayout());
			panel2.add(getBOk(), gridBagConstraints9);
			panel2.add(getBCancel(), gridBagConstraints8);
		}
		return panel2;
	}

	/**
	 * This method initializes bOk	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBOk()
	{
		if (bOk == null)
		{
			bOk = new JButton();
			bOk.setText("Ok");
			bOk.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					ss.getParams().setGeneticCode((String)cbCode.getSelectedItem());
					ss.getParams().setModel((String)cbModel.getSelectedItem());
					ss.getParams().setModelGamma(cbGamma.isSelected());
					ss.getParams().setModelInv(cbInv.isSelected());
					setVisible(false);
				}
			});
		}
		return bOk;
	}

	/**
	 * This method initializes bCancel	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBCancel()
	{
		if (bCancel == null)
		{
			bCancel = new JButton();
			bCancel.setText("Cancel");
			bCancel.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					setVisible(false);
				}
			});
		}
		return bCancel;
	}

	
}
