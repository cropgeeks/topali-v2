package topali.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import topali.data.*;
import topali.cluster.jobs.*;
import static topali.cluster.JobStatus.*;
import static topali.gui.WinMainStatusBar.*;

class JobsPanelEntry extends JPanel
{
	private AnalysisJob job;
	private String startStr;
	
	private JLabel jobLabel, statusLabel, timeLabel, iconLabel;
	private JProgressBar pBar;
	
	private Color bgColor = (Color) UIManager.get("list.background");
		
	JobsPanelEntry(AnalysisJob job)
	{
		this.job = job;
		
		setBackground(bgColor);
		setLayout(new BorderLayout(5, 5));
		setBorder(BorderFactory.createTitledBorder("JobId: N/A"));
				
		pBar = new JProgressBar();
		pBar.setStringPainted(true);
		pBar.setValue(0);
		pBar.setPreferredSize(new Dimension(50, 20));
		pBar.setMaximum(100);
		pBar.setBorderPainted(false);
		pBar.setForeground(new Color(140, 165, 214));
		pBar.setCursor(new Cursor(Cursor.HAND_CURSOR));
				
		if (job.getResult() instanceof TreeResult)
		{
			pBar.setString("non-trackable job");
			pBar.setIndeterminate(true);
		}
		
		jobLabel = new JLabel(job.getResult().jobName);
		statusLabel = new JLabel("Starting job...");
		iconLabel = new JLabel(Icons.STATUS_OFF);
		
		long time = job.getResult().startTime;
		startStr = "Submitted: " + new Date(time).toString();
		timeLabel = new JLabel(startStr);		
		
		JPanel p1 = new JPanel(new GridLayout(2, 1, 5, 5));
		p1.setBackground(bgColor);
		p1.add(jobLabel);
		p1.add(statusLabel);
		
		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.setBackground(bgColor);
		p2.add(pBar);
		p2.add(iconLabel, BorderLayout.EAST);
		
		JPanel p3 = new JPanel(new BorderLayout(5, 5));
		p3.setBackground(bgColor);
		p3.add(p1, BorderLayout.NORTH);
		p3.add(p2);
		p3.add(timeLabel, BorderLayout.SOUTH);
				
		add(p3);
		if (job.getResult().isRemote)
			add(new JLabel(Icons.COMMS), BorderLayout.WEST);
		else
			add(new JLabel(Icons.LOCAL), BorderLayout.WEST);
	}
	
	void setJobId(String jobId)
	{
		String title = "JobId: " + jobId + " ";
		if (job.getResult().isRemote)
			title += "(remote)";
		else
			title += "(local)";
		
		setBorder(BorderFactory.createTitledBorder(title));
	}
	
	void updateStatus()
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
				statusLabel.setText("Queued - waiting to run...");
				WinMainStatusBar.setStatusIcon(GRE);
				break;
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
				statusLabel.setText("Fatal job error...");
				break;
		}
	}
	
	long h=0, m=0, s=0;
	long elapsed;
	
	void setTimeLabel(long start, long current)
	{
//		if (job.getStatus() == COMPLETE || job.getStatus() == ERROR)
//			return;
		
		elapsed = current - start;

		h = (elapsed/(1000*60*60));
		m = (elapsed-(h*1000*60*60))/(1000*60);
//		s = (elapsed-(h*1000*60*60)-(m*1000*60))/1000;
		
		timeLabel.setText(startStr + " (Runtime: "
			+ h + "h:" + Prefs.i2.format(m) + "m)");
	}
	
	void setProgress(float progress)
		{ pBar.setValue((int)progress); }
	
	int getProgress()
		{ return pBar.getValue(); }
	
	AnalysisJob getJob()
		{ return job; }
		
	void setSelected(boolean selected)
	{
		if (selected)
			jobLabel.setFont(jobLabel.getFont().deriveFont(Font.BOLD));
		else
			jobLabel.setFont(jobLabel.getFont().deriveFont(Font.PLAIN));
	}
}