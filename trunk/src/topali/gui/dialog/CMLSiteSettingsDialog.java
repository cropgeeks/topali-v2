// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.Utils;

public class CMLSiteSettingsDialog extends JDialog implements MouseListener
{

	private AlignmentData data;

	private CodeMLResult result = null;

	WinMain winMain;

	private JPanel jContentPane = null;

	private JPanel pSouth = null;

	private JButton bRun = null;

	private JButton bCancel = null;

	private JPanel pNorth = null;

	private JLabel jLabel = null;

	private JPanel pCenter = null;

	private JButton bHelp = null;

	private JCheckBox cM0 = null;

	private JLabel lM0 = null;

	private JCheckBox cM1 = null;

	private JCheckBox cM2 = null;

	private JCheckBox cM1a = null;

	private JCheckBox cM2a = null;

	private JCheckBox cM3 = null;

	private JCheckBox cM7 = null;

	private JCheckBox cM8 = null;

	private JLabel lM1 = null;

	private JLabel lM2 = null;

	private JLabel lM1a = null;

	private JLabel lM2a = null;

	private JLabel lM3 = null;

	private JLabel lM7 = null;

	private JLabel lM8 = null;

	CMLModel m0, m1, m2, m1a, m2a, m3, m7, m8;
	
	Vector<CMLModel> models = new Vector<CMLModel>();
	
	/**
	 * @param owner
	 */
	public CMLSiteSettingsDialog(WinMain winMain, AlignmentData data, CodeMLResult res)
	{
		super(winMain, "Positive Selection - Site Models", true);
		this.data = data;
		this.winMain = winMain;
		initModels();
		initialize();
		
		if(res!=null)
			initPreviousResult(res);
		
		setModal(false);
		setLocationRelativeTo(winMain);
	}

	private void initPreviousResult(CodeMLResult res) {
		cM0.setSelected(false);
		cM1.setSelected(false);
		cM2.setSelected(false);
		cM1a.setSelected(false);
		cM2a.setSelected(false);
		cM3.setSelected(false);
		cM7.setSelected(false);
		cM8.setSelected(false);
		
		for(CMLModel m : res.models) {
			if(m.abbr.equals(m0.abbr)) {
				cM0.setSelected(true);
				m0 = m;
			}
			if(m.abbr.equals(m1.abbr)) {
				cM1.setSelected(true);
				m1 = m;
			}
			if(m.abbr.equals(m2.abbr)) {
				cM2.setSelected(true);
				m2 = m;
			}
			if(m.abbr.equals(m1a.abbr)) {
				cM1a.setSelected(true);
				m1a = m;
			}
			if(m.abbr.equals(m2a.abbr)) {
				cM2a.setSelected(true);
				m2a = m;
			}
			if(m.abbr.equals(m3.abbr)) {
				cM3.setSelected(true);
				m3 = m;
			}
			if(m.abbr.equals(m7.abbr)) {
				cM7.setSelected(true);
				m7 = m;
			}
			if(m.abbr.equals(m8.abbr)) {
				cM8.setSelected(true);
				m8 = m;
			}
		}
	}
	
	private void initModels() {
		m0 = new CMLModel(CMLModel.MODEL_M0);
		m1 = new CMLModel(CMLModel.MODEL_M1);
		m2 = new CMLModel(CMLModel.MODEL_M2);
		m1a = new CMLModel(CMLModel.MODEL_M1a);
		m2a = new CMLModel(CMLModel.MODEL_M2a);
		m3 = new CMLModel(CMLModel.MODEL_M3);
		m7 = new CMLModel(CMLModel.MODEL_M7);
		m8 = new CMLModel(CMLModel.MODEL_M8);
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize()
	{
		this.setSize(377, 296);
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
			jContentPane.add(getPSouth(), BorderLayout.SOUTH);
			jContentPane.add(getPNorth(), BorderLayout.NORTH);
			jContentPane.add(getPCenter(), BorderLayout.CENTER);
		}
		return jContentPane;
	}

