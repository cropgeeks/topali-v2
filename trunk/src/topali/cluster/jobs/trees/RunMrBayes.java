// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.trees;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunMrBayes extends StoppableProcess
{
	private File wrkDir;
			
	RunMrBayes(File wrkDir, MBTreeResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		
		runCancelMonitor();
	}
	
	void run()
		throws Exception
	{		
		MBTreeResult mbResult = (MBTreeResult) result;
	
		ProcessBuilder pb = new ProcessBuilder(mbResult.mbPath, "mb.nex");
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		proc = pb.start();
						
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), true);
		
//		writer.println("Y");
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e)
		{
			System.out.println(e);
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");
		}
	}
}
