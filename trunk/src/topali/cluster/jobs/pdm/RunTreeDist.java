// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunTreeDist
{
	void runTreeDist(File wrkDir, PDMResult result) throws Exception
	{		
		ProcessBuilder pb = new ProcessBuilder(result.treeDistPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
		
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), false);
		
		writer.println("D");
		writer.println("2");
		writer.println("P");
		writer.println("S");
		writer.println("Y");
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
}
