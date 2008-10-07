// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.modeltest.analysis;

import java.io.*;

import topali.cluster.StreamCatcher;

public class TreeDistProcess
{

	public void run(File wrkDir, String trees, String treeDistPath) throws Exception {
		
		File four = new File(wrkDir, "fourTrees.txt");
		File all = new File(wrkDir, "allTrees.txt");
		
		String[] tmp = trees.split(";");
		BufferedWriter out1 = new BufferedWriter(new FileWriter(four));
		BufferedWriter out2 = new BufferedWriter(new FileWriter(all));
		for(int i=0; i<tmp.length; i++) {
			if(i<=3)
				out1.write(tmp[i]+";\n");
			else 
				out2.write(tmp[i]+";\n");
		}
		out1.flush();
		out2.flush();
		out1.close();
		out2.close();
		
		ProcessBuilder pb = new ProcessBuilder(treeDistPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
		writer.println("fourTrees.txt");
		writer.println("D");
		writer.println("2");
		writer.println("L");
		writer.println("S");
		writer.println("Y");
		writer.println("allTrees.txt");
		writer.flush();
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
