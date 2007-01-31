package topali.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import doe.*;

import topali.data.*;
import topali.cluster.*;
import topali.cluster.jobs.*;

public class JobsPanel extends JPanel implements ListSelectionListener
{
	private WinMain winMain;
	
	private JList list;
	private DefaultListModel model;
	private JobsThread jobsThread;
	
	private static JTextArea infoText;
	private static ServerPanel serverPanel = new ServerPanel();
			
	JobsPanel(WinMain winMain)
	{
		this.winMain = winMain;
		
		model = new DefaultListModel();
		list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new JobsPanelListRenderer());
		list.addListSelectionListener(this);
		
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
					cancelJob(list.locationToIndex(e.getPoint()));
			}
		});
		
		
		JScrollPane sp = new JScrollPane(list);
		GradientPanel gp = new GradientPanel(
			Text.Gui.getString("JobsPanel.gui01"));
		gp.setStyle(gp.OFFICE2003);
		
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.add(gp, BorderLayout.NORTH);
//		headerPanel.add(serverPanel);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(sp);
		
		JSplitPane splits = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splits.setTopComponent(mainPanel);
		splits.setBottomComponent(getControlPanel());
		splits.setResizeWeight(0.85);
		splits.setDividerLocation(0.85);		
		
		setLayout(new BorderLayout());
		add(splits);
		
		jobsThread = new JobsThread(this, model);
		setStatusPanel();
	}
	
	private JPanel getControlPanel()
	{
		infoText = new JTextArea();
		Utils.setTextAreaDefaults(infoText);
		JScrollPane sp = new JScrollPane(infoText);
				
		JPanel p2 = new JPanel(new BorderLayout(5, 5));
		p2.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p2.add(serverPanel, BorderLayout.NORTH);
		p2.add(sp);
		
		return p2;
	}
	
	public void clear()
	{
		// Remove knowledge of any jobs
		model.clear();
//		serverPanel.setVisible(model.size() > 0);
		
		// And update the text/icon to reflect this
		setStatusPanel();
		WinMainStatusBar.resetIcon = true;
		WinMainStatusBar.setStatusIcon(WinMainStatusBar.OFF);
	}
	
	// Formats the status bar in the bottom-right corner of the screen with a
	// message describing the number of analysis jobs that are running
	void setStatusPanel()
	{
		// Count the jobs types
		int lCount = 0, rCount = 0;
		float progress = 0;
		for (int i = 0; i < model.size(); i++)
		{
			JobsPanelEntry entry = (JobsPanelEntry) model.get(i);
			AnalysisJob job = entry.getJob();
			progress += entry.getProgress() / (float) model.size();
			
			if (job.getResult().isRemote)
				rCount++;
			else
				lCount++;
		}
		
//		int percent = (int) (progress / model.size());
		
		// "[n] job" or "[n] jobs"
		String jobs = Text.Gui.getString("JobsPanel.gui04");
		if (lCount + rCount == 1)
			jobs = Text.Gui.getString("JobsPanel.gui03");
		// Format the message
		Object[] args = { model.size(), jobs, lCount, rCount };		
		String msg = Text.format(Text.Gui.getString("JobsPanel.gui02"), args);
		
		if (model.size() > 0)
			msg += " - " + (int) progress + "%";
		
		// Display it
//		WinMainStatusBar.setJobText(msg, (model.size() > 0));
		WinMainStatusBar.setJobText(msg, false);
	}
		
	public void addJob(AnalysisJob job)
	{
		JobsPanelEntry jobEntry = new JobsPanelEntry(job);
		model.addElement(jobEntry);
//		serverPanel.setVisible(model.size() > 0);
						
		jobsThread.interrupt();
		
		setStatusPanel();
		WinMainMenuBar.aFileSave.setEnabled(true);
	}
	
	void removeJobEntry(JobsPanelEntry entry)
	{
		// Remove the entry from the jobsPanel
		model.removeElement(entry);
//		serverPanel.setVisible(model.size() > 0);
		
		AnalysisJob job = entry.getJob();
		job.getResult().endTime = System.currentTimeMillis();
		
		// Move its results into main window		
		winMain.navPanel.addResultsNode(null, job.getAlignmentData(), job.getResult());
		
		setStatusPanel();
		WinMainMenuBar.aFileSave.setEnabled(true);
		winMain.requestFocus();
	}
	
	void cancelJobEntry(JobsPanelEntry entry)
	{
		model.removeElement(entry);
//		serverPanel.setVisible(model.size() > 0);
		
		AnalysisJob job = entry.getJob();
		job.getAlignmentData().removeResult(job.getResult());
		
		setStatusPanel();
		WinMainMenuBar.aFileSave.setEnabled(true);
	}
	
	class JobsPanelListRenderer implements ListCellRenderer 
	{		
		// Set the attributes of the class and return a reference
		public Component getListCellRendererComponent(JList list, Object o,
			int i, boolean iss, boolean chf)
		{
			JobsPanelEntry entry = (JobsPanelEntry) o;
			entry.setSelected(iss);
		
			return entry;
		}
		
		public Insets getInsets(Insets i)
		{
			return new Insets(5, 5, 5, 5);
		}
	}
	
	/* Returns true if any jobs are currently running. */
	boolean hasJobs()
	{
		return model.size() > 0;
	}
	
	/* Returns true if this AlignmentData object has running jobs. */
	boolean hasJobs(AlignmentData data)
	{
		for (int i = 0; i < model.size(); i++)
		{
			JobsPanelEntry entry = (JobsPanelEntry) model.get(i);
			AnalysisResult r1 = entry.getJob().getResult();
			
			for (AnalysisResult r2: data.getResults())
				if (r1 == r2)
					return true;
		}
		
		return false;
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting())
			return;
			
		if (list.getSelectedValue() != null)
			displayErrorInfo((JobsPanelEntry)list.getSelectedValue());
		else
			displayErrorInfo(null);
	}
	
	private void displayErrorInfo(JobsPanelEntry entry)
	{
		if (entry == null)
		{
			infoText.setText("");
			return;
		}
		
		infoText.setText(entry.getJob().errorInfo);
		infoText.setCaretPosition(0);
	}
	
	private void cancelJob(int index)
	{
		if (index == -1)
			return;

		JobsPanelEntry entry = (JobsPanelEntry) list.getSelectedValue();
		
		// Warn the user...
		String msg = entry.getJob().getResult().jobName
			+ " - are you sure you wish to cancel this job?";
		if (MsgBox.yesno(msg, 1) != JOptionPane.YES_OPTION)
			return;
		
		// Then send the cancel request...
		int status = entry.getJob().getStatus();
		
		if (status != JobStatus.COMPLETING && status != JobStatus.COMPLETED)
		{
			entry.getJob().setStatus(JobStatus.CANCELLING);
			entry.updateStatus();
		
			jobsThread.interrupt();
		}
	}
	
	// Uses a result object to decide what type of job should be created for
	// running. 
	public void createJob(AnalysisResult result, AlignmentData data)
	{
		AnalysisJob job = null;
		
		// Reset any local jobs to a status of STARTING so that they DO restart
		if (result.isRemote == false)
			result.status = JobStatus.STARTING;
		
		// PDM jobs
		if (result instanceof PDMResult)
		{
			if (result.isRemote)
				job = new PDMRemoteJob((PDMResult)result, data);
			else
				job = new PDMLocalJob((PDMResult)result, data);
		}
		
		// PDM2 jobs
		if (result instanceof PDM2Result)
		{
			if (result.isRemote)
				job = new PDM2RemoteJob((PDM2Result)result, data);
			else
				job = new PDM2LocalJob((PDM2Result)result, data);
		}
		
		// HMM jobs
		if (result instanceof HMMResult)
		{
			if (result.isRemote)
				job = new HMMRemoteJob((HMMResult)result, data);
			else
				job = new HMMLocalJob((HMMResult)result, data);
		}
		
		// DSS jobs
		else if (result instanceof DSSResult)
		{
			if (result.isRemote)
				job = new DSSRemoteJob((DSSResult)result, data);
			else
				job = new DSSLocalJob((DSSResult)result, data);
		}
		
		// LRT jobs
		else if (result instanceof LRTResult)
		{
			if (result.isRemote)
				job = new LRTRemoteJob((LRTResult)result, data);
			else
				job = new LRTLocalJob((LRTResult)result, data);
		}
		
		// CodeML jobs
		else if (result instanceof CodeMLResult)
		{
			if (result.isRemote)
				job = new CodeMLRemoteJob((CodeMLResult)result, data);
			else
				job = new CodeMLLocalJob((CodeMLResult)result, data);
		}
		
		// MrBayes jobs
		else if (result instanceof MBTreeResult)
		{
			if (result.isRemote)
				job = new MBTreeRemoteJob((MBTreeResult)result, data);
			else
				job = new MBTreeLocalJob((MBTreeResult)result, data);
		}
		
		addJob(job);
	}
	
//	public static void setServerStatus(String str)
//		{ serverPanel.setStatus(str); }
	
	private static class ServerPanel extends JPanel
	{
		ServerPanel()
		{		
			JLabel label = new JLabel("Hover mouse here for remote server status",
				JLabel.LEFT);
			label.setIcon(Icons.INFO16);
		
			add(label);
			setBackground(new Color(255, 255, 225));
			setVisible(false);
		}
		
		void setStatus(String str)
		{
			setToolTipText(str);
		}
	}
}