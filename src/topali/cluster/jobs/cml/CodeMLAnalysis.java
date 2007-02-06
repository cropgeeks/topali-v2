// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.cml;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

import sbrn.commons.file.*;

class CodeMLAnalysis extends AnalysisThread
{	
	private CodeMLResult result;
	
	// If running on the cluster, the subjob will be started within its own JVM
	public static void main(String[] args)
		{ new CodeMLAnalysis(new File(args[0])).run(); }
	
	// If running locally, the job will be started via a normal constructor call
	CodeMLAnalysis(File runDir)
		{ super(runDir); }


	public void runAnalysis()
		throws Exception
	{
		// Read the CodeMLResult
		File jobDir = runDir.getParentFile();
		File resultFile = new File(jobDir, "submit.xml");
		result = (CodeMLResult) Castor.unmarshall(resultFile);
		
		// Temporary working directory
		wrkDir = ClusterUtils.getWorkingDirectory(
			result,	jobDir.getName(), runDir.getName());

		// 1) Copy the sequence data to the working directory
		File src = new File(jobDir, "seq.phy");
		File des = new File(wrkDir, "seq.phy");
		FileUtils.copyFile(src, des, false);
		
		// 2) Work out (from the run directory, which run/model to use)
		int modelType = Integer.parseInt(runDir.getName().substring(3));
				
		// 3) Write out the input files that CODEML requires			
		RunCodeML runCodeML = new RunCodeML(wrkDir, result);
		runCodeML.saveCTLSettings(modelType);
		runCodeML.createTree();
		
		// 4) Run the job
		runCodeML.run();
		
		
		// TEMP (for now) - copy results from wrkDir to runDir
		for (File f: wrkDir.listFiles())
			FileUtils.copyFile(f, new File(runDir, f.getName()), false);
		
		new File(runDir, "ok").createNewFile();	
		
		ClusterUtils.emptyDirectory(wrkDir, true);
	}
}