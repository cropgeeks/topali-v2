// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.*;

import pal.alignment.ReadAlignment;
import pal.distance.JukesCantorDistanceMatrix;
import pal.tree.NeighborJoiningTree;
import pal.tree.Tree;
import sbrn.commons.file.FileUtils;
import topali.cluster.StreamCatcher;
import topali.data.CMLModel;
import topali.data.CodeMLResult;

class RunCodeML
{
	private File wrkDir;

	private CodeMLResult result;

	RunCodeML(File wrkDir, CodeMLResult result)
	{
		this.wrkDir = wrkDir;
		this.result = result;
	}

	void run() throws Exception
	{
		ProcessBuilder pb = new ProcessBuilder(result.codemlPath);
		pb.directory(wrkDir);
		pb.redirectErrorStream(true);

		Process proc = pb.start();

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(proc
				.getOutputStream()));

		new StreamCatcher(proc.getInputStream(), true);

		writer.close();

		try
		{
			proc.waitFor();
		} catch (Exception e)
		{
			System.out.println(e);
		}
	}

	// Saves the codeml.ctl settings file used by CODEML
	void saveCTLSettings(CMLModel model) throws IOException
	{
		String settings = model.codemlSettings();

		File ctlFile = new File(wrkDir, "codeml.ctl");
		FileUtils.writeFile(ctlFile, settings);
	}

	// Fast method to generate a JC/NJ tree from the alignment to be analysed
	void createTree() throws Exception
	{
		String file = new File(wrkDir, "seq.phy").getPath();
		ReadAlignment alignment = new ReadAlignment(file);

		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
		Tree tree = new NeighborJoiningTree(dm);

		FileUtils.writeFile(new File(wrkDir, "tree.txt"), tree.toString());
	}
}
