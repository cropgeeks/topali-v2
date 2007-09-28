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

public class LRTSettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;

	private JButton bRun = new JButton(), bCancel = new JButton(), bDefault = new JButton(), bHelp = new JButton();

	private BasicPanel basicPanel;

	private AdvancedPanel advancedPanel;

	private AlignmentData data;

	private LRTResult result = null;

	public LRTSettingsDialog(WinMain winMain, AlignmentData data,
			LRTResult iResult)
	{
		super(winMain, Text.GuiDiag.getString("LRTSettingsDialog.gui01"), true);
		this.data = data;

		if (iResult != null)
			setInitialSettings(iResult);

		tabs = new JTabbedPane();
		addTabs();

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent e)
			{
				// basicPanel.stepSpin.requestFocus();
			}
		});

		add(tabs, BorderLayout.CENTER);
		JPanel bp = Utils.getButtonPanel(bRun, bCancel, bDefault, bHelp, this, "lrt_settings");
		add(bp, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(bRun);
		Utils.addCloseHandler(this, bCancel);

		pack();

		setLocationRelativeTo(winMain);
		setResizable(false);
		setVisible(true);
	}

	private void addTabs()
	{
		advancedPanel = new AdvancedPanel();
		basicPanel = new BasicPanel();

		tabs.removeAll();
		tabs.addTab(Text.GuiDiag.getString("LRTSettingsDialog.gui04"),
				basicPanel);
		tabs.addTab(Text.GuiDiag.getString("LRTSettingsDialog.gui05"),
				advancedPanel);
	}

	public LRTResult getLRTResult()
	{
		return result;
	}

	private void createLRTObject(boolean makeRemote)
	{
		SequenceSet ss = data.getSequenceSet();

		result = new LRTResult();

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = makeRemote;

		// Do we need to estimate parameters?
		if (ss.hasParametersEstimated() == false)
			SequenceSetUtils.estimateParameters(ss);

		result.window = Prefs.lrt_window;
		result.treeToolTipWindow = Prefs.lrt_window;
		result.step = Prefs.lrt_step;
		result.runs = Prefs.lrt_runs + 1;

		result.method = Prefs.lrt_method;
		result.tRatio = ss.getParams().getTRatio();
		result.alpha = ss.getParams().getAlpha();
		
		int runNum = data.getTracker().getLrtRunCount() + 1;
		data.getTracker().setLrtRunCount(runNum);
		result.guiName = "LRT " + runNum;
		result.jobName = "LRT Analysis " + runNum + " on " + data.name + " ("
				+ ss.getSelectedSequences().length + "/" + ss.getSize()
				+ " sequences)";
	}

	private void setInitialSettings(LRTResult iResult)
	{
		Prefs.lrt_window = iResult.window;
		Prefs.lrt_step = iResult.step;
		Prefs.lrt_runs = iResult.runs - 1;

		Prefs.lrt_method = iResult.method;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bRun)
		{
			basicPanel.saveSettings();
			advancedPanel.saveSettings();

			createLRTObject((e.getModifiers() & ActionEvent.CTRL_MASK) == 0);
			setVisible(false);
		}

		else if (e.getSource() == bDefault)
			defaultClicked();
	}

	private void defaultClicked()
	{
		int res = MsgBox.yesno(Text.GuiDiag
				.getString("LRTSettingsDialog.gui06"), 1);
		if (res != JOptionPane.YES_OPTION)
			return;

		// Tell the preferences object to reset them
		Prefs.setLRTDefaults();

		// Now recreate the panels with these new values
		int index = tabs.getSelectedIndex();
		addTabs();
		tabs.setSelectedIndex(index);
	}

	class BasicPanel extends JPanel
	{
		SlidePanel slidePanel;

		BasicPanel()
		{
			slidePanel = new SlidePanel(data, Prefs.lrt_window, Prefs.lrt_step);

			DoeLayout layout = new DoeLayout();
			add(layout.getPanel(), BorderLayout.NORTH);

			JLabel info1 = new JLabel(
					"Please confirm the current settings for "
							+ "running LRT. Additional configuration is also");
			JLabel info2 = new JLabel("<html>available by selecting the <b>"
					+ "Advanced</b> tab and modifying the options found there."
					+ "</html>");

			layout.add(info1, 0, 0, 1, 1, new Insets(5, 5, 2, 5));
			layout.add(info2, 0, 1, 1, 1, new Insets(0, 5, 10, 5));
			layout.add(slidePanel, 0, 2, 1, 1, new Insets(5, 5, 0, 5));
		}

		void saveSettings()
		{
			Prefs.lrt_window = slidePanel.getWindowSize();
			Prefs.lrt_step = slidePanel.getStepSize();
		}
	}

	static class AdvancedPanel extends JScrollPane
	{
		JLabel lRuns, lModel, lType, lCompute;

		JComboBox cModel, cType, cCompute;

		private SpinnerNumberModel runsModel;

		private JSpinner cRuns;

		AdvancedPanel()
		{
			setPreferredSize(new Dimension(50, 50));

			// Threshold runs
			runsModel = new SpinnerNumberModel(Prefs.lrt_runs, 10, 1000, 1);
			cRuns = new JSpinner(runsModel);
			((JSpinner.NumberEditor) cRuns.getEditor())
					.getTextField()
					.setToolTipText(
							"Number of times to re-run the analysis to calculate the threshold values");
			lRuns = new JLabel("Number of bootstrapping threshold runs:");

			// Substitution model
			String[] v4 =
			{ "Jukes Cantor", "Felsenstein 84" };
			cModel = new JComboBox(v4);
			cModel.setSelectedIndex(Prefs.lrt_method - 1);
			cModel
					.setToolTipText("Nucleotide substitution model to use in distance matrix calculations");
			lModel = new JLabel("Nucleotide substitution model:");

			DoeLayout layout = new DoeLayout();
			setViewportView(layout.getPanel());
			setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
			getVerticalScrollBar().setUnitIncrement(10);

			layout.add(lRuns, 0, 1, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cRuns, 1, 1, 1, 1, new Insets(5, 5, 5, 10));

			layout.add(lModel, 0, 2, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cModel, 1, 2, 1, 1, new Insets(5, 5, 5, 10));
		}

		void saveSettings()
		{
			Prefs.lrt_runs = runsModel.getNumber().intValue();
			Prefs.lrt_method = cModel.getSelectedIndex() + 1;
		}
	}
}
