// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.util.Vector;

import javax.swing.*;

import topali.cluster.JobStatus;
import topali.cluster.jobs.*;
import topali.data.*;
import topali.gui.jobs.*;
import topali.var.Utils;
import doe.*;

public class JobsPanel extends JPanel
{

	private final JobsThread jobsThread;

	public static JTextArea infoText;

	private final JPanel jp;

	public Vector<JobsPanelEntry> jobs;

	public JobsPanel()
	{
		jobs = new Vector<JobsPanelEntry>();

		jp = new JPanel();
		// jp.setLayout(new GridBagLayout());
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
		jp.setBackground(Color.WHITE);

		JScrollPane sp = new JScrollPane(jp);

		GradientPanel gp = new GradientPanel(Text.Gui
				.getString("JobsPanel.gui01"));
		gp.setStyle(GradientPanel.OFFICE2003);

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

		setStatusPanel();
	}

	private JPanel getControlPanel()
	{
		infoText = new JTextArea();
		Utils.setTextAreaDefaults(infoText);
		JScrollPane sp = new JScrollPane(infoText);

		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p2.add(sp);

		return p2;
	}

	public void addJob(JobsPanelEntry e)
	{
		jobs.add(e);

		e.setAlignmentX(Component.TOP_ALIGNMENT);
		jp.add(e);

		jobsThread.interrupt();
		setStatusPanel();
		//WinMainMenuBar.aFileSave.setEnabled(true);
		//WinMainMenuBar.aVamCommit.setEnabled(true);
		ProjectState.setDataChanged();
	}

	void removeJobEntry(JobsPanelEntry entry, boolean getResults)
	{
		jobs.remove(entry);
		jp.remove(entry);

		AnalysisJob job = entry.getJob();
		job.getResult().endTime = System.currentTimeMillis();

		// Move its results into main window
		if (getResults)
		{
			AnalysisResult res = job.getResult();
			
			if(!res.warning.equals("")) {
				String msg = "Job '"+res.guiName+"' finished with a warning:\n\n";
				msg += res.warning;
				MsgBox.msg(msg, MsgBox.WAR);
			}
			
			WinMain.navPanel.addResultsNode(null, job.getAlignmentData(), res);
		} else
			job.getAlignmentData().removeResult(job.getResult());

		setStatusPanel();

		//WinMainMenuBar.aFileSave.setEnabled(true);
		//WinMainMenuBar.aVamCommit.setEnabled(true);
		ProjectState.setDataChanged();
		jp.repaint();
	}

	public void cancelJob(JobsPanelEntry e)
	{
		// Then send the cancel request...
		int status = e.getJob().getStatus();

		if (status != JobStatus.COMPLETING && status != JobStatus.COMPLETED)
		{
			if (status != JobStatus.FATAL_ERROR && e.getJob().getResult().isRemote)
				Tracker.log("CANCELLED", e.getJob().getJobId());

			e.getJob().setStatus(JobStatus.CANCELLING);
			e.updateStatus();
			jobsThread.interrupt();
		}
	}