	/**
	 * This method initializes pSouth
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getPSouth()
	{
		if (pSouth == null)
		{
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 2;
			gridBagConstraints11.anchor = GridBagConstraints.EAST;
			gridBagConstraints11.weightx = 0.3;
			gridBagConstraints11.gridy = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.weightx = 0.3;
			gridBagConstraints1.anchor = GridBagConstraints.EAST;
			gridBagConstraints1.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints1.ipadx = 0;
			gridBagConstraints1.ipady = 0;
			gridBagConstraints1.weighty = 0.0;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.weightx = 0.3;
			gridBagConstraints.weighty = 0.0;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints.gridy = 0;
			pSouth = new JPanel();
			pSouth.setLayout(new GridBagLayout());
			pSouth.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			pSouth.add(getBRun(), gridBagConstraints1);
			pSouth.add(getBCancel(), gridBagConstraints);
			pSouth.add(getBHelp(), gridBagConstraints11);
		}
		return pSouth;
	}

	/**
	 * This method initializes bRun
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBRun()
	{
		if (bRun == null)
		{
			bRun = new JButton();
			bRun.setText("Run");
			bRun.addActionListener(new java.awt.event.ActionListener()
			{
				public void actionPerformed(java.awt.event.ActionEvent e)
				{
					runAction(e);
				}
			});
		}
		return bRun;
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
					cancelAction();
				}
			});
		}
		return bCancel;
	}

	/**
	 * This method initializes pNorth
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getPNorth()
	{
		if (pNorth == null)
		{
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			jLabel = new JLabel();
			jLabel.setText("Select Models:");
			pNorth = new JPanel();
			pNorth.setLayout(new GridBagLayout());
			pNorth.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			pNorth.add(jLabel, gridBagConstraints3);
		}
		return pNorth;
	}

	/**
	 * This method initializes pCenter
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getPCenter()
	{
		if (pCenter == null)
		{
			GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
			gridBagConstraints19.gridx = 1;
			gridBagConstraints19.anchor = GridBagConstraints.WEST;
			gridBagConstraints19.gridy = 7;
			lM8 = new JLabel();
			lM8.setText("<html><u>M8 (beta&w>1 (11 categories))</u></html>");
			lM8.addMouseListener(this);
			lM8.setForeground(Color.blue);
			GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
			gridBagConstraints18.gridx = 1;
			gridBagConstraints18.anchor = GridBagConstraints.WEST;
			gridBagConstraints18.gridy = 6;
			lM7 = new JLabel();
			lM7.setText("<html><u>M7 (beta (10 categories))</u></html>");
			lM7.addMouseListener(this);
			lM7.setForeground(Color.blue);
			GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
			gridBagConstraints17.gridx = 1;
			gridBagConstraints17.anchor = GridBagConstraints.WEST;
			gridBagConstraints17.gridy = 5;
			lM3 = new JLabel();
			lM3.setText("<html><u>M3 (Discrete (3 categories))</u></html>");
			lM3.addMouseListener(this);
			lM3.setForeground(Color.blue);
			GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
			gridBagConstraints16.gridx = 1;
			gridBagConstraints16.anchor = GridBagConstraints.WEST;
			gridBagConstraints16.gridy = 4;
			lM2a = new JLabel();
			lM2a.setText("<html><u>M2a (Positive Selection)</u></html>");
			lM2a.addMouseListener(this);
			lM2a.setForeground(Color.blue);
			GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
			gridBagConstraints15.gridx = 1;
			gridBagConstraints15.anchor = GridBagConstraints.WEST;
			gridBagConstraints15.gridy = 3;
			lM1a = new JLabel();
			lM1a.setText("<html><u>M1a (Nearly Neutral)</u></html>");
			lM1a.addMouseListener(this);
			lM1a.setForeground(Color.blue);
			GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
			gridBagConstraints14.gridx = 1;
			gridBagConstraints14.anchor = GridBagConstraints.WEST;
			gridBagConstraints14.gridy = 2;
			lM2 = new JLabel();
			lM2.setText("M2 (Positive Seleciton)");
			GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
			gridBagConstraints13.gridx = 1;
			gridBagConstraints13.anchor = GridBagConstraints.WEST;
			gridBagConstraints13.gridy = 1;
			lM1 = new JLabel();
			lM1.setText("M1 (Nearly Neutral)");
			GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
			gridBagConstraints12.gridx = 0;
			gridBagConstraints12.gridy = 7;
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.gridx = 0;
			gridBagConstraints10.gridy = 6;
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 0;
			gridBagConstraints9.gridy = 5;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 0;
			gridBagConstraints8.gridy = 4;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 0;
			gridBagConstraints7.gridy = 3;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.gridy = 2;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.gridy = 1;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 1;
			gridBagConstraints4.anchor = GridBagConstraints.WEST;
			gridBagConstraints4.gridy = 0;
			lM0 = new JLabel();
			lM0.setText("<html><u>M0 (one ratio)</u></html>");
			lM0.addMouseListener(this);
			lM0.setForeground(Color.blue);
			lM0.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.gridy = 0;
			pCenter = new JPanel();
			pCenter.setLayout(new GridBagLayout());
			pCenter.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			pCenter.add(getCM0(), gridBagConstraints2);
			pCenter.add(lM0, gridBagConstraints4);
			pCenter.add(getCM1(), gridBagConstraints5);
			pCenter.add(getCM2(), gridBagConstraints6);
			pCenter.add(getCM1a(), gridBagConstraints7);
			pCenter.add(getCM2a(), gridBagConstraints8);
			pCenter.add(getCM3(), gridBagConstraints9);
			pCenter.add(getCM7(), gridBagConstraints10);
			pCenter.add(getCM8(), gridBagConstraints12);
			pCenter.add(lM1, gridBagConstraints13);
			pCenter.add(lM2, gridBagConstraints14);
			pCenter.add(lM1a, gridBagConstraints15);
			pCenter.add(lM2a, gridBagConstraints16);
			pCenter.add(lM3, gridBagConstraints17);
			pCenter.add(lM7, gridBagConstraints18);
			pCenter.add(lM8, gridBagConstraints19);
		}
		return pCenter;
	}

	/**
	 * This method initializes bHelp
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getBHelp()
	{
		if (bHelp == null)
		{
			bHelp = TOPALiHelp.getHelpButton("cmlsite_settings");
		}
		return bHelp;
	}

	/**
	 * This method initializes cM0
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM0()
	{
		if (cM0 == null)
		{
			cM0 = new JCheckBox();
			cM0.setSelected(true);
		}
		return cM0;
	}

	/**
	 * This method initializes cM1
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM1()
	{
		if (cM1 == null)
		{
			cM1 = new JCheckBox();
		}
		return cM1;
	}

	/**
	 * This method initializes cM2
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM2()
	{
		if (cM2 == null)
		{
			cM2 = new JCheckBox();
		}
		return cM2;
	}

	/**
	 * This method initializes cM1a
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM1a()
	{
		if (cM1a == null)
		{
			cM1a = new JCheckBox();
			cM1a.setSelected(true);
		}
		return cM1a;
	}

	/**
	 * This method initializes cM2a
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM2a()
	{
		if (cM2a == null)
		{
			cM2a = new JCheckBox();
			cM2a.setSelected(true);
		}
		return cM2a;
	}

	/**
	 * This method initializes cM3
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM3()
	{
		if (cM3 == null)
		{
			cM3 = new JCheckBox();
		}
		return cM3;
	}

	/**
	 * This method initializes cM7
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM7()
	{
		if (cM7 == null)
		{
			cM7 = new JCheckBox();
			cM7.setSelected(true);
		}
		return cM7;
	}

	/**
	 * This method initializes cM8
	 * 
	 * @return javax.swing.JCheckBox
	 */
	private JCheckBox getCM8()
	{
		if (cM8 == null)
		{
			cM8 = new JCheckBox();
			cM8.setSelected(true);
		}
		return cM8;
	}

