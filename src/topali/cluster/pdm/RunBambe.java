// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunBambe
{
	private File wrkDir;
	
	RunBambe(File wrkDir)
	{
		this.wrkDir = wrkDir;
	}
	
	private void deleteFiles()
	{
		// Delete any existing output from Bambe
		File[] file = wrkDir.listFiles();
		for (int i = 0; i < file.length; i++)
			if (file[i].getName().toLowerCase().startsWith("run1"))
			{
				while (!file[i].delete())
				{
					System.gc();
					System.out.println("DELETE fail on " + file[i]);
					
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
				}
			}
	}
	
	void runBambe()
		throws Exception
	{
		deleteFiles();
	
		ProcessBuilder pb = null;
		if (ClusterUtils.isWindows)
			pb = new ProcessBuilder("" + new File(wrkDir, "runbambe.bat"));
		else
			pb = new ProcessBuilder("" + new File(wrkDir, "runbambe"));
		
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
		
		new StreamCatcher(proc.getInputStream(), true);
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
}