	// Formats the status bar in the bottom-right corner of the screen with a
	// message describing the number of analysis jobs that are running
	void setStatusPanel()
	{
		// Count the jobs types
		int lCount = 0, rCount = 0;
		//float progress = 0;
		for (JobsPanelEntry entry : jobs)
		{
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
		Object[] args =
		{ jobs.size(), js, lCount, rCount };
		String msg = Text.format(Text.Gui.getString("JobsPanel.gui02"), args);

//		if (jobs.size() > 0)
//			msg += " - " + (int) progress + "%";

		// Display it
		WinMainStatusBar.setJobText(msg, false);
		if (jobs.size() == 0)
		{
			WinMainStatusBar.resetIcon = true;
			WinMainStatusBar.setStatusIcon(WinMainStatusBar.OFF);
		}
	}

	public void createJob(AnalysisResult result, AlignmentData data)
	{
		AnalysisJob job = null;
		JobsPanelEntry entry = null;

		// Reset any local jobs to a status of STARTING so that they DO restart
		if (result.isRemote == false)
			result.status = JobStatus.STARTING;

		// PDM jobs
		if (result instanceof PDMResult)
		{
			if (result.isRemote)
				job = new PDMRemoteJob((PDMResult) result, data);
			else
				job = new PDMLocalJob((PDMResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// PDM2 jobs
		if (result instanceof PDM2Result)
		{
			if (result.isRemote)
				job = new PDM2RemoteJob((PDM2Result) result, data);
			else
				job = new PDM2LocalJob((PDM2Result) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// HMM jobs
		if (result instanceof HMMResult)
		{
			if (result.isRemote)
				job = new HMMRemoteJob((HMMResult) result, data);
			else
				job = new HMMLocalJob((HMMResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// DSS jobs
		else if (result instanceof DSSResult)
		{
			if (result.isRemote)
				job = new DSSRemoteJob((DSSResult) result, data);
			else
				job = new DSSLocalJob((DSSResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// LRT jobs
		else if (result instanceof LRTResult)
		{
			if (result.isRemote)
				job = new LRTRemoteJob((LRTResult) result, data);
			else
				job = new LRTLocalJob((LRTResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		// CodeML jobs
		else if (result instanceof CodeMLResult)
		{
			if (result.isRemote)
				job = new CodeMLRemoteJob((CodeMLResult) result, data);
			else
				job = new CodeMLLocalJob((CodeMLResult) result, data);
			entry = new CodeMLJobEntry(job);
		}

		// MrBayes jobs
		else if (result instanceof MBTreeResult)
		{
			if (result.isRemote)
				job = new MrBayesRemoteJob((MBTreeResult) result, data);
			else
				job = new MrBayesLocalJob((MBTreeResult) result, data);
			entry = new ProgressBarJobEntry(job);
		}

		else if(result instanceof PhymlResult) {
			if(result.isRemote)
				job = new PhymlRemoteJob((PhymlResult)result, data);
			else
				job = new PhymlLocalJob((PhymlResult)result, data);
			entry = new NoTrackingJobEntry(job);
		}
		
		else if(result instanceof RaxmlResult) {
			RaxmlResult res = (RaxmlResult)result;
			if(result.isRemote)
				job = new RaxmlRemoteJob(res, data);
			else
				job = new RaxmlLocalJob(res, data);
				
			if(res.bootstrap>0)
				entry = new ProgressBarJobEntry(job);
			else
				entry = new NoTrackingJobEntry(job);
		}

		else if(result instanceof MGResult) {
			if(result.isRemote)
				job = new MGRemoteJob((MGResult)result, data);
			else
				job = new MGLocalJob((MGResult)result, data);
			//entry = new NoTrackingJobEntry(job);
			entry = new ProgressBarJobEntry(job);
		}

		else if(result instanceof ModelTestResult) {
			if(result.isRemote)
				job = new ModelTestRemoteJob((ModelTestResult)result, data);
			else
				job = new ModelTestLocalJob((ModelTestResult)result, data);
			entry = new ProgressBarJobEntry(job);
		}
		
		else if(result instanceof CodonWResult) {
			if(result.isRemote)
				job = new CodonWRemoteJob((CodonWResult)result, data);
			else
				job = new CodonWLocalJob((CodonWResult)result, data);
			entry = new NoTrackingJobEntry(job);
		}

		else if(result instanceof FastMLResult) {
			if(result.isRemote)
				job = new FastMLRemoteJob((FastMLResult)result, data);
			else
				job = new FastMLLocalJob((FastMLResult)result, data);
			entry = new NoTrackingJobEntry(job);
		}

		addJob(entry);
	}

	boolean hasJobs()
	{
		return jobs.size() > 0;
	}

	boolean hasJobs(AlignmentData data)
	{
		for (JobsPanelEntry entry : jobs)
		{
			AnalysisResult r1 = entry.getJob().getResult();

			for (AnalysisResult r2 : data.getResults())
				if (r1 == r2)
					return true;
		}
		return false;
	}

	public void clear()
	{
		// Remove knowledge of any jobs
		jobs.clear();
		jp.removeAll();
		infoText.setText("");

		// And update the text/icon to reflect this
		setStatusPanel();
		WinMainStatusBar.resetIcon = true;
		WinMainStatusBar.setStatusIcon(WinMainStatusBar.OFF);
	}

	public void select(JobsPanelEntry e)
	{
		for (JobsPanelEntry entry : jobs)
			entry.setSelected(false);
		e.setSelected(true);
		infoText.setText(e.getJob().errorInfo);
	}
}