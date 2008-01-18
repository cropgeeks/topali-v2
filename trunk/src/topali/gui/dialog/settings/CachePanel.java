// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui.dialog.settings;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

import scri.commons.multicore.TokenManager;
import topali.cluster.*;
import topali.gui.Prefs;
import topali.var.*;
import topali.var.utils.Utils;
import doe.MsgBox;

class CachePanel extends JPanel implements ActionListener
{
	private JLabel sizeLabel;

	private JButton bClear;

	private SpinnerNumberModel cpuModel;

	private JSpinner cpuSpin;

	public CachePanel()
	{
		setLayout(new BorderLayout());
		add(createControls());
	}

	private JPanel createControls()
	{
		JPanel p1 = new JPanel(new FlowLayout(5, 5, FlowLayout.LEFT));
		p1.setBorder(BorderFactory.createTitledBorder("Local cache:"));

		sizeLabel = new JLabel();
		updateLabel();

		bClear = new JButton("Clear");
		bClear.addActionListener(this);
		if (LocalJobs.jobsRunning())
			bClear.setEnabled(false);

		p1.add(sizeLabel);
		p1.add(bClear);

		JPanel p2 = new JPanel(new FlowLayout(5, 5, FlowLayout.LEFT));
		p2.setBorder(BorderFactory.createTitledBorder("Processor options:"));

		int max = Runtime.getRuntime().availableProcessors();
		cpuModel = new SpinnerNumberModel(Prefs.gui_max_cpus, 1, max, 1);
		cpuSpin = new JSpinner(cpuModel);
		Dimension d = cpuSpin.getPreferredSize();
		d.width = 55;
		cpuSpin.setPreferredSize(d);
		if (LocalJobs.jobsRunning())
			cpuSpin.setEnabled(false);

		JLabel cpuLabel = new JLabel("Maximum number of CPUs/Cores to use "
				+ "for local jobs: ");
		p2.add(cpuLabel);
		p2.add(cpuSpin);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel.add(p1, BorderLayout.NORTH);
		panel.add(p2);

		return panel;
	}

	private void updateLabel()
	{
		setText(-1);

		Runnable r = new Runnable()
		{
			public void run()
			{
				long size = determineSize(SysPrefs.tmpDir);
				setText(size);
			}
		};
		new Thread(r).start();
	}

	private void setText(float size)
	{
		String txt = "TOPALi temporary working directory: ";

		float mb = size / 1025 / 1024f;

		if (size == -1)
			txt += "(calculating)";
		else
			txt += Utils.d2.format(mb) + " MB in use";

		sizeLabel.setText(txt);
	}

	boolean isOK()
	{
		Prefs.gui_max_cpus = cpuModel.getNumber().intValue();

		if (LocalJobs.jobsRunning() == false)
			LocalJobs.manager = new TokenManager(Prefs.gui_max_cpus);

		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == bClear)
		{
			String msg = "You should be certain that no locally running jobs "
					+ "are in progress before continuing, otherwise they are "
					+ "likely to fail. Continue clearing cache?";

			if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
				return;
			else
			{
				ClusterUtils.emptyDirectory(SysPrefs.tmpDir, false);
				updateLabel();
			}
		}
	}

	// Works out (in bytes) how much disk space is used by the given directory
	private long determineSize(File dir)
	{
		long size = 0;
		for (File f : dir.listFiles())
		{
			if (f.isDirectory())
				size += determineSize(f);
			else
				size += f.length();
		}

		return size;
	}
}