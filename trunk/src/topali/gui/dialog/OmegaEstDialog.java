// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;

import javax.swing.*;

import doe.MsgBox;

import topali.data.CMLModel;

/**
 * Dialog for selecting omega start values for the codeml site models
 */
public class OmegaEstDialog extends JDialog
{

	private JPanel jContentPane = null;

	private JPanel p1 = null;

	private JPanel p2 = null;

	private JPanel p3 = null;

	private JPanel p4 = null;

	private JLabel l1 = null;

	private JScrollPane sp = null;

	private JList list = null;

	private JButton bAdd = null;

	private JButton bRemove = null;

	private JButton bOk = null;

	private JButton bCancel = null;

	private DefaultListModel listModel;
	
	CMLModel model;
	
	/**
	 * @param owner
	 */
	public OmegaEstDialog(Frame owner, CMLModel model)
	{
		super(owner);
		this.model = model;
		
		listModel = new DefaultListModel();
		for(Double d : model.wStart)
			listModel.addElement(d);
		
		initialize();
		
		setLocationRelativeTo(MsgBox.frm);
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(200, 200);
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
			jContentPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			jContentPane.add(getP1(), BorderLayout.NORTH);
			jContentPane.add(getP2(), BorderLayout.CENTER);
			jContentPane.add(getP3(), BorderLayout.EAST);
			jContentPane.add(getP4(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	/**
	 * This method initializes p1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getP1()
	{
		if (p1 == null)
		{
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.weightx = 1.0;
			l1 = new JLabel();
			l1.setText("Omega start values:");
			p1 = new JPanel();
			p1.setLayout(new GridBagLayout());
			p1.add(l1, gridBagConstraints);
		}
		return p1;
	}

	/**
	 * This method initializes p2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getP2()
	{
		if (p2 == null)
		{
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.BOTH;
			gridBagConstraints1.weighty = 1.0;
			gridBagConstraints1.weightx = 1.0;
			p2 = new JPanel();
			p2.setLayout(new GridBagLayout());
			p2.add(getSp(), gridBagConstraints1);
		}
		return p2;
	}

	/**
	 * This method initializes p3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getP3()
	{
		if (p3 == null)
		{
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints3.gridy = 1;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = -1;
			gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints2.gridy = -1;
			p3 = new JPanel();
			p3.setLayout(new GridBagLayout());
			p3.add(getBAdd(), gridBagConstraints2);
			p3.add(getBRemove(), gridBagConstraints3);
		}
		return p3;
	}

	/**
	 * This method initializes p4	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getP4()
	{
		if (p4 == null)
		{
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.insets = new Insets(2, 2, 2, 2);
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.insets = new Insets(2, 2, 2, 2);
			p4 = new JPanel();
			p4.setLayout(new GridBagLayout());
			p4.add(getBOk(), gridBagConstraints4);
			p4.add(getBCancel(), gridBagConstraints5);
		}
		return p4;
	}

	/**
	 * This method initializes sp	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getSp()
	{
		if (sp == null)
		{
			sp = new JScrollPane();
			sp.setViewportView(getList());
		}
		return sp;
	}

	/**
	 * This method initializes list	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getList()
	{
		if (list == null)
		{
			list = new JList(listModel);
		}
		return list;
	}

	/**
	 * This method initializes bAdd	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBAdd()
	{
		if (bAdd == null)
		{
			bAdd = new JButton();
			bAdd.setText("+");
			bAdd.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					if(listModel.getSize()>10) {
						JOptionPane.showMessageDialog(null, "Sorry, max. 10 values allowed", "Error", JOptionPane.WARNING_MESSAGE);
						return;
					}
					
					String s = JOptionPane.showInputDialog(null, "New omega start value:");
					if(s==null || s.trim().equals("")) //cancel button
						return;
						
					try
					{
						Double w = Double.parseDouble(s);
						if(!listModel.contains(w))
							listModel.addElement(w);
					} catch (NumberFormatException e1)
					{
						JOptionPane.showMessageDialog(null, "Please enter a number.", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
		}
		return bAdd;
	}

	/**
	 * This method initializes bRemove	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBRemove()
	{
		if (bRemove == null)
		{
			bRemove = new JButton();
			bRemove.setText("-");
			bRemove.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					int i = list.getSelectedIndex();
					if(i>=0 && i<listModel.getSize())
						listModel.remove(i);
				}
			});
		}
		return bRemove;
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
					model.wStart.clear();
					for(int i=0; i<listModel.getSize(); i++)
						model.wStart.add((Double)listModel.get(i));
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
