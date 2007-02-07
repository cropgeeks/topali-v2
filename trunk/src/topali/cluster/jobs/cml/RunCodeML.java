// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.*;

import pal.alignment.*;
import pal.distance.*;
import pal.tree.*;

import topali.cluster.*;
import topali.data.*;

import sbrn.commons.file.*;

class RunCodeML
{
	private File wrkDir;
	private CodeMLResult result;
	
	RunCodeML(File wrkDir, CodeMLResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}
	
	void run()
		throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(result.codemlPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);
		
		Process proc = pb.start();
						
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(
			proc.getOutputStream()));
		
		new StreamCatcher(proc.getInputStream(), true);
		
		writer.close();
		
		try { proc.waitFor(); }
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	// Saves the codeml.ctl settings file used by CODEML
	void saveCTLSettings(int modelNum)
		throws IOException
	{
		String nl = System.getProperty("line.separator");
		
		String settings = Models.getModel(modelNum);

		File ctlFile = new File(wrkDir, "codeml.ctl");
		FileUtils.writeFile(ctlFile, settings);
	}
	
	// Fast method to generate a JC/NJ tree from the alignment to be analysed
	void createTree()
		throws Exception
	{
		String file = new File(wrkDir, "seq.phy").getPath();
		ReadAlignment alignment = new ReadAlignment(file);
		
		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
		Tree tree = new NeighborJoiningTree(dm);
		
		FileUtils.writeFile(new File(wrkDir, "tree.txt"), tree.toString());
	}
}
