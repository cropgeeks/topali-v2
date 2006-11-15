package topali.gui;

import javax.swing.*;

import static topali.cluster.JobStatus.*;
import topali.cluster.jobs.*;
import topali.data.*;

class JobsThread extends Thread
{
	private JobsPanel jobsPanel;
	private DefaultListModel model;
	private TimerThread timerThread;
	
	JobsThread(JobsPanel jobsPanel, DefaultListModel model)
	{
		this.jobsPanel = jobsPanel;
		this.model = model;
		
		timerThread = new TimerThread();
		
		start();
		timerThread.start();
	}
	
	public void run()
	{
while (true) {
			
		try { Thread.sleep(Prefs.web_check_secs * 1000); }
		catch (InterruptedException e)
		{
			// Interupts are generated when a new job is submitted so it
			// can be dealt with immediately
			timerThread.interrupt();
		}
						
		WinMainStatusBar.resetIcon = true;
					
		for (int i = 0; i < model.size(); i++)
		{
			JobsPanelEntry entry = (JobsPanelEntry) model.get(i);			
			AnalysisJob job = entry.getJob();
			
			if (job.getStatus() != FATAL_ERROR)
				job.errorInfo = null;
			
			try
			{				
				switch (job.getStatus())
				{
					case STARTING:
					{
						job.ws_submitJob();
						entry.setJobId(job.getJobId());
						
						break;
					}
						
					// For all these states, just continue to check progress
					case UNKNOWN:
					case QUEUING:
					case HOLDING:
					case RUNNING:
					case COMMS_ERROR:
					{
						float progress = job.ws_getProgress(); 
						entry.setProgress(progress);
						
						// When a job is finished, the "old" outofdate results
						// object must be removed from the dataset and replaced
						// with the new one
						if (progress >= 100f)
						{
							// Retrieve the result
							AnalysisResult oldR = job.getResult();
							AnalysisResult newR = job.ws_downloadResult();
							
							// Then tell the service to cleanup tmp files
							job.ws_cleanup();
							
							job.getAlignmentData().replaceResult(oldR, newR);
							
							jobsPanel.removeJobEntry(entry);
						}
						
						break;
					}
					
					case CANCELLING:
					{
						// Catch any errors here, because we want to cancel the
						// job regardless of what happens at the server end
						try { job.ws_cancelJob(); }
						catch (Exception e) {}
						
						jobsPanel.cancelJobEntry(entry);						
						
						break;
					}
				}
			}
			catch (org.apache.axis.AxisFault e)
			{
				String fault = e.getFaultString();
				
				if (fault.startsWith("java.net."))
					entry.getJob().setStatus(COMMS_ERROR);
					
				else
					entry.getJob().setStatus(FATAL_ERROR);
				
				job.errorInfo = e.dumpToString();
			}
			catch (Exception e)
			{
				entry.getJob().setStatus(FATAL_ERROR);
				e.printStackTrace(System.out);
				
				job.errorInfo = e.toString();
			}

			entry.updateStatus();
			
			jobsPanel.setStatusPanel();
			jobsPanel.repaint();
		}
	}}
	
	class TimerThread extends Thread
	{
		public void run()
		{
			while (true)
			{
				long current = System.currentTimeMillis();				
				for (int i = 0; i < model.size(); i++)
				{
					JobsPanelEntry entry = (JobsPanelEntry) model.get(i);				
					AnalysisJob job = entry.getJob();
					
					entry.setTimeLabel(job.getResult().startTime, current);
				}
				
				jobsPanel.repaint();
				
				try { Thread.sleep(50); }
				catch (InterruptedException e) {}
			}
		}
	}
}