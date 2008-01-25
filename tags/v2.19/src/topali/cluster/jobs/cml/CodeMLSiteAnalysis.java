// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.File;

import scri.commons.file.FileUtils;
import topali.cluster.*;
import topali.cluster.jobs.cml.parser.CMLResultParser;
import topali.data.*;
import topali.fileio.Castor;

class CodeMLSiteAnalysis extends AnalysisThread
{
	private CodeMLResult result;

	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
	{
		new CodeMLSiteAnalysis(new File(args[0])).run();
	}

	// If running locally, the job will be started via a normal constructor call
	CodeMLSiteAnalysis(File runDir)
	{
		super(runDir);
	}

	@Override
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

		// 2) Work out (from the run directory, which run/model to use)
		int i = Integer.parseInt(runDir.getName().substring(3)); 
		CMLModel model = result.models.get(i-1); // (i-1) as i is starting with 1

		// 3) Write out the input files that CODEML requires
		RunCodeML runCodeML = new RunCodeML(wrkDir, result);
		runCodeML.saveCTLSettings(model);
		runCodeML.createTree();
		
		// 4) Run the job
		runCodeML.run();
		
		// TEMP (for now) - copy results from wrkDir to runDir
		for (File f : wrkDir.listFiles())
			FileUtils.copyFile(f, new File(runDir, f.getName()), false);

		File resultsFile = new File(wrkDir, "results.txt");
		File rstFile = new File(wrkDir, "rst");

		CMLResultParser parser = CMLResultParser.getParser(model);
		parser.parse(resultsFile.getPath(), rstFile.getPath());

		Castor.saveXML(model, new File(runDir, "model.xml"));

		//ClusterUtils.emptyDirectory(wrkDir, true);
	}
}