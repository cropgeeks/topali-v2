// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.analyses.SequenceSetUtils;
import topali.data.*;
import topali.gui.*;
import topali.var.Utils;
import doe.*;

public class PDMSettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;

	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	private BasicPanel basicPanel;

	// private ExtraPanel extraPanel;
	private BambePanel bambePanel;

	private AlignmentData data;

	private PDMResult result = null;

	public PDMSettingsDialog(WinMain winMain, AlignmentData data,
			PDMResult iResult)
	{
		super(winMain, "Probabilistic Divergence Measures - Confirm Settings",
				true);
		this.data = data;

		if (iResult != null)
			setInitialSettings(iResult);

		tabs = new JTabbedPane();
		basicPanel = new BasicPanel();
		// extraPanel = new ExtraPanel();
		bambePanel = new BambePanel();
		tabs.addTab("Basic", basicPanel);
		// tabs.addTab("Advanced", extraPanel);
		tabs.addTab("Advanced", bambePanel);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				// basicPanel.stepSpin.requestFocus();
			}
		});

		add(tabs, BorderLayout.CENTER);

		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "pdm_settings");
		add(bp, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);

		pack();
		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	public PDMResult getPDMResult()
	{
		return result;
	}

	private void createPDMObject(boolean makeRemote)
	{
		SequenceSet ss = data.getSequenceSet();

		result = new PDMResult();

		if (Prefs.isWindows)
		{
			result.bambePath = Utils.getLocalPath() + "bambe.exe";
			result.treeDistPath = Utils.getLocalPath() + "treedist.exe";
		} else
		{
			result.bambePath = Utils.getLocalPath() + "bambe/bambe";
			result.treeDistPath = Utils.getLocalPath() + "treedist/treedist";
		}

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = makeRemote;

		// Do we need to estimate parameters?
		if (ss.hasParametersEstimated() == false)
			SequenceSetUtils.estimateParameters(ss);

		result.pdm_window = Prefs.pdm_window;
		result.treeToolTipWindow = Prefs.pdm_window;
		result.pdm_step = Prefs.pdm_step;
		result.pdm_runs = Prefs.pdm_runs + 1;
		result.pdm_prune = Prefs.pdm_prune;
		result.pdm_cutoff = Prefs.pdm_cutoff;
		result.pdm_seed = Prefs.pdm_seed;
		result.pdm_burn = Prefs.pdm_burn;
		result.pdm_cycles = Prefs.pdm_cycles;
		result.pdm_burn_algorithm = Prefs.pdm_burn_algorithm;
		result.pdm_main_algorithm = Prefs.pdm_main_algorithm;
		result.pdm_use_beta = Prefs.pdm_use_beta;
		result.pdm_parameter_update_interval = Prefs.pdm_parameter_update_interval;
		result.pdm_update_theta = Prefs.pdm_update_theta;
		result.pdm_tune_interval = Prefs.pdm_tune_interval;
		result.pdm_molecular_clock = Prefs.pdm_molecular_clock;
		result.pdm_category_list = Prefs.pdm_category_list;
		result.pdm_initial_theta = Prefs.pdm_initial_theta;
		result.pdm_outgroup = Prefs.pdm_outgroup;
		result.pdm_global_tune = Prefs.pdm_global_tune;
		result.pdm_local_tune = Prefs.pdm_local_tune;
		result.pdm_theta_tune = Prefs.pdm_theta_tune;
		result.pdm_beta_tune = Prefs.pdm_beta_tune;

		result.frequencies = ss.getParams().getFreqs();
		result.kappa = ss.getParams().getKappa();

		result.tRatio = ss.getParams().getTRatio();
		result.alpha = ss.getParams().getAlpha();
		
		int runNum = data.getTracker().getPdmRunCount() + 1;
		data.getTracker().setPdmRunCount(runNum);
		result.guiName = "PDM " + runNum;
		result.jobName = "PDM Analysis " + runNum + " on " + data.name + " ("
				+ ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";
	}

	private void setInitialSettings(PDMResult iResult)
	{
		Prefs.pdm_window = iResult.pdm_window;
		Prefs.pdm_step = iResult.pdm_step;
		Prefs.pdm_prune = iResult.pdm_prune;
		Prefs.pdm_cutoff = iResult.pdm_cutoff;
		Prefs.pdm_seed = iResult.pdm_seed;
		Prefs.pdm_burn = iResult.pdm_burn;
		Prefs.pdm_cycles = iResult.pdm_cycles;
		Prefs.pdm_burn_algorithm = iResult.pdm_burn_algorithm;
		Prefs.pdm_main_algorithm = iResult.pdm_main_algorithm;
		Prefs.pdm_use_beta = iResult.pdm_use_beta;
		Prefs.pdm_parameter_update_interval = iResult.pdm_parameter_update_interval;
		Prefs.pdm_update_theta = iResult.pdm_update_theta;
		Prefs.pdm_tune_interval = iResult.pdm_tune_interval;
		Prefs.pdm_molecular_clock = iResult.pdm_molecular_clock;
		Prefs.pdm_category_list = iResult.pdm_category_list;
		Prefs.pdm_initial_theta = iResult.pdm_initial_theta;
		Prefs.pdm_outgroup = iResult.pdm_outgroup;
		Prefs.pdm_global_tune = iResult.pdm_global_tune;
		Prefs.pdm_local_tune = iResult.pdm_local_tune;
		Prefs.pdm_theta_tune = iResult.pdm_theta_tune;
		Prefs.pdm_beta_tune = iResult.pdm_beta_tune;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bRun)
		{
			basicPanel.saveSettings();
			// if (extraPanel.saveSettings() == false)
			// return;
			if (bambePanel.saveSettings() == false)
				return;

			createPDMObject((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
			setVisible(false);
		}

		else if (e.getSource() == bDefault)
			defaultClicked();
	}

	private void defaultClicked()
	{
		int res = MsgBox.yesno(
				"This will return all settings (on all tabs) to their default "
						+ "values. Continue?", 1);
		if (res != JOptionPane.YES_OPTION)
			return;

		// Tell the preferences object to reset them
		Prefs.setPDMDefaults();

		// Now recreate the panels with these new values
		basicPanel = new BasicPanel();
		// extraPanel = new ExtraPanel();
		bambePanel = new BambePanel();

		int index = tabs.getSelectedIndex();
		tabs.removeAll();
		tabs.addTab("Basic", basicPanel);
		// tabs.addTab("Advanced", extraPanel);
		tabs.addTab("Advanced", bambePanel);
		tabs.setSelectedIndex(index);
	}

	class BasicPanel extends JPanel
	{
		SlidePanel slidePanel;

		BasicPanel()
		{
			slidePanel = new SlidePanel(data, Prefs.pdm_window, Prefs.pdm_step);

			DoeLayout layout = new DoeLayout();
			add(layout.getPanel(), BorderLayout.NORTH);

			JLabel info1 = new JLabel(
					"Please confirm the current settings for "
							+ "running PDM. Additional configuration is also");
			JLabel info2 = new JLabel("<html>available by selecting the <b>"
					+ "Advanced</b> tab and modifying the options found there."
					+ "</html>");

			layout.add(info1, 0, 0, 1, 1, new Insets(5, 5, 2, 5));
			layout.add(info2, 0, 1, 1, 1, new Insets(0, 5, 10, 5));
			layout.add(slidePanel, 0, 2, 1, 1, new Insets(5, 5, 0, 5));
		}

		void saveSettings()
		{
			Prefs.pdm_window = slidePanel.getWindowSize();
			Prefs.pdm_step = slidePanel.getStepSize();
		}
	}

	static class BambePanel extends JScrollPane
	{
		JLabel lSeed, lBurn, lCycles, lBurnAlg, lMainAlg, lUseBeta, lSample;

		JLabel lNewick, lMole, lModel, lCat, lSingleKap, lInitKap, lInitThet;

		JLabel lEstimate, lPIA, lPIG, lPIC, lPIT, lTTP, lGAM, lParam,
				lTreeFile;

		JLabel luKap, luThet, luPi, luTTP, luGAM, ltInt, ltGlo, ltLoc, ltThe;

		JLabel ltPi, ltKap, ltTTP, ltGam, ltBet, lOutgroup, lTree, lHeight;

		JLabel lPrune, lRuns;

		JTextField cSeed, cBurn, cCycles, cSample, cCat, cInitKap, cInitThet;

		JTextField cTTP, cGAM, cParam, cOutgroup;

		JTextField ctInt, ctGlo, ctLoc, ctThe, ctPi, ctKap, ctTTP, ctGam,
				ctBet;

		JTextField cHeight, cTreeFile;

		JComboBox cBurnAlg, cMainAlg, cUseBeta, cNewick, cMole, cModel, cTree;

		JComboBox cSingleKap, cEstimate, cuKap, cuThet, cuPi, cuTTP, cuGAM;

		JComboBox cPrune;

		JSpinner cRuns;

		SpinnerNumberModel runsModel;

		BambePanel()
		{
			setPreferredSize(new Dimension(50, 50));

			// Threshold runs
			runsModel = new SpinnerNumberModel(Prefs.pdm_runs, 10, 1000, 1);
			cRuns = new JSpinner(runsModel);
			((JSpinner.NumberEditor) cRuns.getEditor())
					.getTextField()
					.setToolTipText(
							"Number of times to re-run the analysis to calculate the threshold values");
			lRuns = new JLabel("Number of bootstrapping threshold runs:");

			// Pruning
			String[] v1 =
			{ "Yes", "No" };
			cPrune = new JComboBox(v1);
			lPrune = new JLabel("Prune histogram results:");
			cPrune.setToolTipText("Toggle whether or not to use Pruned PDM "
					+ "(recommended for large numbers of sequences)");
			if (Prefs.pdm_prune == false)
				cPrune.setSelectedIndex(1);

			// Seed
			cSeed = new JTextField("" + Prefs.pdm_seed);
			cSeed.setToolTipText("1 < odd integer < 2147483647 (2^31 - 1)");
			lSeed = new JLabel("Seed:");

			// Burn
			cBurn = new JTextField("" + Prefs.pdm_burn);
			cBurn.setToolTipText("Number of cycles to run burn algorithm");
			lBurn = new JLabel("Burn-in time:");

			// Cycles
			cCycles = new JTextField("" + Prefs.pdm_cycles);
			cCycles.setToolTipText("Number of cycles to run main algorithm");
			lCycles = new JLabel("Cycles:");

			// Burn-algorithm
			String[] v2 =
			{ "global", "local" };
			cBurnAlg = new JComboBox(v2);
			lBurnAlg = new JLabel("Burn-algorithm:");
			if (Prefs.pdm_burn_algorithm.equals("local"))
				cBurnAlg.setSelectedIndex(1);

			// Main-algorithm
			String[] v3 =
			{ "local", "global" };
			cMainAlg = new JComboBox(v3);
			cMainAlg.setSelectedItem(Prefs.pdm_main_algorithm);
			lMainAlg = new JLabel("Main-algorithm:");

			// Use-beta
			String[] v4 =
			{ "false", "true" };
			cUseBeta = new JComboBox(v4);
			cUseBeta.setSelectedItem(Prefs.pdm_use_beta);
			cUseBeta
					.setToolTipText("Use scaled beta distribution modification of local");
			lUseBeta = new JLabel("Use-beta:");

			// Sample-interval
			// cSample = new JTextField("" + Prefs.pdm_sample_interval);
			// cSample.setToolTipText("Interval to save information to file");
			// lSample = new JLabel("Sample-interval:");

			// Newick format
			String[] v5 =
			{ "true", "false" };
			// cNewick = new JComboBox(v4);
			// cNewick.setSelectedItem(Prefs.pdm_newick_format);
			// cNewick.setToolTipText("Format of output tree, false implies
			// bambe format");
			// lNewick = new JLabel("Newick-format:");

			// Molecular clock
			cMole = new JComboBox(v4);
			cMole.setSelectedItem(Prefs.pdm_molecular_clock);
			cMole.setToolTipText("If true, assume constant rate of evolution "
					+ "for all branches");
			lMole = new JLabel("Molecular-clock:");

			// Lilelihood-model
			// String[] v5 = { "HKY85", "F84", "TN93" };
			// cModel = new JComboBox(v5);
			// cModel.setSelectedItem(Prefs.pdm_likelihood_model);
			// lModel = new JLabel("Likelihood-model:");

			// Category-list
			cCat = new JTextField("" + Prefs.pdm_category_list);
			cCat.setToolTipText("One site category");
			lCat = new JLabel("Category-list:");

			// Single-kappa
			// cSingleKap = new JComboBox(v4);
			// cSingleKap.setSelectedItem(Prefs.pdm_single_kappa);
			// cSingleKap.setToolTipText("Use separate kappa values for all site
			// categories");
			// lSingleKap = new JLabel("Single-kappa:");

			// Initial-theta
			cInitThet = new JTextField("" + Prefs.pdm_initial_theta);
			lInitThet = new JLabel("Initial-theta:");

			// Estimate-pi
			// cEstimate = new JComboBox(v4);
			// cEstimate.setSelectedItem(Prefs.pdm_estimate_pi);
			// cEstimate.setToolTipText("Use emperical relative frequencies");
			// lEstimate = new JLabel("Estimate-pi:");

			// Initial-ttp
			// cTTP = new JTextField("" + Prefs.pdm_initial_ttp);
			// lTTP = new JLabel("Initial-ttp:");

			// Initial-gamma
			// cGAM = new JTextField("" + Prefs.pdm_initial_gamma);
			// lGAM = new JLabel("Initial-gamma:");

			// Parameter-update-interval
			cParam = new JTextField("" + Prefs.pdm_parameter_update_interval);
			cParam
					.setToolTipText("Number of tree updates after each parameter update");
			lParam = new JLabel("Parameter-update-interval:");

			// Update-kappa
			// cuKap = new JComboBox(v4);
			// cuKap.setSelectedItem(Prefs.pdm_update_kappa);
			// cuKap.setToolTipText("Transition/tranversion bias parameters, not
			// used");
			// luKap = new JLabel("Update-kappa:");

			// Update-theta
			cuThet = new JComboBox(v5);
			cuThet.setSelectedItem(Prefs.pdm_update_theta);
			cuThet.setToolTipText("Substitution rate parameters");
			luThet = new JLabel("Update-theta:");

			// Update-pi
			// cuPi = new JComboBox(v4);
			// cuPi.setSelectedItem(Prefs.pdm_update_pi);
			// cuPi.setToolTipText("Base relative frequency parameters");
			// luPi = new JLabel("Update-pi:");

			// Update-ttp
			// cuTTP = new JComboBox(v4);
			// cuTTP.setSelectedItem(Prefs.pdm_update_ttp);
			// cuTTP.setToolTipText("Transition/tranversion bias parameters for
			// TN93");
			// luTTP = new JLabel("Update-ttp:");

			// Update-kappa
			// cuGAM = new JComboBox(v4);
			// cuGAM.setSelectedItem(Prefs.pdm_update_gamma);
			// cuGAM.setToolTipText("Transition/tranversion bias parameters for
			// TN93");
			// luGAM = new JLabel("Update-gamma:");

			// Tune-interval
			ctInt = new JTextField("" + Prefs.pdm_tune_interval);
			ctInt
					.setToolTipText("Interval to adapt to global-tune during burn-in");
			ltInt = new JLabel("Tune-interval:");

			// Global-tune
			ctGlo = new JTextField("" + Prefs.d5.format(Prefs.pdm_global_tune));
			ctGlo.setToolTipText("Half window width for global");
			ltGlo = new JLabel("Global-tune:");

			// Local-tune
			ctLoc = new JTextField("" + Prefs.d5.format(Prefs.pdm_local_tune));
			ctLoc.setToolTipText("Stretch parameter for local");
			ltLoc = new JLabel("Local-tune:");

			// Theta-tune
			ctThe = new JTextField("" + Prefs.d5.format(Prefs.pdm_theta_tune));
			ctThe.setToolTipText("Dirichlet parameter for theta update");
			ltThe = new JLabel("Theta-tune:");

			// Pi-tune
			// ctPi = new JTextField("" + Prefs.d5.format(Prefs.pdm_pi_tune));
			// ctPi.setToolTipText("Dirichlet parameter for pi update");
			// ltPi = new JLabel("Pi-tune:");

			// Kappa-tune
			// ctKap = new JTextField("" +
			// Prefs.d5.format(Prefs.pdm_kappa_tune));
			// ctKap.setToolTipText("Half window width for kappa update");
			// ltKap = new JLabel("Kappa-tune:");

			// Ttp-tune
			// ctTTP = new JTextField("" + Prefs.d5.format(Prefs.pdm_ttp_tune));
			// ctTTP.setToolTipText("Half window width for ttp update");
			// ltTTP = new JLabel("Ttp-tune:");

			// Gamma-tune
			// ctGam = new JTextField("" +
			// Prefs.d5.format(Prefs.pdm_gamma_tune));
			// ctGam.setToolTipText("Half window width for gamma update");
			// ltGam = new JLabel("Gamma-tune:");

			// Beta-tune
			ctBet = new JTextField("" + Prefs.d5.format(Prefs.pdm_beta_tune));
			ctBet.setToolTipText("Half window width for beta update");
			ltBet = new JLabel("Beta-tune:");

			// Outgroup
			cOutgroup = new JTextField("" + Prefs.pdm_outgroup);
			lOutgroup = new JLabel("Outgroup:");

			// Intitial-tree-type
			// String[] v6 = { "random", "upgma", "neighbor-joining", "newick",
			// "bambe" };
			// cTree = new JComboBox(v6);
			// cTree.setSelectedItem(Prefs.pdm_initial_tree_type);
			// lTree = new JLabel("Initial-tree-type:");

			// Max-initial-tree-height
			// cHeight = new JTextField("" +
			// Prefs.d5.format(Prefs.pdm_max_initial_tree_height));
			// cHeight.setToolTipText("Height of initial random tree");
			// lHeight = new JLabel("Max-initial-tree-height:");

			// Tree-file
			// cTreeFile = new JTextField("" + Prefs.pdm_tree_file);
			// cTreeFile.setToolTipText("Name of file with initial tree (newick
			// or bambe)");
			// lTreeFile = new JLabel("Tree-file:");

			DoeLayout layout = new DoeLayout();
			setViewportView(layout.getPanel());
			setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
			getVerticalScrollBar().setUnitIncrement(10);

			layout.add(lRuns, 0, 0, 0, 1, new Insets(10, 10, 5, 5));
			layout.add(cRuns, 1, 0, 1, 1, new Insets(10, 5, 5, 10));

			layout.add(lPrune, 0, 1, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cPrune, 1, 1, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lSeed, 0, 2, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cSeed, 1, 2, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lBurn, 0, 3, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cBurn, 1, 3, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lCycles, 0, 4, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cCycles, 1, 4, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lBurnAlg, 0, 5, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cBurnAlg, 1, 5, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lMainAlg, 0, 6, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cMainAlg, 1, 6, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lUseBeta, 0, 7, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cUseBeta, 1, 7, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lSample, 0, 8, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cSample, 1, 8, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lNewick, 0, 9, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cNewick, 1, 9, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lMole, 0, 8, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cMole, 1, 8, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lModel, 0, 9, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cModel, 1, 9, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lCat, 0, 10, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cCat, 1, 10, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lSingleKap, 0, 11, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cSingleKap, 1, 11, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lInitKap, 0, 12, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cInitKap, 1, 12, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lInitThet, 0, 13, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cInitThet, 1, 13, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lEstimate, 0, 14, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cEstimate, 1, 14, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lPIA, 0, 15, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cPIA, 1, 15, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lPIG, 0, 16, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cPIG, 1, 16, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lPIC, 0, 17, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cPIC, 1, 17, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lPIT, 0, 18, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cPIT, 1, 18, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lTTP, 0, 19, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cTTP, 1, 19, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lGAM, 0, 20, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cGAM, 1, 20, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lParam, 0, 21, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cParam, 1, 21, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(luKap, 0, 22, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cuKap, 1, 22, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(luThet, 0, 23, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cuThet, 1, 23, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(luPi, 0, 24, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cuPi, 1, 24, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(luTTP, 0, 25, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cuTTP, 1, 25, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(luGAM, 0, 26, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cuGAM, 1, 26, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(ltInt, 0, 27, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(ctInt, 1, 27, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(ltGlo, 0, 28, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(ctGlo, 1, 28, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(ltLoc, 0, 29, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(ctLoc, 1, 29, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(ltThe, 0, 30, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(ctThe, 1, 30, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(ltPi, 0, 31, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(ctPi, 1, 31, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(ltKap, 0, 32, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(ctKap, 1, 32, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(ltTTP, 0, 33, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(ctTTP, 1, 33, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(ltGam, 0, 34, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(ctGam, 1, 34, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(ltBet, 0, 35, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(ctBet, 1, 35, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lOutgroup, 0, 36, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cOutgroup, 1, 36, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lTree, 0, 37, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cTree, 1, 37, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lHeight, 0, 38, 0, 1, new Insets(5, 10, 5, 5));
			// layout.add(cHeight, 1, 38, 1, 1, new Insets(5, 5, 5, 10));

			// layout.add(lTreeFile, 0, 39, 0, 1, new Insets(5, 10, 10, 5));
			// layout.add(cTreeFile, 1, 39, 1, 1, new Insets(5, 5, 10, 10));
		}

		boolean saveSettings()
		{
			Prefs.pdm_runs = runsModel.getNumber().intValue();

			try
			{
				Prefs.pdm_seed = Integer.parseInt(cSeed.getText());
			} catch (Exception e)
			{
				return error("Seed: positive (odd) integer expected.", cSeed);
			}

			try
			{
				Prefs.pdm_burn = Integer.parseInt(cBurn.getText());
			} catch (Exception e)
			{
				return error("Burn-in time: positive integer expected.", cBurn);
			}

			try
			{
				Prefs.pdm_cycles = Integer.parseInt(cCycles.getText());
			} catch (Exception e)
			{
				return error("Cycles: positive integer expected.", cCycles);
			}

			Prefs.pdm_burn_algorithm = (String) cBurnAlg.getSelectedItem();
			Prefs.pdm_main_algorithm = (String) cMainAlg.getSelectedItem();
			Prefs.pdm_use_beta = (String) cUseBeta.getSelectedItem();

			// try { Prefs.pdm_sample_interval =
			// Integer.parseInt(cSample.getText()); }
			// catch (Exception e) {
			// return error("Sample-interval: positive integer expected.",
			// cBurn);
			// }

			// Prefs.pdm_newick_format = (String) cNewick.getSelectedItem();
			Prefs.pdm_molecular_clock = (String) cMole.getSelectedItem();
			// Prefs.pdm_likelihood_model = (String) cModel.getSelectedItem();
			Prefs.pdm_category_list = cCat.getText();
			// Prefs.pdm_single_kappa = (String) cSingleKap.getSelectedItem();
			Prefs.pdm_initial_theta = cInitThet.getText();
			// Prefs.pdm_estimate_pi = (String) cEstimate.getSelectedItem();
			// Prefs.pdm_initial_ttp = cTTP.getText();
			// Prefs.pdm_initial_gamma = cGAM.getText();

			try
			{
				Prefs.pdm_parameter_update_interval = Integer.parseInt(cParam
						.getText());
			} catch (Exception e)
			{
				return error(
						"Parameter-update-interval: positive integer expected.",
						cParam);
			}

			// Prefs.pdm_update_kappa = (String) cuKap.getSelectedItem();
			Prefs.pdm_update_theta = (String) cuThet.getSelectedItem();
			// Prefs.pdm_update_pi = (String) cuPi.getSelectedItem();
			// Prefs.pdm_update_ttp = (String) cuTTP.getSelectedItem();
			// Prefs.pdm_update_gamma = (String) cuGAM.getSelectedItem();

			try
			{
				Prefs.pdm_tune_interval = Integer.parseInt(ctInt.getText());
			} catch (Exception e)
			{
				return error("Tune-interval: positive integer expected.", ctInt);
			}

			try
			{
				Prefs.pdm_global_tune = Prefs.d5.parse(ctGlo.getText())
						.floatValue();
			} catch (Exception e)
			{
				return error("Global-tune: positive number expected.", ctInt);
			}

			try
			{
				Prefs.pdm_local_tune = Prefs.d5.parse(ctLoc.getText())
						.floatValue();
			} catch (Exception e)
			{
				return error("Local-tune: positive number expected.", ctLoc);
			}

			try
			{
				Prefs.pdm_theta_tune = Prefs.d5.parse(ctThe.getText())
						.floatValue();
			} catch (Exception e)
			{
				return error("Theta-tune: positive number expected.", ctThe);
			}

			// try { Prefs.pdm_pi_tune =
			// Prefs.d.parse(ctPi.getText()).floatValue(); }
			// catch (Exception e) {
			// return error("Pi-tune: positive number expected.", ctPi);
			// }

			// try { Prefs.pdm_kappa_tune =
			// Prefs.d5.parse(ctKap.getText()).floatValue(); }
			// catch (Exception e) {
			// return error("Kappa-tune: positive number expected.", ctKap);
			// }

			// try { Prefs.pdm_ttp_tune =
			// Prefs.d.parse(ctTTP.getText()).floatValue(); }
			// catch (Exception e) {
			// return error("Ttp-tune: positive number expected.", ctTTP);
			// }

			// try { Prefs.pdm_gamma_tune =
			// Prefs.d.parse(ctGam.getText()).floatValue(); }
			// catch (Exception e) {
			// return error("Gamma-tune: positive number expected.", ctGam);
			// }

			try
			{
				Prefs.pdm_beta_tune = Prefs.d5.parse(ctBet.getText())
						.floatValue();
			} catch (Exception e)
			{
				return error("Beta-tune: positive number expected.", ctBet);
			}

			try
			{
				Prefs.pdm_outgroup = Integer.parseInt(cOutgroup.getText());
			} catch (Exception e)
			{
				return error("Outgroup: positive integer expected.", ctBet);
			}

			Prefs.pdm_prune = cPrune.getSelectedIndex() == 0;

			// Prefs.pdm_initial_tree_type = (String) cTree.getSelectedItem();

			// try { Prefs.pdm_max_initial_tree_height =
			// Prefs.d.parse(cHeight.getText()).floatValue; }
			// catch (Exception e) {
			// return error("Max-initial-tree-height: positive number
			// expected.", cHeight);
			// }

			// Prefs.pdm_tree_file = cTreeFile.getText();

			return true;
		}

		private boolean error(String msg, Component control)
		{
			String str = "An incorrect value has been entered for the "
					+ "following advanced setting:\n" + msg;

			MsgBox.msg(str, MsgBox.ERR);
			control.requestFocus();
			return false;
		}
	}
}
