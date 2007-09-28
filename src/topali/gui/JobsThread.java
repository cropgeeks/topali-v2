// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import static topali.cluster.JobStatus.*;

import javax.swing.*;

import org.apache.log4j.Logger;

import topali.cluster.JobStatus;
import topali.cluster.jobs.AnalysisJob;
import topali.data.AnalysisResult;

import doe.*;

public class JobsThread extends Thread
{
	 Logger log = Logger.getLogger(this.getClass());

	private final JobsPanel jobsPanel;

	private TimerThread timerThread;

	JobsThread(JobsPanel jobsPanel)
	{
		this.jobsPanel = jobsPanel;
		timerThread = new TimerThread();
		timerThread.start();
	}

	@Override
	public void run()
	{
		while (true)
		{

			try
			{
				Thread.sleep(Prefs.web_check_secs * 1000);
			} catch (InterruptedException e)
			{
				// Interupts are generated when a new job is submitted so it
				// can be dealt with immediately
				timerThread.interrupt();
			}

			WinMainStatusBar.resetIcon = true;

			for (int i = 0; i < jobsPanel.jobs.size(); i++)
			{
				final JobsPanelEntry entry = jobsPanel.jobs.get(i);
				final AnalysisJob job = entry.getJob();

				if (job.getStatus() != FATAL_ERROR)
					job.errorInfo = null;

				try
				{
					switch (job.getStatus())
					{
					case STARTING:
					{
						job.ws_submitJob();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								entry.setJobId(job.getJobId());
							}
						});
						break;
					}

						// For all these states, just continue to check progress
					case UNKNOWN:
					case QUEUING:
					case HOLDING:
					case RUNNING:
					case COMMS_ERROR:
					{
						final JobStatus status = job.ws_getProgress();
						log.info("Requested JobStatus for "+job.getJobId()+": "+status);

						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								entry.setJobStatus(status);
							}
						});

						// When a job is finished, the "old" outofdate results
						// object must be removed from the dataset and replaced
						// with the new one
						if (status.progress >= 100f)
						{
							// Retrieve the result
							AnalysisResult oldR = job.getResult();
							AnalysisResult newR = job.ws_downloadResult();

							// Then tell the service to cleanup tmp files
							job.ws_cleanup();

							job.getAlignmentData().replaceResult(oldR, newR);

							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									jobsPanel.removeJobEntry(entry, true);
								}
							});
						}

						break;
					}

					case CANCELLING:
					{
						// Catch any errors here, because we want to cancel the
						// job regardless of what happens at the server end
						try
						{
							job.ws_cancelJob();
						} catch (Exception e)
						{
							log.warn(e);
						}

						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								jobsPanel.removeJobEntry(entry, false);
							}
						});
						break;
					}
					}
				} catch (org.apache.axis.AxisFault e)
				{
					String fault = e.getFaultString();

					//--- Catch the error, which is thrown when the webservice (alignment size) limit has been exceeded.
					String msg = e.getMessage();
					//TODO: Look for better way to do this
					//msg = java.util.concurrent.RejectedExecutionException: Max. alignment size for this job type is limited to 10000 bases in total.
					if(msg.contains("The maximum alignment")) {
						String[] tmp = msg.split("\\s+");
						int limit = Integer.parseInt(tmp[18]);

						MsgBox.msg("This job type has been limited (by the server) to a maximum "
							+ "of " + limit + " sequences per submission. Please reduce the number "
							+ "of sequences selected and try again.\nYou can either manually choose "
							+ "a subset of sequences, or use F84 phylogenetic tree estimation (with "
							+ "group-clustering) to auto select a subset.", MsgBox.ERR);
					//	jobsPanel.removeJobEntry(entry, false);
					}
					//---

					final int error = (fault.startsWith("java.net.")) ? COMMS_ERROR
							: FATAL_ERROR;
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							entry.getJob().setStatus(error);
						}
					});
					job.errorInfo = e.dumpToString();
					log.warn(e);
				} catch (Exception e)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							entry.getJob().setStatus(FATAL_ERROR);
						}
					});
					job.errorInfo = e.toString();
					log.warn(e);
				}

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						entry.updateStatus();
					}
				});
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					jobsPanel.setStatusPanel();
				}
			});

		}
	}

	class TimerThread extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				final long current = System.currentTimeMillis();
				for (final JobsPanelEntry entry : jobsPanel.jobs)
				{
					AnalysisJob job = entry.getJob();
					final long start = job.getResult().startTime;
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							entry.setTimeLabel(start, current);
						}
					});
				}

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						jobsPanel.repaint();
					}
				});

				try
				{
					Thread.sleep(100);
				} catch (InterruptedException e)
				{
				}
			}
		}
	}
}