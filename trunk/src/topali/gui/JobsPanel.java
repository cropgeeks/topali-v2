package topali.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.cluster.jobs.CodeMLLocalJob;
import topali.cluster.jobs.CodeMLRemoteJob;
import topali.cluster.jobs.DSSLocalJob;
import topali.cluster.jobs.DSSRemoteJob;
import topali.cluster.jobs.HMMLocalJob;
import topali.cluster.jobs.HMMRemoteJob;
import topali.cluster.jobs.LRTLocalJob;
import topali.cluster.jobs.LRTRemoteJob;
import topali.cluster.jobs.MBTreeLocalJob;
import topali.cluster.jobs.MBTreeRemoteJob;
import topali.cluster.jobs.PDM2LocalJob;
import topali.cluster.jobs.PDM2RemoteJob;
import topali.cluster.jobs.PDMLocalJob;
import topali.cluster.jobs.PDMRemoteJob;
import topali.data.AlignmentData;
import topali.data.AnalysisResult;
import topali.data.CodeMLResult;
import topali.data.DSSResult;
import topali.data.HMMResult;
import topali.data.LRTResult;
import topali.data.MBTreeResult;
import topali.data.PDM2Result;
import topali.data.PDMResult;
import topali.gui.jobs.CodeMLJobEntry;
import topali.gui.jobs.NoTrackingJobEntry;
import topali.gui.jobs.ProgressBarJobEntry;
import doe.GradientPanel;

public class JobsPanel extends JPanel {
	private WinMain winMain;

	private final JobsThread jobsThread;

	private static JTextArea infoText;

	private final JPanel jp;

	public Vector<JobsPanelEntry> jobs;

	public JobsPanel(WinMain winMain) {
		this.winMain = winMain;

		jobs = new Vector<JobsPanelEntry>();

		jp = new JPanel();
		// jp.setLayout(new GridBagLayout());
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.setBackground(Color.WHITE);

		JScrollPane sp = new JScrollPane(jp);

		GradientPanel gp = new GradientPanel(Text.Gui
				.getString("JobsPanel.gui01"));
		gp.setStyle(gp.OFFICE2003);

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.add(gp, BorderLayout.NORTH);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(sp, BorderLayout.CENTER);

		JSplitPane splits = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splits.setTopComponent(mainPanel);
		splits.setBottomComponent(getControlPanel());
		splits.setResizeWeight(0.85);
		splits.setDividerLocation(0.85);

		setLayout(new BorderLayout());
		add(splits);

		jobsThread = new JobsThread(this);
		jobsThread.start();
	}

	private JPanel getControlPanel() {
		infoText = new JTextArea();
		Utils.setTextAreaDefaults(infoText);
		JScrollPane sp = new JScrollPane(infoText);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p2.add(sp);

		return p2;
	}

	public void addJob(AnalysisJob job, JobsPanelEntry e) {
		jobs.add(e);

		e.setAlignmentX(JPanel.TOP_ALIGNMENT);
		jp.add(e);

		jobsThread.interrupt();
		setStatusPanel();
		WinMainMenuBar.aFileSave.setEnabled(true);
	}

	void removeJobEntry(JobsPanelEntry entry, boolean getResults) {
		jobs.remove(entry);
		jp.remove(entry);

		AnalysisJob job = entry.getJob();
		job.getResult().endTime = System.currentTimeMillis();

		// Move its results into main window
		if (getResults) {
			winMain.navPanel.addResultsNode(null, job.getAlignmentData(), job
					.getResult());
		}
		else
			job.getAlignmentData().removeResult(job.getResult());
		
		setStatusPanel();
		WinMainMenuBar.aFileSave.setEnabled(true);
		jp.repaint();
	}

	public void cancelJob(JobsPanelEntry e) {
		// Then send the cancel request...
		int status = e.getJob().getStatus();
		if (status != JobStatus.COMPLETING && status != JobStatus.COMPLETED) {
			e.getJob().setStatus(JobStatus.CANCELLING);
			e.updateStatus();
			jobsThread.interrupt();
		}
	}

	// Formats the status bar in the bottom-right corner of the screen with a
	// message describing the number of analysis jobs that are running
	void setStatusPanel() {
		// Count the jobs types
		int lCount = 0, rCount = 0;
		float progress = 0;
		for (JobsPanelEntry entry : jobs) {
			AnalysisJob job = entry.getJob();

			if (job.getResult().isRemote)
				rCount++;
			else
				lCount++;
		}

		// int percent = (int) (progress / model.size());

		// "[n] job" or "[n] jobs"
		String js = Text.Gui.getString("JobsPanel.gui04");
		if (lCount + rCount == 1)
			js = Text.Gui.getString("JobsPanel.gui03");
		// Format the message
		Object[] args = { jobs.size(), js, lCount, rCount };
		String msg = Text.format(Text.Gui.getString("JobsPanel.gui02"), args);

		if (jobs.size() > 0)
			msg += " - " + (int) progress + "%";

		// Display it
		WinMainStatusBar.setJobText(msg, false);
	}

	public void createJob(AnalysisResult result, AlignmentData data) {
		AnalysisJob job = null;
		JobsPanelEntry entry = null;
		
		// Reset any local jobs to a status of STARTING so that they DO restart
		if (result.isRemote == false)
			result.status = JobStatus.STARTING;

		// PDM jobs
		if (result instanceof PDMResult) {
			if (result.isRemote)
				job = new PDMRemoteJob((PDMResult) result, data);
			else
				job = new PDMLocalJob((PDMResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// PDM2 jobs
		if (result instanceof PDM2Result) {
			if (result.isRemote)
				job = new PDM2RemoteJob((PDM2Result) result, data);
			else
				job = new PDM2LocalJob((PDM2Result) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// HMM jobs
		if (result instanceof HMMResult) {
			if (result.isRemote)
				job = new HMMRemoteJob((HMMResult) result, data);
			else
				job = new HMMLocalJob((HMMResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// DSS jobs
		else if (result instanceof DSSResult) {
			if (result.isRemote)
				job = new DSSRemoteJob((DSSResult) result, data);
			else
				job = new DSSLocalJob((DSSResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// LRT jobs
		else if (result instanceof LRTResult) {
			if (result.isRemote)
				job = new LRTRemoteJob((LRTResult) result, data);
			else
				job = new LRTLocalJob((LRTResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// CodeML jobs
		else if (result instanceof CodeMLResult) {
			if (result.isRemote)
				job = new CodeMLRemoteJob((CodeMLResult) result, data);
			else
				job = new CodeMLLocalJob((CodeMLResult) result, data);
			entry = new CodeMLJobEntry(job);
		}

		// MrBayes jobs
		else if (result instanceof MBTreeResult) {
			if (result.isRemote)
				job = new MBTreeRemoteJob((MBTreeResult) result, data);
			else
				job = new MBTreeLocalJob((MBTreeResult) result, data);
			entry = new NoTrackingJobEntry(job);
		}

		addJob(job, entry);
	}

	boolean hasJobs() {
		return jobs.size() > 0;
	}

	boolean hasJobs(AlignmentData data) {
		for (JobsPanelEntry entry : jobs) {
			AnalysisResult r1 = entry.getJob().getResult();

			for (AnalysisResult r2 : data.getResults())
				if (r1 == r2)
					return true;
		}
		return false;
	}

	public void clear() {
		// Remove knowledge of any jobs
		jobs.clear();
		jp.removeAll();

		// And update the text/icon to reflect this
		setStatusPanel();
		WinMainStatusBar.resetIcon = true;
		WinMainStatusBar.setStatusIcon(WinMainStatusBar.OFF);
	}

}