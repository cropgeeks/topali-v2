// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm2;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunMrBayes
{
	private File wrkDir;
	private PDM2Result result;
	
	RunMrBayes(File wrkDir, PDM2Result result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}
	
	void run()
		throws Exception
	{		
		ProcessBuilder pb = new ProcessBuilder(result.mbPath, "pdm.nex");
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
						
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), false);
		
//		writer.println("Y");
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
}
