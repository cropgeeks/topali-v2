// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import static topali.cluster.JobStatus.*;
import static topali.gui.WinMainStatusBar.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Date;

import javax.swing.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.var.utils.Utils;
import scri.commons.gui.MsgBox;

public abstract class JobsPanelEntry extends JPanel implements MouseListener
{
	private AnalysisJob job;

	private String startStr;
	private String qCount = "-1";

	private JLabel jobLabel, statusLabel, timeLabel, iconLabel, cancelLabel;

	protected Color bgColor = (Color) UIManager.get("list.background");

	boolean isSelected = false;
	
	public JobsPanelEntry(AnalysisJob job)
	{
		this.job = job;

		setBackground(bgColor);
		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createTitledBorder("JobId: N/A"));

		jobLabel = new JLabel(job.getResult().jobName);
		statusLabel = new JLabel("Starting job...");
		iconLabel = new JLabel(Icons.STATUS_OFF);

		long time = job.getResult().startTime;
		startStr = "Submitted: " + new Date(time).toString();
		timeLabel = new JLabel(startStr);

		JPanel header = new JPanel(new GridBagLayout());
		header.setBackground(bgColor);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.LINE_START;
		header.add(jobLabel, c);
		c.gridx = 0;
		c.gridy = 1;
		header.add(statusLabel, c);

		cancelLabel = new JLabel("<html><u>Cancel</u></html>");
		cancelLabel.setToolTipText("Cancel this job.");
		cancelLabel.setForeground(Color.BLUE);
		cancelLabel.addMouseListener(this);
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.LINE_END;
		header.add(cancelLabel, c);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.setBackground(bgColor);
		p2.add(getProgressComponent());
		p2.add(iconLabel, BorderLayout.EAST);

		JPanel p3 = new JPanel(new BorderLayout(5, 5));
		p3.setBackground(bgColor);
		p3.add(header, BorderLayout.NORTH);
		p3.add(p2);
		p3.add(timeLabel, BorderLayout.SOUTH);

		add(p3);

		if (job.getResult().isRemote)
			add(new JLabel(Icons.COMMS), BorderLayout.WEST);
		else
			add(new JLabel(Icons.LOCAL), BorderLayout.WEST);

		this.addMouseListener(this);
	}

	public abstract JComponent getProgressComponent();

	public void setJobStatus(JobStatus status)
	{
		if (status.status == JobStatus.QUEUING)
			qCount = status.text;
	}

	public void setJobId(String jobId)
	{
		String title = "JobId: " + jobId + " ";
		if (job.getResult().isRemote)
		{
			if (job.getResult().url.startsWith("http://www.topali.org"))
				title += "(running on the Scottish Crop Research Institute HPC cluster)";
			else if (job.getResult().url.startsWith("http://compbio.dundee"))
				title += "(running on the University of Dundee Barton Group HPC cluster)";
			else
				title += "(running remotely)";
		}
		else
			title += "(running locally)";



		setBorder(BorderFactory.createTitledBorder(title));
	}

	public void updateStatus()
	{
		statusLabel.setForeground(Color.black);
		iconLabel.setIcon(Icons.STATUS_GRE);

		switch (job.getStatus())
		{
		case UNKNOWN:
			iconLabel.setIcon(Icons.STATUS_BLU);
			WinMainStatusBar.setStatusIcon(BLU);
			statusLabel.setForeground(Color.blue);
			statusLabel.setText("Unknown job status...");
			break;

		case STARTING:
			statusLabel.setText("Starting job...");
			WinMainStatusBar.setStatusIcon(GRE);
			break;
		case QUEUING:
		{
			String txt = "waiting to run...";
			int q = Integer.parseInt(qCount);
			if (q >= 0)
				txt = q + (q==1 ? " job " : " jobs ") + "ahead of you...";

			statusLabel.setText("Queued - " + txt);
			WinMainStatusBar.setStatusIcon(GRE);
			break;
		}
		case RUNNING:
			statusLabel.setText("Running...");
			WinMainStatusBar.setStatusIcon(GRE);
			break;
		case HOLDING:
			statusLabel.setText("Holding - waiting to resume...");
			WinMainStatusBar.setStatusIcon(GRE);
			break;
		case COMPLETING:
			statusLabel.setText("Completed - waiting to retrieve result...");
			WinMainStatusBar.setStatusIcon(GRE);
			break;

		case CANCELLING:
			statusLabel.setText("Cancelling...");
			WinMainStatusBar.setStatusIcon(GRE);
			break;

		case COMPLETED:
		case CANCELLED:
			WinMainStatusBar.setStatusIcon(OFF);
			break;

		case COMMS_ERROR:
			iconLabel.setIcon(Icons.STATUS_BLU);
			WinMainStatusBar.setStatusIcon(BLU);
			statusLabel.setForeground(Color.blue);
			statusLabel.setText("Communication error - waiting to retry...");
			break;

		case FATAL_ERROR:
			iconLabel.setIcon(Icons.STATUS_RED);
			WinMainStatusBar.setStatusIcon(RED);
			statusLabel.setForeground(Color.red);
			statusLabel.setText("Fatal job error (click for details)");
			break;
		}
	}

	long h = 0, m = 0;

	long elapsed;

	public void setTimeLabel(long start, long current)
	{
		elapsed = current - start;

		h = (elapsed / (1000 * 60 * 60));
		m = (elapsed - (h * 1000 * 60 * 60)) / (1000 * 60);

		timeLabel.setText(startStr + " (Runtime: " + h + "h:"
				+ Utils.i2.format(m) + "m)");
	}

	AnalysisJob getJob()
	{
		return job;
	}

	public void setSelected(boolean selected)
	{
	    this.isSelected = selected;
	    
		if (selected)
			jobLabel.setFont(jobLabel.getFont().deriveFont(Font.BOLD));
		else
			jobLabel.setFont(jobLabel.getFont().deriveFont(Font.PLAIN));
	}

	// Have to override this, to prevent JobsPanel's BoxLayout from scaling this
	// Component to it's
	// maximum size.
	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(super.getMaximumSize().width,
				getPreferredSize().height);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return new Dimension(super.getMaximumSize().width,
				getPreferredSize().height);
	}

	public void mouseClicked(MouseEvent e)
	{
	    
		if (e.getSource().equals(cancelLabel))
		{
			String msg = job.getResult().jobName
					+ " - are you sure you wish to cancel this job?";
			if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
				return;
			else
			{
				WinMain.jobsPanel.cancelJob(this);
				if(isSelected)
				    WinMain.jobsPanel.clearInfoPanel();
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				cancelLabel.setText("<html>Cancel</html>");
				cancelLabel.setForeground(Color.LIGHT_GRAY);
				cancelLabel.removeMouseListener(this);
			}
		}
		

	}

	public void mousePressed(MouseEvent e)
	{
		if (!e.getSource().equals(cancelLabel))
			WinMain.jobsPanel.select(this);
	}

	public void mouseEntered(MouseEvent e)
	{
		if (e.getSource().equals(cancelLabel))
		{
			setCursor(new Cursor(Cursor.HAND_CURSOR));
			cancelLabel.setForeground(Color.CYAN);
			cancelLabel.repaint();
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (e.getSource().equals(cancelLabel))
		{
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			cancelLabel.setForeground(Color.BLUE);
			cancelLabel.repaint();
		}
	}

	public void mouseReleased(MouseEvent e)
	{
	}
}