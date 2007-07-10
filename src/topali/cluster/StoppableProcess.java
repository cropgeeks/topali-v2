// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster;

import topali.data.*;

/*
 * This is a base class for classes that wish to run native process jobs, but
 * still keep a TOPALi-eye on them and kill them if the user wishes.
 */
public abstract class StoppableProcess
{
	protected AnalysisResult result = null;
	
	protected Process proc = null;
	protected volatile boolean isRunning = true;
	
	public void runCancelMonitor()
	{
		// Before we start, kick off a thread to monitor for job cancellations
		Runnable r = new Runnable() {
			public void run()
			{
				while (isRunning)
				{
					if (LocalJobs.isRunning(result.jobId) == false)
					{
						proc.destroy();
						isRunning = false;
					}
					
					try { Thread.sleep(1000); }
					catch (InterruptedException e) {}
				}
			}
		};
		
		if (result.isRemote == false)
			new Thread(r).start();
	}
}