	private void runAction(ActionEvent e) {
		setVisible(false);
		
		SequenceSet ss = data.getSequenceSet();

		result = new CodeMLResult(CodeMLResult.TYPE_SITEMODEL);

		if (Prefs.isWindows)
			result.codemlPath = Utils.getLocalPath() + "codeml.exe";
		else
			result.codemlPath = Utils.getLocalPath() + "codeml/codeml";

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = ((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);

		if(cM0.isSelected())
			result.models.addAll(m0.generateModels());
		if(cM1.isSelected())
			result.models.addAll(m1.generateModels());
		if(cM2.isSelected())
			result.models.addAll(m2.generateModels());
		if(cM1a.isSelected())
			result.models.addAll(m1a.generateModels());
		if(cM2a.isSelected())
			result.models.addAll(m2a.generateModels());
		if(cM3.isSelected())
			result.models.addAll(m3.generateModels());
		if(cM7.isSelected())
			result.models.addAll(m7.generateModels());
		if(cM8.isSelected())
			result.models.addAll(m8.generateModels());
		
		int runNum = data.getTracker().getCodeMLRunCount() + 1;
		data.getTracker().setCodeMLRunCount(runNum);
		result.guiName = "CodeML Result " + runNum;
		result.jobName = "CodeML Analysis " + runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		winMain.submitJob(data, result);
	}
	
	private void cancelAction() {
		this.setVisible(false);
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource().equals(lM0))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m0);
			d.setVisible(true);
		} else if (e.getSource().equals(lM1a))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m1a);
			d.setVisible(true);
		} else if (e.getSource().equals(lM2a))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m2a);
			d.setVisible(true);
		} else if (e.getSource().equals(lM3))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m3);
			d.setVisible(true);
		} else if (e.getSource().equals(lM7))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m7);
			d.setVisible(true);
		} else if (e.getSource().equals(lM8))
		{
			OmegaEstDialog d = new OmegaEstDialog(null, m8);
			d.setVisible(true);
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.HAND_CURSOR));

	}

	public void mouseExited(MouseEvent e)
	{
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	
}
