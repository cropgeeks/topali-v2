// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.File;

import topali.cluster.*;
import topali.data.ModelTestResult;

public class ModelTestProcess extends StoppableProcess
{
	private File wrkDir;

	ModelTestProcess(File wrkDir, ModelTestResult result)
	{
		this.wrkDir = wrkDir;
		//super class StoppableProcess needs the result
		this.result = result;
		runCancelMonitor();
	}

	
	public void run() throws Exception
	{
		ProcessBuilder pb = null;
		if (ClusterUtils.isWindows)
		{
			File batchFile = new File(wrkDir, "runphyml.bat");
			pb = new ProcessBuilder(batchFile.toString());
		} else
		{
			File batchFile = new File(wrkDir, "runphyml.sh");
			pb = new ProcessBuilder("sh", batchFile.toString());
		}

		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		proc = pb.start();
		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();

		try
		{
			proc.waitFor();
			proc.destroy();
		} catch (Exception e)
		{
			System.out.println(e);
		}

		(new File(wrkDir, "finished")).createNewFile();
	}
}
