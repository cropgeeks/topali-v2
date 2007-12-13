// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.phyml.analysis;

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

	@Override
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
	}
	
}
