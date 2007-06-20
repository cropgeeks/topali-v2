// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml;

import java.io.*;

import topali.cluster.*;
import topali.data.PhymlResult;

public class PhymlProcess extends StoppableProcess
{

	private File wrkDir;
	
	PhymlProcess(File wrkDir, PhymlResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		runCancelMonitor();
	}

	void run() throws Exception
	{
		PhymlResult result = (PhymlResult) this.result;
		
		ProcessBuilder pb = new ProcessBuilder(result.phymlParameters);
	
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
		writer.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
			if (LocalJobs.isRunning(result.jobId) == false)
				throw new Exception("cancel");
		}
	}
	
}
