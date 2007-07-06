// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.fastml;

import java.io.*;

import topali.cluster.*;
import topali.data.FastMLResult;

public class FastMLProcess extends StoppableProcess
{

	private File wrkDir;
	
	FastMLProcess(File wrkDir, FastMLResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
		runCancelMonitor();
	}

	void run() throws Exception
	{
		FastMLResult result = (FastMLResult) this.result;

		String[] cmds = {result.fastmlPath, "-g", "-b", "-s", "seq.fasta", "-t", "tree.txt"};
		//sbrn.commons.file.FileUtils.writeFile(new File(wrkDir, "wibble"), result.mgPath);
		ProcessBuilder pb = new ProcessBuilder(cmds);
	
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
