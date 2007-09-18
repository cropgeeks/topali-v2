// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.gui.*;

public class VamsasSessionDialog extends JDialog implements MouseListener
{
	private JPanel jContentPane = null;
	private JPanel foot = null;
	private JButton bok = null;
	private JButton bcanc = null;
	private JPanel head = null;
	private JLabel l1 = null;
	private JPanel center = null;
	private JList list = null;

	String[] sessions = null;
	String selSession = null;

	/**
	 * @param owner
	 */
	public VamsasSessionDialog(Frame owner, String[] sessions)
	{
		super(owner);
		this.setModal(true);
		this.sessions = sessions;
		initialize();
		setLocationRelativeTo(TOPALi.winMain);
	}

	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(237, 243);
		this.setTitle("Select Session");
		this.setContentPane(getJContentPane());
		getRootPane().setDefaultButton(bok);
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
			BorderLayout borderLayout = new BorderLayout();
			borderLayout.setHgap(0);
			borderLayout.setVgap(0);
			jContentPane = new JPanel();
			jContentPane.setLayout(borderLayout);
			jContentPane.add(getFoot(), BorderLayout.SOUTH);
			jContentPane.add(getHead(), BorderLayout.NORTH);
			jContentPane.add(getCenter(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes foot
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getFoot()
	{
		if (foot == null)
		{
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.insets = new Insets(3, 3, 3, 3);
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.insets = new Insets(3, 3, 3, 3);
			gridBagConstraints.gridy = 0;
			foot = new JPanel();
			foot.setLayout(new GridBagLayout());
			foot.add(getBok(), gridBagConstraints1);
			foot.add(getBcanc(), gridBagConstraints);
		}
		return foot;
	}

	/**
	 * This method initializes bok
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getBok()
	{
		if (bok == null)
		{
			bok = new JButton();
			bok.setText("OK");
			bok.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					action();
				}
			});
		}
		return bok;
	}

	/**
	 * This method initializes bcanc
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getBcanc()
	{
		if (bcanc == null)
		{
			bcanc = new JButton();
			bcanc.setText("Cancel");
			bcanc.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					dispose();
				}
			});
		}
		return bcanc;
	}

	/**
	 * This method initializes head
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getHead()
	{
		if (head == null)
		{
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.insets = new Insets(5, 5, 3, 3);
			l1 = new JLabel();
			l1.setText("Available VAMSAS sessions:");
			head = new JPanel();
			head.setLayout(new GridBagLayout());
			head.add(l1, gridBagConstraints3);
		}
		return head;
	}

	/**
	 * This method initializes center
	 *
	 * @return javax.swing.JPanel
	 */
	private JPanel getCenter()
	{
		if (center == null)
		{
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.BOTH;
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.weightx = 1.0;
			gridBagConstraints2.weighty = 1.0;
			gridBagConstraints2.insets = new Insets(3, 3, 3, 3);
			gridBagConstraints2.gridx = 0;
			center = new JPanel();
			center.setLayout(new GridBagLayout());
			center.add(new JScrollPane(getList()), gridBagConstraints2);
		}
		return center;
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
			list = new JList(sessions);
			list.setSelectedIndex(sessions.length-1);
			list.addMouseListener(this);
		}
		return list;
	}

	public String getSelSession() {
		return selSession;
	}

	public void action() {
		selSession = (String)list.getSelectedValue();
		setVisible(false);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if(e.getClickCount()>1) {
			action();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}


}
