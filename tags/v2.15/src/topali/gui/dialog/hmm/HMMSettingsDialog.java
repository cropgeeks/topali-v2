// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.hmm;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import doe.*;

import topali.data.*;
import topali.gui.*;

public class HMMSettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;
	private JButton bRun, bCancel, bDefault, bHelp;
	private BasicPanel basicPanel;
	private MosaicPanel mosaicPanel;
	private ModelPanel modelPanel;
	private RunPanel runPanel;
	
	private AlignmentData data;
	private HMMResult result = null;

	public HMMSettingsDialog(WinMain winMain, AlignmentData data, HMMResult iResult)
	{
		super(winMain, "Hidden Markov Model - Confirm Settings", true);
		this.data = data;
		
		if (iResult != null)
			setInitialSettings(iResult);
		
		bRun = new JButton("Run");
		bRun.addActionListener(this);
		bCancel = new JButton("Cancel");
		bCancel.addActionListener(this);
		bDefault = new JButton("Defaults");
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("hmm_settings");
		
		tabs = new JTabbedPane();
		addTabs(iResult);

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
//				basicPanel.stepSpin.requestFocus();
			}
		});
		
		JPanel p1 = new JPanel(new GridLayout(1, 4, 5, 5));
		p1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		p1.add(bRun);
		p1.add(bDefault);
		p1.add(bCancel);
		p1.add(bHelp);
		JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		p2.add(p1);
		
		getContentPane().add(tabs, BorderLayout.CENTER);
		getContentPane().add(p2, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);
				
		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}
	
	private void addTabs(HMMResult iResult)
	{
		basicPanel = new BasicPanel();
		modelPanel = new ModelPanel();
		mosaicPanel = new MosaicPanel(data, iResult);
		runPanel = new RunPanel();
		
		tabs.removeAll();
		tabs.addTab("Basic", basicPanel);
		tabs.addTab("Topology Structure", mosaicPanel);
		tabs.addTab("Model Settings", modelPanel);
		tabs.addTab("Run Settings", runPanel);
	}	

	public HMMResult getHMMResult()
		{ return result; }
	
	private void createHMMObject(boolean makeRemote)
	{
		result = new HMMResult();
		
		if (Prefs.isWindows)
			result.barcePath = Utils.getLocalPath() + "barce.exe";
		else
			result.barcePath = Utils.getLocalPath() + "barce/barce";
		
		result.selectedSeqs = data.getSequenceSet().getSelectedSequenceSafeNames();
		result.bpArray = mosaicPanel.getBreakpointArray();
		result.isRemote = makeRemote;
		
		result.hmm_model = Prefs.hmm_model;
		result.hmm_initial = Prefs.hmm_initial;
		result.hmm_freq_est_1 = Prefs.hmm_freq_est_1;
		result.hmm_freq_est_2 = Prefs.hmm_freq_est_2;
		result.hmm_freq_est_3 = Prefs.hmm_freq_est_3;
		result.hmm_freq_est_4 = Prefs.hmm_freq_est_4;
		result.hmm_transition = Prefs.hmm_transition;
		result.hmm_transition_ratio = Prefs.hmm_transition_ratio;
		result.hmm_freq_1 = Prefs.hmm_freq_1;
		result.hmm_freq_2 = Prefs.hmm_freq_2;
		result.hmm_freq_3 = Prefs.hmm_freq_3;
		result.hmm_difficulty = Prefs.hmm_difficulty;
		
		result.hmm_burn = Prefs.hmm_burn;
		result.hmm_points = Prefs.hmm_points;
		result.hmm_thinning = Prefs.hmm_thinning;
		result.hmm_tuning = Prefs.hmm_tuning;
		result.hmm_lambda = Prefs.hmm_lambda;
		result.hmm_annealing = Prefs.hmm_annealing;
		result.hmm_station = Prefs.hmm_station;
		result.hmm_update = Prefs.hmm_update;
		result.hmm_branch = Prefs.hmm_branch;
	}
	
	private void setInitialSettings(HMMResult iResult)
	{
		Prefs.hmm_model = iResult.hmm_model;
		Prefs.hmm_initial = iResult.hmm_initial;
		Prefs.hmm_freq_est_1 = iResult.hmm_freq_est_1;
		Prefs.hmm_freq_est_2 = iResult.hmm_freq_est_2;
		Prefs.hmm_freq_est_3 = iResult.hmm_freq_est_3;
		Prefs.hmm_freq_est_4 = iResult.hmm_freq_est_4;
		Prefs.hmm_transition = iResult.hmm_transition;
		Prefs.hmm_transition_ratio = iResult.hmm_transition_ratio;
		Prefs.hmm_freq_1 = iResult.hmm_freq_1;
		Prefs.hmm_freq_2 = iResult.hmm_freq_2;
		Prefs.hmm_freq_3 = iResult.hmm_freq_3;
		Prefs.hmm_difficulty = iResult.hmm_difficulty;
		
		Prefs.hmm_burn = iResult.hmm_burn;
		Prefs.hmm_points = iResult.hmm_points;
		Prefs.hmm_thinning = iResult.hmm_thinning;
		Prefs.hmm_tuning = iResult.hmm_tuning;
		Prefs.hmm_lambda = iResult.hmm_lambda;
		Prefs.hmm_annealing = iResult.hmm_annealing;
		Prefs.hmm_station = iResult.hmm_station;
		Prefs.hmm_update = iResult.hmm_update;
		Prefs.hmm_branch = iResult.hmm_branch;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bRun)
		{
			if (modelPanel.saveSettings() == false)
				return;
			if (runPanel.saveSettings() == false)
				return;
		
			createHMMObject((e.getModifiers() & e.CTRL_MASK) == 0);
			setVisible(false);
		}
		
		else if (e.getSource() == bDefault)
			defaultClicked();
	}
		
	private void defaultClicked()
	{
		int res = MsgBox.yesno("This will return all settings (on all tabs) to their default "
			+ "values. Continue?", 1);
		if (res != JOptionPane.YES_OPTION)
			return;
	
		// Tell the preferences object to reset them
		Prefs.setHMMDefaults();
	
		// Now recreate the panels with these new values
		int index = tabs.getSelectedIndex();
		addTabs(null);
		tabs.setSelectedIndex(index);
	}
	
	static class BasicPanel extends JPanel
	{
		BasicPanel()
		{		
			DoeLayout layout = new DoeLayout();		
			add(layout.getPanel(), BorderLayout.NORTH);

			JLabel info1 = new JLabel("Please confirm the current settings for "
				+ "running HMM. Additional configuration is also available");
			JLabel info2 = new JLabel("<html>by selecting the <b>Topology</b>, "
				+ "<b>Model</b> or <b>Run</b> tabs and modifying the options "
				+ "found there.</html>");
				
			layout.add(info1, 0, 0, 1, 5, new Insets(5, 5, 2, 5));
			layout.add(info2, 0, 1, 1, 5, new Insets(0, 5, 10, 5));
			
			layout.add(new JLabel(" "), 0, 2, 1, 4, new Insets(5, 5, 5, 5));
			layout.add(new JLabel(" "), 0, 3, 1, 3, new Insets(5, 5, 5, 5));
		}
	}
	
	static class ModelPanel extends JScrollPane implements ActionListener
	{
		JLabel lModel, lInitial, lTransition, lFreq, lDifficulty, lFreqEst;
		JLabel lTransRatio;
		JComboBox cModel, cInitial, cTransition;
		JTextField cFreq1, cFreq2, cFreq3, cDifficulty, cTransRatio;
		JTextField cFreqEst1, cFreqEst2, cFreqEst3, cFreqEst4;
	
	
		ModelPanel()
		{
			setPreferredSize(new Dimension(50, 50));
		
			// Model
			String[] v1 = { "F84+gaps", "JC+gaps", "K2P+gaps", "F81+gaps" };
			cModel = new JComboBox(v1);
			cModel.setSelectedItem(Prefs.hmm_model);
			cModel.addActionListener(this);
			lModel = new JLabel("Nucleotide substitution model:");

			// Initial
			String[] v2 = { "Yes", "No" };
			cInitial = new JComboBox(v2);
			cInitial.setSelectedItem(Prefs.hmm_initial);
			cInitial.addActionListener(this);
			lInitial = new JLabel("Estimate initial character frequencies from data:");
			
			cFreqEst1 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_est_1));
			cFreqEst2 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_est_2));
			cFreqEst3 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_est_3));
			cFreqEst4 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_est_4));
			lFreqEst = new JLabel("    Frequency estimates:");
			
			// Transition
			cTransition = new JComboBox(v2);
			cTransition.setSelectedItem(Prefs.hmm_transition);
			cTransition.addActionListener(this);
			lTransition = new JLabel("Estimate transition/transversion ratio from data:");
			
			cTransRatio = new JTextField(Prefs.d4.format(Prefs.hmm_transition_ratio));
			lTransRatio = new JLabel("    Initial transition-transversion ratio:");
			
			// Frequencies
			cFreq1 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_1));
			cFreq2 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_2));
			cFreq3 = new JTextField(Prefs.d4.format(Prefs.hmm_freq_3));
			lFreq = new JLabel("Frequencies of the three topologies:");
			
			// Difficulty
			cDifficulty = new JTextField(Prefs.d4.format(Prefs.hmm_difficulty));
			lDifficulty = new JLabel("Difficulty of changing trees:");
		
		
			DoeLayout layout = new DoeLayout();
			setViewportView(layout.getPanel());			
			setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
			getVerticalScrollBar().setUnitIncrement(10);
			
			layout.add(lModel, 0, 0, 0, 1, new Insets(10, 10, 5, 5));
			layout.add(cModel, 1, 0, 1, 4, new Insets(10, 5, 5, 10));
			
			layout.add(lInitial, 0, 1, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cInitial, 1, 1, 1, 4, new Insets(5, 5, 5, 10));
			
			layout.add(lFreqEst, 0, 2, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cFreqEst1, 1, 2, 1, 1, new Insets(5, 5, 5, 0));
			layout.add(cFreqEst2, 2, 2, 1, 1, new Insets(5, 5, 5, 0));
			layout.add(cFreqEst3, 3, 2, 1, 1, new Insets(5, 5, 5, 0));
			layout.add(cFreqEst4, 4, 2, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lTransition, 0, 3, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cTransition, 1, 3, 1, 4, new Insets(5, 5, 5, 10));
			
			layout.add(lTransRatio, 0, 4, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cTransRatio, 1, 4, 1, 4, new Insets(5, 5, 5, 10));
			
			JPanel p1 = new JPanel(new GridLayout(1, 3, 5, 0));
			p1.add(cFreq1); p1.add(cFreq2); p1.add(cFreq3);
			layout.add(lFreq, 0, 5, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(p1, 1, 5, 1, 4, new Insets(5, 5, 5, 10));
			
			layout.add(lDifficulty, 0, 6, 0, 4, new Insets(5, 10, 10, 5));
			layout.add(cDifficulty, 1, 6, 1, 4, new Insets(5, 5, 10, 10));
			
			setStates();
		}
		
		public void actionPerformed(ActionEvent e)
		{
			saveSettings();
			setStates();
		}
		
		private void setStates()
		{
			boolean state;
			
			// Frequency estimate text entries enabled/disabled
			if ((Prefs.hmm_model.equals("F84+gaps") ||
					Prefs.hmm_model.equals("F81+gaps")) &&
					Prefs.hmm_initial.equals("No"))
				state = true;
			else
				state = false;
			lFreqEst.setEnabled(state);
			cFreqEst1.setEnabled(state); cFreqEst2.setEnabled(state);
			cFreqEst3.setEnabled(state); cFreqEst4.setEnabled(state);
			
			// Estimate yes/no enabled/disabled
			if (Prefs.hmm_model.equals("F84+gaps") ||
					Prefs.hmm_model.equals("F81+gaps"))
			{
				cInitial.setEnabled(true);
				lInitial.setEnabled(true);
			}
			else
			{
				cInitial.setEnabled(false);
				lInitial.setEnabled(false);
			}
			
			
			// Initial transition-transversion enabled/disabled
			if ((Prefs.hmm_model.equals("F84+gaps") ||
					Prefs.hmm_model.equals("K2P+gaps")) &&
					Prefs.hmm_transition.equals("No"))
			{
				cTransRatio.setEnabled(true);
				lTransRatio.setEnabled(true);
			}
			else
			{
				cTransRatio.setEnabled(false);
				lTransRatio.setEnabled(false);
			}
			
			// Estimate transition enabled/disabled
			if (Prefs.hmm_model.equals("F84+gaps") ||
					Prefs.hmm_model.equals("K2P+gaps"))
			{
				cTransition.setEnabled(true);
				lTransition.setEnabled(true);
			}
			else
			{
				cTransition.setEnabled(false);
				lTransition.setEnabled(false);
			}
		}
	
		boolean saveSettings()
		{
			Prefs.hmm_model = (String) cModel.getSelectedItem();
			Prefs.hmm_initial = (String) cInitial.getSelectedItem();
			
			try
			{
				Prefs.hmm_freq_est_1 = Prefs.d4.parse(cFreqEst1.getText()).floatValue();
				Prefs.hmm_freq_est_2 = Prefs.d4.parse(cFreqEst2.getText()).floatValue();
				Prefs.hmm_freq_est_3 = Prefs.d4.parse(cFreqEst3.getText()).floatValue();
				Prefs.hmm_freq_est_4 = Prefs.d4.parse(cFreqEst4.getText()).floatValue();
				
				if (Prefs.hmm_freq_est_1 + Prefs.hmm_freq_est_2
					+ Prefs.hmm_freq_est_3 + Prefs.hmm_freq_est_4 != 1.0f)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Frequency estimates: values must sum to one.", cFreqEst1);
			}
			
			
			Prefs.hmm_transition = (String) cTransition.getSelectedItem();
			
			try
			{
				Prefs.hmm_transition_ratio = Prefs.d4.parse(cTransRatio.getText()).floatValue();
				if (Prefs.hmm_transition_ratio < 0)
					throw new Exception();
			}
			catch (Exception e) {
				return error("Initial transition-transversion ratio: positive number expected.", cTransRatio);
			}
			
			try
			{
				Prefs.hmm_freq_1 = Prefs.d4.parse(cFreq1.getText()).floatValue();
				Prefs.hmm_freq_2 = Prefs.d4.parse(cFreq2.getText()).floatValue();
				Prefs.hmm_freq_3 = Prefs.d4.parse(cFreq3.getText()).floatValue();
				
				float sum = Prefs.hmm_freq_1 + Prefs.hmm_freq_2 + Prefs.hmm_freq_3;
				
				// Barce needs this to sum to one (but need some margin for error)
				if (sum < 0.98f || sum > 1.02f)
					throw new Exception();
				
				// Even so, scale the values so that barce is happy
				Prefs.hmm_freq_1 = Prefs.hmm_freq_1 / (float) sum;
				Prefs.hmm_freq_2 = Prefs.hmm_freq_2 / (float) sum;
				Prefs.hmm_freq_3 = Prefs.hmm_freq_3 / (float) sum;
			}
			catch (Exception e)	{
				return error("Topology frequencies: values must sum to one.", cFreq1);
			}
			
			try
			{
				Prefs.hmm_difficulty = Prefs.d4.parse(cDifficulty.getText()).floatValue();
				if (Prefs.hmm_difficulty < 0 || Prefs.hmm_difficulty > 1)
					throw new Exception();
			}
			catch (Exception e) {
				return error("Difficulty of changing trees: number must be between zero and one.", cDifficulty);
			}
			
			return true;
		}
		
		private boolean error(String msg, Component control)
		{
			String str = "An incorrect value has been entered for the "
				+ "following model setting:\n" + msg;
		
			MsgBox.msg(str, MsgBox.ERR);
			control.requestFocus();
			return false;
		}
	}
	
	static class RunPanel extends JScrollPane implements ActionListener
	{
		JLabel lBurn, lSample, lPoints, lThinning, lTune, lLambda, lAnneal;
		JLabel lStation, lUpdate, lBranch;
		JTextField cBurn, cSample, cPoints, cThinning, cTune, cBranch;
		JComboBox cLambda, cAnneal, cStation, cUpdate;
	
	
		RunPanel()
		{
			setPreferredSize(new Dimension(50, 50));
		
			// Burn
			cBurn = new JTextField("" + Prefs.hmm_burn);
			lBurn = new JLabel("Length of the burn-in period:");
			
			// Sample
			cSample = new JTextField("" + Prefs.hmm_points
				* Prefs.hmm_thinning);
			cSample.setEnabled(false);
			lSample = new JLabel("Length of the sampling period (determined by):");
			lSample.setEnabled(false);
			
			// Points
			cPoints = new JTextField("" + Prefs.hmm_points);
			lPoints = new JLabel("    Number of points to return:");
			
			// Thinning
			cThinning = new JTextField("" + Prefs.hmm_thinning);
			lThinning = new JLabel("    Thinning interval:");
			
			// Tuning intervale
			cTune = new JTextField("" + Prefs.hmm_tuning);
			lTune = new JLabel("Tuning interval:");
			
			// Lambda
			String[] v1 = { "Yes", "No" };
			cLambda = new JComboBox(v1);
			cLambda.setSelectedItem(Prefs.hmm_lambda);
			cLambda.addActionListener(this);
			lLambda = new JLabel("Update lambda with Gibbs sampling:");
			
			// Annealing
			String[] v2 = { "PAR", "PROB", "NONE" };
			cAnneal = new JComboBox(v2);
			cAnneal.setSelectedItem(Prefs.hmm_annealing);
			lAnneal = new JLabel("    Annealing scheme for lambda:");
			
			// Stationary frequencies
			cStation = new JComboBox(v1);
			cStation.setSelectedItem(Prefs.hmm_station);
			lStation = new JLabel("Update stationary frequencies in MCMC algorithm:");
			
			// Transition-transversion ratio
			cUpdate = new JComboBox(v1);
			cUpdate.setSelectedItem(Prefs.hmm_update);
			lUpdate = new JLabel("Update transition-transversion ratio in MCMC algorithm:");
			
			// Branch length
			cBranch = new JTextField(Prefs.d4.format(Prefs.hmm_branch));
			lBranch = new JLabel("Branch length in initial trees:");
		
		
			DoeLayout layout = new DoeLayout();
			setViewportView(layout.getPanel());
			setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
			getVerticalScrollBar().setUnitIncrement(10);
			
			layout.add(lBurn, 0, 0, 0, 1, new Insets(10, 10, 5, 5));
			layout.add(cBurn, 1, 0, 1, 3, new Insets(10, 5, 5, 10));
			
			layout.add(lSample, 0, 1, 0, 1, new Insets(5, 10, 5, 10));
			layout.add(cSample, 1, 1, 1, 2, new Insets(5, 5, 5, 10));
			
			layout.add(lPoints, 0, 2, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cPoints, 1, 2, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lThinning, 0, 3, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cThinning, 1, 3, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lTune, 0, 4, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cTune, 1, 4, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lLambda, 0, 5, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cLambda, 1, 5, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lAnneal, 0, 6, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cAnneal, 1, 6, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lStation, 0, 7, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cStation, 1, 7, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lUpdate, 0, 8, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cUpdate, 1, 8, 1, 1, new Insets(5, 5, 5, 10));
					
			layout.add(lBranch, 0, 9, 0, 1, new Insets(5, 10, 10, 5));
			layout.add(cBranch, 1, 9, 1, 1, new Insets(5, 5, 10, 10));

			addListeners(cPoints);
			addListeners(cThinning);
			
			setStates();
		}
		
		private void addListeners(JComponent comp)
		{
			AbstractAction action = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					computeSample();
				}
			};
			
			// Key listener for ENTER presses
			KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
			comp.getInputMap(JComponent.WHEN_FOCUSED).put(ks, "enter");
			comp.getActionMap().put("enter", action);
		
			comp.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					computeSample();
				}
			});
		}
		
		public void actionPerformed(ActionEvent e)
		{
			saveSettings();
			setStates();
		}
		
		private void setStates()
		{
			if (Prefs.hmm_lambda.equals("Yes"))
			{
				cAnneal.setEnabled(true);
				lAnneal.setEnabled(true);
			}
			else
			{
				cAnneal.setEnabled(false);
				lAnneal.setEnabled(false);
			}
		}
		
		private void computeSample()
		{
			try
			{
				int p = Integer.parseInt(cPoints.getText());
				int t = Integer.parseInt(cThinning.getText());
				
				if (p < 1 || t < 1) throw new Exception();
			
				Prefs.hmm_points = p;
				Prefs.hmm_thinning = t;				
				cSample.setText("" + (p * t));
			}
			catch (Exception e)
			{
				cPoints.setText("" + Prefs.hmm_points);
				cThinning.setText("" + Prefs.hmm_thinning);
			}
		}
	
		boolean saveSettings()
		{
			try
			{
				Prefs.hmm_burn = Integer.parseInt(cBurn.getText());
				if (Prefs.hmm_burn < 1)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Burn-in period: positive integer expexted.", cBurn);
			}
			
			try
			{
				Prefs.hmm_points = Integer.parseInt(cPoints.getText());
				if (Prefs.hmm_points < 1)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Number of points to return: positive integer expexted.", cPoints);
			}
			
			try
			{
				Prefs.hmm_thinning = Integer.parseInt(cThinning.getText());
				if (Prefs.hmm_points < 1)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Thinning interval: positive integer expexted.", cThinning);
			}
			
			try
			{
				Prefs.hmm_tuning = Integer.parseInt(cTune.getText());
				if (Prefs.hmm_tuning < 1)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Tuning interval: positive integer expexted.", cTune);
			}
			
			Prefs.hmm_lambda = (String) cLambda.getSelectedItem();
			Prefs.hmm_annealing = (String) cAnneal.getSelectedItem();
			Prefs.hmm_station = (String) cStation.getSelectedItem();
			Prefs.hmm_update = (String) cUpdate.getSelectedItem();
			
			try
			{
				Prefs.hmm_branch = Prefs.d4.parse(cBranch.getText()).floatValue();
				if (Prefs.hmm_branch < 0)
					throw new Exception();
			}
			catch (Exception e)	{
				return error("Branch length: positive number expexted.", cTune);
			}
			
			return true;
		}
		
		private boolean error(String msg, Component control)
		{
			String str = "An incorrect value has been entered for the "
				+ "following run setting:\n" + msg;
		
			MsgBox.msg(str, MsgBox.ERR);
			control.requestFocus();
			return false;
		}
	}
}
