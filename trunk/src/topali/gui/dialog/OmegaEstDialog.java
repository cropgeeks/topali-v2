// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;

public class OmegaEstDialog extends JDialog implements ActionListener
{

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JPanel north = null;

	private JPanel center = null;

	private JLabel label1 = null;

	private JLabel lStart = null;

	private JLabel lEnd = null;

	private JLabel lStep = null;

	private JTextField fStart = null;

	private JTextField fEnd = null;

	private JTextField fStepsize = null;

	private JPanel south = null;

	private JButton bOk = null;

	private JButton bCancel = null;

	private JLabel lRuns = null;

	private JLabel runs = null;

	double start, end, stepsize;
	boolean canceled = false;
	
	/**
	 * @param owner
	 */
	public OmegaEstDialog(JDialog owner, double start, double end, double stepsize)
	{
		super(owner, true);
		this.start = start;
		this.end = end;
		this.stepsize = stepsize;
		initialize();
		calcRuns();
		setLocationRelativeTo(owner);
	}

	private void calcRuns() {
		if(fStart==null || fEnd==null || fStepsize==null)
			return;
		
		try
		{
			double a = Double.parseDouble(fStart.getText());
			double b = Double.parseDouble(fEnd.getText());
			double c = Double.parseDouble(fStepsize.getText());
			int runs = (int)(Math.log10(b/a)/Math.log10(c)) + 1;
			if(runs==Double.MAX_VALUE || runs < 1)
				this.runs.setText("---");
			else
				this.runs.setText(String.valueOf(runs));
		} catch (Exception e)
		{
			this.runs.setText(String.valueOf(runs));
		}
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(233, 152);
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
			jContentPane.add(getNorth(), BorderLayout.NORTH);
			jContentPane.add(getCenter(), BorderLayout.CENTER);
			jContentPane.add(getSouth(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	/**
	 * This method initializes north	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getNorth()
	{
		if (north == null)
		{
			label1 = new JLabel();
			label1.setText("Omega estimation");
			north = new JPanel();
			north.setLayout(new GridBagLayout());
			north.add(label1, new GridBagConstraints());
		}
		return north;
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
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 3;
			gridBagConstraints9.gridy = 1;
			runs = new JLabel();
			runs.setText("= ");
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 3;
			gridBagConstraints8.weightx = 0.25;
			gridBagConstraints8.gridy = 0;
			lRuns = new JLabel();
			lRuns.setText("Runs:");
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints5.gridy = 1;
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.gridx = 2;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints4.gridy = 1;
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.gridx = 1;
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.gridx = 0;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 2;
			gridBagConstraints2.weightx = 0.25;
			gridBagConstraints2.gridy = 0;
			lStep = new JLabel();
			lStep.setText("Inc.:");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 1;
			gridBagConstraints1.weightx = 0.25;
			gridBagConstraints1.gridy = 0;
			lEnd = new JLabel();
			lEnd.setText("End:");
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.weightx = 0.25;
			gridBagConstraints.ipadx = 0;
			gridBagConstraints.ipady = 2;
			gridBagConstraints.gridy = 0;
			lStart = new JLabel();
			lStart.setText("Start:");
			center = new JPanel();
			center.setLayout(new GridBagLayout());
			center.add(lStart, gridBagConstraints);
			center.add(lEnd, gridBagConstraints1);
			center.add(lStep, gridBagConstraints2);
			center.add(getFStart(), gridBagConstraints3);
			center.add(getFEnd(), gridBagConstraints4);
			center.add(getFStepsize(), gridBagConstraints5);
			center.add(lRuns, gridBagConstraints8);
			center.add(runs, gridBagConstraints9);
		}
		return center;
	}

	/**
	 * This method initializes fStart	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getFStart()
	{
		if (fStart == null)
		{
			fStart = new JTextField(new tfDocument(), String.valueOf(start), 3);
		}
		return fStart;
	}

	/**
	 * This method initializes fEnd	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getFEnd()
	{
		if (fEnd == null)
		{
			fEnd = new JTextField(new tfDocument(), String.valueOf(end), 3);
		}
		return fEnd;
	}

	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getFStepsize()
	{
		if (fStepsize == null)
		{
			fStepsize = new JTextField(new tfDocument(), String.valueOf(stepsize), 3);
		}
		return fStepsize;
	}

	/**
	 * This method initializes south	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getSouth()
	{
		if (south == null)
		{
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.insets = new Insets(0, 0, 0, 5);
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 1;
			gridBagConstraints6.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints6.gridy = 0;
			south = new JPanel();
			south.setLayout(new GridBagLayout());
			south.add(getBOk(), gridBagConstraints7);
			south.add(getBCancel(), gridBagConstraints6);
		}
		return south;
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
			bOk.addActionListener(this);
			bOk.setText("Ok");
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
			bCancel.addActionListener(this);
			bCancel.setText("Cancel");
		}
		return bCancel;
	}

	public void actionPerformed(ActionEvent e)
	{
		 if(e.getSource().equals(bCancel)) {
			canceled = true;
			this.setVisible(false);
		}
		
		else if(e.getSource().equals(bOk)) {
			start = Double.parseDouble(fStart.getText());
			end = Double.parseDouble(fEnd.getText());
			stepsize = Double.parseDouble(fStepsize.getText());
			this.setVisible(false);
		}
	}

	public boolean wasCanceled() {
		return canceled;
	}
	
	public double getEnd()
	{
		return end;
	}

	public double getStart()
	{
		return start;
	}

	public double getStepsize()
	{
		return stepsize;
	}
	
	class tfDocument extends PlainDocument {

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException
		{
			super.insertString(offs, str, a);
			calcRuns();
		}
		
	}
}
