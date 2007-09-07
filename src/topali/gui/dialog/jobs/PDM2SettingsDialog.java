// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.jobs;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import topali.data.*;
import topali.gui.*;
import topali.var.Utils;
import doe.*;

public class PDM2SettingsDialog extends JDialog implements ActionListener
{
	private JTabbedPane tabs;

	private JButton bRun, bCancel, bDefault, bHelp;

	private BasicPanel basicPanel;

	private AlignmentData data;

	private PDM2Result result = null;

	public PDM2SettingsDialog(WinMain winMain, AlignmentData data,
			PDM2Result iResult)
	{
		super(
				winMain,
				"Probabilistic Divergence Measures (Enhanced) - Confirm Settings",
				true);
		this.data = data;

		if (iResult != null)
			setInitialSettings(iResult);

		bRun = new JButton("Run");
		bRun.addActionListener(this);
		bCancel = new JButton("Cancel");
		bCancel.addActionListener(this);
		bDefault = new JButton("Defaults");
		bDefault.addActionListener(this);
		bHelp = TOPALiHelp.getHelpButton("pdm_settings");

		tabs = new JTabbedPane();
		basicPanel = new BasicPanel();
		tabs.addTab("Basic", basicPanel);

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

	public PDM2Result getPDM2Result()
	{
		return result;
	}

	private void createPDMObject(boolean makeRemote)
	{
		SequenceSet ss = data.getSequenceSet();

		result = new PDM2Result();

		if (Prefs.isWindows)
		{
			result.mbPath = Utils.getLocalPath() + "mb.exe";
			result.treeDistPath = Utils.getLocalPath() + "treedist.exe";
		} else
		{
			result.mbPath = Utils.getLocalPath() + "mb/mb";
			result.treeDistPath = Utils.getLocalPath() + "treedist/treedist";
		}

		result.selectedSeqs = ss.getSelectedSequenceSafeNames();
		result.isRemote = makeRemote;

		result.pdm_window = Prefs.pdm2_window;
		result.treeToolTipWindow = Prefs.pdm2_window;
		result.pdm_step = Prefs.pdm2_step;
	}

	private void setInitialSettings(PDM2Result iResult)
	{
		Prefs.pdm2_window = iResult.pdm_window;
		Prefs.pdm2_step = iResult.pdm_step;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bCancel)
			setVisible(false);

		else if (e.getSource() == bRun)
		{
			basicPanel.saveSettings();

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
		Prefs.setPDM2Defaults();

		// Now recreate the panels with these new values
		basicPanel = new BasicPanel();

		int index = tabs.getSelectedIndex();
		tabs.removeAll();
		tabs.addTab("Basic", basicPanel);
		tabs.setSelectedIndex(index);
	}

	class BasicPanel extends JPanel
	{
		SlidePanel slidePanel;

		BasicPanel()
		{
			slidePanel = new SlidePanel(data, Prefs.pdm2_window,
					Prefs.pdm2_step);

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
			Prefs.pdm2_window = slidePanel.getWindowSize();
			Prefs.pdm2_step = slidePanel.getStepSize();
		}
	}
}
