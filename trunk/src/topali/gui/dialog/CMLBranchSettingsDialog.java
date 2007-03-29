// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import pal.alignment.Alignment;
import pal.distance.JukesCantorDistanceMatrix;
import pal.tree.NeighborJoiningTree;
import pal.tree.Tree;
import topali.data.*;
import topali.gui.*;
import topali.gui.atv.ATV;
import topali.var.NHTreeUtils;
import topali.var.Utils;

public class CMLBranchSettingsDialog extends JDialog implements WindowListener
{
	WinMain winMain;
	
	AlignmentData data;
	DefaultListModel listModel = new DefaultListModel();
	String tree;
	ATV atv;
	
	private JPanel jContentPane = null;

	private JPanel pSouth = null;

	private JButton bRun = null;

	private JButton bCancel = null;

	private JPanel pEast = null;

	private JButton bAdd = null;

	private JButton bRemove = null;

	private JPanel pNorth = null;

	private JLabel jLabel = null;

	private JPanel pCenter = null;

	private JList lHypos = null;

	private JButton bHelp = null;

	private CodeMLResult result;
	
	public CMLBranchSettingsDialog(WinMain winMain, AlignmentData data, CodeMLResult result) {
		super(winMain, "Positive Selection - Branch Models", false);
		this.winMain = winMain;
		this.data = data;
		
		Alignment alignment = data.getSequenceSet().getAlignment(false);
		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
		Tree tree = new NeighborJoiningTree(dm);
		this.tree = tree.toString();
		this.tree = this.tree.replaceAll(";", "");
		this.tree = NHTreeUtils.removeBranchLengths(this.tree);
		listModel.addElement(this.tree);
		
		if(result!=null) {
			for(int i=1; i<result.hypos.size(); i++)
				listModel.addElement(result.hypos.get(i).tree);
			this.result = result;
		}
		else
			this.result = new CodeMLResult(CodeMLResult.TYPE_BRANCHMODEL);
		
		this.setSize(400, 300);
		this.setContentPane(getJContentPane());
		
		setLocationRelativeTo(winMain);
	}
	
	private void runAction(ActionEvent e) {
		setVisible(false);
		
		SequenceSet ss = data.getSequenceSet();
		
		if (Prefs.isWindows)
			result.codemlPath = Utils.getLocalPath() + "codeml.exe";
		else
			result.codemlPath = Utils.getLocalPath() + "codeml/codeml";

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = ((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
		
		result.hypos.clear();
		int size =  listModel.getSize();
		for(int i=0; i<size; i++) {
			String tree = (String)listModel.getElementAt(i);
			CMLHypothesis hypo = new CMLHypothesis();
			hypo.tree = tree;
			if(i==0)
				hypo.model = 0;
			else
				hypo.model = 2;
			result.hypos.add(hypo);
		}
		
		int runNum = data.getTracker().getCodeMLRunCount() + 1;
		data.getTracker().setCodeMLRunCount(runNum);
		result.guiName = "CodeML Result " + runNum;
		result.jobName = "CodeML Analysis " + runNum + " on " + data.name
				+ " (" + ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";

		winMain.submitJob(data, result);
	}
	
	private void cancelAction() {
		setVisible(false);
	}

	private void addAction() {		
		atv = new ATV(this.tree, data.name, winMain, this);
		SwingUtilities.invokeLater(atv);
	}
	
	private void removeAction() {
		int i = lHypos.getSelectedIndex();
		if(i>=1)
			listModel.remove(i);
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
			jContentPane.add(getPEast(), BorderLayout.EAST);
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
	 * This method initializes pEast	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPEast()
	{
		if (pEast == null)
		{
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.insets = new Insets(10, 0, 0, 0);
			gridBagConstraints2.gridy = 1;
			pEast = new JPanel();
			pEast.setLayout(new GridBagLayout());
			pEast.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			pEast.add(getBAdd(), new GridBagConstraints());
			pEast.add(getBRemove(), gridBagConstraints2);
		}
		return pEast;
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
					addAction();
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
					removeAction();
				}
			});
		}
		return bRemove;
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
			jLabel.setText("Add hypothesis:");
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
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = GridBagConstraints.BOTH;
			gridBagConstraints4.gridy = 0;
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.weighty = 1.0;
			gridBagConstraints4.gridx = 0;
			pCenter = new JPanel();
			pCenter.setLayout(new GridBagLayout());
			pCenter.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			pCenter.add(new JScrollPane(getLHypos()), gridBagConstraints4);
		}
		return pCenter;
	}

	/**
	 * This method initializes lHypos	
	 * 	
	 * @return javax.swing.JList	
	 */
	private JList getLHypos()
	{
		if (lHypos == null)
		{
			lHypos = new JList(listModel);
		}
		return lHypos;
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
			bHelp = TOPALiHelp.getHelpButton("cmlbranch_settings");
		}
		return bHelp;
	}

	public void windowActivated(WindowEvent e)
	{	
	}

	public void windowClosed(WindowEvent e)
	{	
		String tree = atv.getTree();
		tree = tree.replaceAll("_#", " #");
		//tree = NHTreeUtils.removeBranchLengths(tree);
		if(!listModel.contains(tree))
			listModel.addElement(tree);
	}

	public void windowClosing(WindowEvent e)
	{	
	}

	public void windowDeactivated(WindowEvent e)
	{	
	}

	public void windowDeiconified(WindowEvent e)
	{	
	}

	public void windowIconified(WindowEvent e)
	{	
	}

	public void windowOpened(WindowEvent e)
	{
	}
	
	
}
