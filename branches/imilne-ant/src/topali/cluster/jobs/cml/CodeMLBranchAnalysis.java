// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;

import sbrn.commons.file.FileUtils;
import topali.cluster.*;
import topali.cluster.jobs.cml.parser.CMLBranchResultParser;
import topali.data.*;
import topali.fileio.Castor;

public class CodeMLBranchAnalysis extends AnalysisThread
{
	private CodeMLResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new CodeMLBranchAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	CodeMLBranchAnalysis(File runDir)
	{
		super(runDir);
	}

	public void runAnalysis() throws Exception
	{
		// Read the CodeMLResult
		File jobDir = runDir.getParentFile();
		File resultFile = new File(jobDir, "submit.xml");
		try
		{
			result = (CodeMLResult) Castor.unmarshall(resultFile);
		} catch (RuntimeException e)
		{
			e.printStackTrace();
		}

		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(result, jobDir.getName(),
				runDir.getName());

		// 1) Copy the sequence data to the working directory
		File src = new File(jobDir, "seq.phy");
		File des = new File(wrkDir, "seq.phy");
		FileUtils.copyFile(src, des, false);

		// 2) Work out (from the run directory, which hypothesis to use)
		int i = Integer.parseInt(runDir.getName().substring(3)); 
		CMLHypothesis hypo = result.hypos.get(i-1); // (i-1) as i is starting with 1

		// 3) Write out the input files that CODEML requires
		RunCodeML runCodeML = new RunCodeML(wrkDir, result);
		runCodeML.saveCTLSettings(hypo);
		
		// 4) Run the job
		runCodeML.run();
		
		// TEMP (for now) - copy results from wrkDir to runDir
		for (File f : wrkDir.listFiles())
			FileUtils.copyFile(f, new File(runDir, f.getName()), false);

		File resultsFile = new File(wrkDir, "results.txt");

		CMLBranchResultParser parser = new CMLBranchResultParser(hypo);
		parser.parse(resultsFile);
		
		Castor.saveXML(hypo, new File(runDir, "hypo.xml"));

		ClusterUtils.emptyDirectory(wrkDir, true);
	}

}
