// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;

import topali.cluster.*;
import topali.data.*;

class RunTreeDist
{
	void runTreeDist(File wrkDir, PDM2Result result, int win)
		throws Exception
	{		
		// Before starting, make sure any previous "outfile" is deleted,
		// otherwise TreeDist will fail
		new File(wrkDir, "outfile").delete();
		
		ProcessBuilder pb = new ProcessBuilder(result.treeDistPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
				
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), false);
		
		writer.println("win" + (win-1) + ".txt");
		writer.println("D");
		writer.println("2");
		writer.println("L");
		writer.println("S");
		writer.println("Y");
		writer.println("win" + (win) + ".txt");
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
}
