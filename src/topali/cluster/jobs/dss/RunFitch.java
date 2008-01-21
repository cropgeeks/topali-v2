// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.dss;

import java.io.*;

import pal.distance.DistanceMatrix;
import pal.tree.Tree;
import topali.cluster.*;
import topali.data.DSSResult;

class RunFitch
{
	private static String CR = System.getProperty("line.separator");

	private DSSResult result;

	// private boolean optimize = true;
	// private double ss;

	RunFitch(DSSResult result)
	{
		this.result = result;
	}

	/*
	 * RunFitch(DSSResult result, boolean optimize) { this.result = result;
	 * this.optimize = optimize; }
	 * 
	 * private void cleanDirectory(File wrkDir) { new File(wrkDir,
	 * "outfile").delete(); new File(wrkDir, "outtree").delete(); }
	 *  // TODO: We don't actually *get* the tree in this method anymore - only
	 * the // SumsOfSquares value is read Tree getTree(File wrkDir, Tree tree,
	 * DistanceMatrix distance) throws Exception { cleanDirectory(wrkDir);
	 *  // Write out the distance matrix BufferedWriter out = new
	 * BufferedWriter( new FileWriter(new File(wrkDir, "infile")));
	 * out.write(distance.toString()); out.close();
	 *  // Write out the tree out = new BufferedWriter(new FileWriter(new
	 * File(wrkDir, "intree"))); out.write(tree.toString()); out.close();
	 *  // Run fitch runFitch(wrkDir);
	 *  // Get the sum of sqaures BufferedReader in = new BufferedReader( new
	 * FileReader(new File(wrkDir, "outfile")), 2048);
	 * 
	 * String str = in.readLine(); while (str != null) { if (str.startsWith("Sum
	 * of squares")) { try { ss = Double.parseDouble(str.substring(18)); } catch
	 * (Exception e) {} }
	 * 
	 * str = in.readLine(); }
	 * 
	 * in.close();
	 * 
	 * return null; }
	 * 
	 * 
	 * double getSS() { return ss; }
	 * 
	 * private void runFitch(File wrkDir) throws Exception { ProcessBuilder pb =
	 * new ProcessBuilder(result.fitchPath); pb.directory(wrkDir);
	 * pb.redirectErrorStream(true);
	 * 
	 * Process proc = pb.start();
	 * 
	 * PrintWriter writer = new PrintWriter(new OutputStreamWriter(
	 * proc.getOutputStream())); new StreamCatcher(proc.getInputStream(),
	 * false);
	 * 
	 * writer.println("u"); // Search for best tree = No if (!optimize)
	 * writer.println("n"); // Use lengths from user trees = Yes
	 * writer.println("p"); // Power... writer.println("0"); // 0
	 * 
	 * writer.println("y"); writer.close();
	 * 
	 * try { proc.waitFor(); proc.destroy(); } catch (Exception e) {
	 * System.out.println(e); } }
	 */

	static void saveData(File wrkDir, int run, DistanceMatrix distance,
			Tree tree) throws Exception
	{
		wrkDir = new File(wrkDir, "fitch" + run);
		wrkDir.mkdirs();

		// Write out the distance matrix
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"infile")));
		out.write(distance.toString());
		out.close();

		// Write out the tree
		out = new BufferedWriter(new FileWriter(new File(wrkDir, "intree")));
		out.write(tree.toString());
		out.close();
	}

	static double readFitchResult(File wrkDir, int run) throws Exception
	{
		wrkDir = new File(wrkDir, "fitch" + run);

		// Get the sum of squares
		BufferedReader in = new BufferedReader(new FileReader(new File(wrkDir,
				"outfile")), 2048);

		double ss = 0;
		String str = in.readLine();
		while (str != null)
		{
			if (str.startsWith("Sum of squares"))
			{
				try
				{
					ss = Double.parseDouble(str.substring(18));
				} catch (Exception e)
				{
				}
			}

			str = in.readLine();
		}

		in.close();
		return ss;
	}

	void runFitchScripts(File wrkDir, int windowCount) throws Exception
	{
		ProcessBuilder pb = null;

		if (ClusterUtils.isWindows)
		{
			writeDOSScript(wrkDir, windowCount);
			File batchFile = new File(wrkDir, "fitch_run.bat");
			pb = new ProcessBuilder(batchFile.toString());
		} else
		{
			writeUNIXScript(wrkDir);
			writeUNIXScript(new File("."));
			File batchFile = new File(wrkDir, "fitch_run.sh");
			pb = new ProcessBuilder("sh", batchFile.toString());
		}

		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();
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

	private void writeUNIXScript(File wrkDir) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"fitch_run.sh")));

		out.write("#!/bin/bash" + CR);
		out.write("for i in win*" + CR + "do" + CR);
		out.write("cd ${i}" + CR);

		writeUNIXScript(out, 1, false);
		writeUNIXScript(out, 2, true);
		if (result.passCount == DSS.TWO_PASS)
		{
			writeUNIXScript(out, 3, false);
			writeUNIXScript(out, 4, true);
		}

		out.write("cd .." + CR);
		out.write("done" + CR);

		out.close();
	}

	private void writeUNIXScript(BufferedWriter out, int run, boolean optimize)
			throws Exception
	{
		out.write("cd fitch" + run + CR);
		out.write("\"" + result.fitchPath + "\" << END1" + CR);
		if (optimize)
			out.write("u" + CR + "p" + CR + "0" + CR + "y" + CR);
		else
			out.write("u" + CR + "n" + CR + "p" + CR + "0" + CR + "y" + CR);
		out.write("END1" + CR);
		out.write("cd .." + CR);
	}

	private void writeDOSScript(File wrkDir, int windowCount) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"fitch_run.bat")));

		for (int i = 0; i < windowCount; i++)
		{
		    if(!(new File(wrkDir, "win"+(i+1))).exists())
			continue;
		    
			out.write("cd win" + (i + 1) + CR);

			writeDOSScript(out, 1, false);
			writeDOSScript(out, 2, true);

			if (result.passCount == DSS.TWO_PASS)
			{
				writeDOSScript(out, 3, false);
				writeDOSScript(out, 4, true);
			}

			out.write("cd .." + CR);
		}

		out.close();

		writeDOSInputs(wrkDir);
	}

	private void writeDOSScript(BufferedWriter out, int run, boolean optimize)
			throws Exception
	{
		out.write("cd fitch" + run + CR);
		out.write("\"" + result.fitchPath + "\" < ");
		if (optimize)
			out.write("..\\..\\fitch_optimize" + CR);
		else
			out.write("..\\..\\fitch_normal" + CR);
		out.write("cd .." + CR);
	}

	private void writeDOSInputs(File wrkDir) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"fitch_normal")));
		out.write("u" + CR + "n" + CR + "p" + CR + "0" + CR + "y");
		out.close();

		out = new BufferedWriter(new FileWriter(new File(wrkDir,
				"fitch_optimize")));
		out.write("u" + CR + "p" + CR + "0" + CR + "y");
		out.close();
	}
}
