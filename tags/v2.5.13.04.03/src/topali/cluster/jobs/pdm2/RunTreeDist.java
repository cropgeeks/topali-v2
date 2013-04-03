// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.pdm2;

import java.io.*;

import topali.cluster.StreamCatcher;
import topali.data.PDM2Result;

class RunTreeDist
{
	// Runs TreeDist on pairs of tree files (MrB output)
	void runTreeDistTypeA(File wrkDir, PDM2Result result, int win)
			throws Exception
	{
		// Before starting, make sure any previous "outfile" is deleted,
		// otherwise TreeDist will fail
		new File(wrkDir, "outfile").delete();

		ProcessBuilder pb = new ProcessBuilder(result.treeDistPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
		writer.println("win" + (win - 1));
		writer.println("D");
		writer.println("2");
		writer.println("L");
		writer.println("S");
		writer.println("Y");
		writer.println("win" + (win));
		writer.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	// Runs TreeDist on a single input file that contains every tree we've ever
	// found - TreeDist will decide which are duplicates
	void runTreeDistTypeB(File wrkDir, PDM2Result result) throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(result.treeDistPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
		writer.println("D");
		writer.println("2");
		writer.println("P");
		writer.println("S");
		writer.println("Y");
		writer.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}
}
