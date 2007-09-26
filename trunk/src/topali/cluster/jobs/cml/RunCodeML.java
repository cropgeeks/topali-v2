// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.*;

import pal.alignment.*;
import pal.distance.AlignmentDistanceMatrix;
import pal.misc.Identifier;
import pal.substmodel.*;
import pal.substmodel.SubstitutionModel;
import pal.tree.*;
import sbrn.commons.file.FileUtils;
import topali.cluster.StreamCatcher;
import topali.data.*;
import topali.var.NHTreeUtils;

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

		StreamCatcher sc = new StreamCatcher(proc.getInputStream(), false);
		sc.start();
		
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
	
	void saveCTLSettings(CMLHypothesis hypo) throws IOException
	{
		String settings = hypo.getCTL();
		File ctlFile = new File(wrkDir, "codeml.ctl");
		FileUtils.writeFile(ctlFile, settings);
		
		FileUtils.writeFile(new File(wrkDir, "tree.txt"), hypo.tree);
	}

	// Fast method to generate a JC/NJ tree from the alignment to be analysed
	void createTree() throws Exception
	{
		String file = new File(wrkDir, "seq.phy").getPath();
		ReadAlignment alignment = new ReadAlignment(file);

		double[] freqs = AlignmentUtils.estimateFrequencies(alignment);
		AbstractRateMatrix ratematrix = new F84(2, freqs);
		RateDistribution rate = new GammaRates(4, 4);
		SubstitutionModel sModel = SubstitutionModel.Utils.createSubstitutionModel(ratematrix, rate);
		AlignmentDistanceMatrix distmatrix = new AlignmentDistanceMatrix(new SitePattern(alignment), sModel);
		Tree tree = new NeighborJoiningTree(distmatrix);
		tree.getRoot().setIdentifier(new Identifier(""));

//		JukesCantorDistanceMatrix dm = new JukesCantorDistanceMatrix(alignment);
//		Tree tree = new NeighborJoiningTree(dm);
		
		String treeSt = tree.toString();
		treeSt = treeSt.replaceAll(";", "");
		treeSt = NHTreeUtils.removeBranchLengths(treeSt);
		
		FileUtils.writeFile(new File(wrkDir, "tree.txt"), treeSt);
	}
}
