// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modelgenerator;

import java.io.*;

import topali.cluster.*;
import topali.data.MGResult;

public class ModelGeneratorProcess extends StoppableProcess
{

	private File wrkDir;

	ModelGeneratorProcess(File wrkDir, MGResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;

		runCancelMonitor();
	}

	void run() throws Exception
	{
		MGResult result = (MGResult) this.result;

		//sbrn.commons.file.FileUtils.writeFile(new File(wrkDir, "wibble"), result.mgPath);
		ProcessBuilder pb = new ProcessBuilder(result.javaPath, "-jar", result.mgPath, "mg.fasta", "4");
	
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		new StreamCatcher(proc.getInputStream(), true);

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
