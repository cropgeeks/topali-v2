// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import doe.*;

import topali.analyses.*;
import topali.data.*;
import topali.gui.*;

public class DSSSettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;
	private JButton bRun, bCancel, bDefault, bHelp;
	private BasicPanel basicPanel;
	private AdvancedPanel advancedPanel;
	
	private AlignmentData data;
	private DSSResult result = null;

	public DSSSettingsDialog(WinMain winMain, AlignmentData data, DSSResult iResult)
	{
		super(winMain, Text.GuiDiag.getString("DSSSettingsDialog.gui01"), true);
		this.data = data;
		
		if (iResult != null)
			setInitialSettings(iResult);
		
		bRun = new JButton(Text.GuiDiag.getString("DSSSettingsDialog.gui02"));
		bRun.addActionListener(this);
		bCancel = new JButton(Text.Gui.getString("cancel"));
		bCancel.addActionListener(this);
		bDefault = new JButton(Text.Gui.getString("defaults"));
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("dss_settings");
		
		tabs = new JTabbedPane();
		addTabs();

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
		
		add(tabs, BorderLayout.CENTER);
		add(p2, BorderLayout.SOUTH);

		
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
		tabs.addTab(Text.GuiDiag.getString("DSSSettingsDialog.gui04"),
			basicPanel);
		tabs.addTab(Text.GuiDiag.getString("DSSSettingsDialog.gui05"),
			advancedPanel);
	}
	
	public DSSResult getDSSResult()
		{ return result; }
	
	private void createDSSObject(boolean makeRemote)
	{
		SequenceSet ss = data.getSequenceSet();
		
		result = new DSSResult();
		
		if (Prefs.isWindows)
			result.fitchPath = Utils.getLocalPath() + "fitch.exe";
		else if (Prefs.isMacOSX)
			result.fitchPath = Utils.getLocalPath() + "fitch";
		else
			result.fitchPath = Utils.getLocalPath() + "fitch/fitch";
		
		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = makeRemote;
		
		// Do we need to estimate parameters?
		if (ss.hasParametersEstimated() == false)
			SequenceSetUtils.estimateParameters(ss);
		
		result.window = Prefs.dss_window;
		result.treeToolTipWindow = Prefs.dss_window;
		result.step = Prefs.dss_step;
		result.runs = Prefs.dss_runs + 1;
		
		result.method = Prefs.dss_method;
		result.power = Prefs.dss_power;
		result.passCount = Prefs.dss_pass_count;
		result.avgDist = topali.cluster.dss.DSS.getAverageDistance(ss);
		result.tRatio = ss.getParams().getTRatio();
		result.alpha = ss.getParams().getAlpha();
	}
	
	private void setInitialSettings(DSSResult iResult)
	{
		Prefs.dss_window = iResult.window;
		Prefs.dss_step = iResult.step;
		Prefs.dss_runs = iResult.runs -1;
		
		Prefs.dss_method = iResult.method;
		Prefs.dss_power = iResult.power;
		Prefs.dss_pass_count = iResult.passCount;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);
		
		else if (e.getSource() == bRun)
		{
			basicPanel.saveSettings();
			advancedPanel.saveSettings();
		
			createDSSObject((e.getModifiers() & e.CTRL_MASK) == 0);
			setVisible(false);
		}
		
		else if (e.getSource() == bDefault)
			defaultClicked();
	}
		
	private void defaultClicked()
	{
		int res = MsgBox.yesno(
			Text.GuiDiag.getString("DSSSettingsDialog.gui06"), 1);
		if (res != JOptionPane.YES_OPTION)
			return;
	
		// Tell the preferences object to reset them
		Prefs.setDSSDefaults();
	
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
			slidePanel = new SlidePanel(data, Prefs.dss_window, Prefs.dss_step);
			
			DoeLayout layout = new DoeLayout();		
			add(layout.getPanel(), BorderLayout.NORTH);

			JLabel info1 = new JLabel("Please confirm the current settings for "
				+ "running DSS. Additional configuration is also");
			JLabel info2 = new JLabel("<html>available by selecting the <b>"
				+ "Advanced</b> tab and modifying the options found there."
				+ "</html>");
				
			layout.add(info1, 0, 0, 1, 1, new Insets(5, 5, 2, 5));
			layout.add(info2, 0, 1, 1, 1, new Insets(0, 5, 10, 5));
			layout.add(slidePanel, 0, 2, 1, 1, new Insets(5, 5, 0, 5));
		}
		
		void saveSettings()
		{
			Prefs.dss_window = slidePanel.getWindowSize();
			Prefs.dss_step = slidePanel.getStepSize();
		}
	}
	
	static class AdvancedPanel extends JScrollPane
	{
		JLabel lPower, lRuns, lModel, lType, lCompute, lPass;
		JComboBox cPower, cModel, cType, cCompute, cPass;
		private SpinnerNumberModel runsModel;
		private JSpinner cRuns;
	
		AdvancedPanel()
		{
			setPreferredSize(new Dimension(50, 50));
		
			// Threshold runs
			runsModel = new SpinnerNumberModel(Prefs.dss_runs, 10, 1000, 1);
			cRuns = new JSpinner(runsModel);
			((JSpinner.NumberEditor)cRuns.getEditor()).getTextField()
				.setToolTipText("Number of times to re-run the analysis to calculate the threshold values");
			lRuns = new JLabel("Number of bootstrapping threshold runs:");
		
/*			// Threshold computation method
			String[] v2 = { "Yes", "No" };
			cCompute = new JComboBox(v2);
			cCompute.addActionListener(this);
			cCompute.setSelectedItem(Prefs.dss_threshold_boot);
			cCompute.setToolTipText("Calculate a threshold via parametric bootstrapping"); 
			lCompute = new JLabel("Calculate threshold:");	
*/			
			// Least squares power
			String[] v3 = { "Unweighted (power=0)", "Weighted (power=2)" };
			cPower = new JComboBox(v3);
			cPower.setSelectedIndex(Prefs.dss_power -1);
			cPower.setToolTipText("Least squares method to use when calculating sum of squares");
			cPower.setEnabled(false);
			lPower = new JLabel("Least squares method:");
			lPower.setEnabled(false);
			
/*			// Analysis type
			String[] v1 = { "Fast (but very approximate)", "Slow (but accurate - using Fitch)" };
			cType = new JComboBox(v1);
			cType.addActionListener(this);
			cType.setSelectedItem(Prefs.dss_type);
			cType.setToolTipText("Analysis method used to optimize branch lengths/calculate sum of squares");
			lType = new JLabel("DSS analysis method:");
*/			
			// Substitution model
			String[] v4 = { "Jukes Cantor", "Felsenstein 84" };
			cModel = new JComboBox(v4);
			cModel.setSelectedIndex(Prefs.dss_method - 1);
			cModel.setToolTipText("Nucleotide substitution model to use in distance matrix calculations"); 
			lModel = new JLabel("Nucleotide substitution model:");			
			
			// Number of passes
			String[] v5 = { "One", "Two" };
			cPass = new JComboBox(v5);
			cPass.setSelectedIndex(Prefs.dss_pass_count -1);
			cPass.setToolTipText("Number of passes to perform over the alignment (one: forward only, two: "
				+ "forward and backward)");
			lPass = new JLabel("Number of passes:");
			
		
			DoeLayout layout = new DoeLayout();
			setViewportView(layout.getPanel());
			setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
			getVerticalScrollBar().setUnitIncrement(10);
			
//			layout.add(lType, 0, 0, 0, 1, new Insets(10, 10, 5, 5));
//			layout.add(cType, 1, 0, 1, 1, new Insets(10, 5, 5, 10));
			
//			layout.add(lCompute, 0, 1, 0, 1, new Insets(5, 10, 5, 5));
//			layout.add(cCompute, 1, 1, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lRuns, 0, 2, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cRuns, 1, 2, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lPower, 0, 3, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cPower, 1, 3, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lModel, 0, 4, 0, 1, new Insets(5, 10, 5, 5));
			layout.add(cModel, 1, 4, 1, 1, new Insets(5, 5, 5, 10));
			
			layout.add(lPass, 0, 5, 0, 1, new Insets(5, 10, 10, 5));
			layout.add(cPass, 1, 5, 1, 1, new Insets(5, 5, 10, 10));
		}
	
		void saveSettings()
		{
			Prefs.dss_runs = runsModel.getNumber().intValue();			
			Prefs.dss_power = cPower.getSelectedIndex() +1;
			Prefs.dss_method = cModel.getSelectedIndex() + 1;
			Prefs.dss_pass_count = cPass.getSelectedIndex() +1;
			
/*			Prefs.dss_model = (String) cModel.getSelectedItem();
			Prefs.dss_type = (String) cType.getSelectedItem();
			Prefs.dss_threshold_boot = (String) cCompute.getSelectedItem();
			Prefs.dss_pass_count = (String) cPass.getSelectedItem();
*/			
		}
	}
}
