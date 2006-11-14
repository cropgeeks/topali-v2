// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.cml;

import java.io.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

import sbrn.commons.file.*;
import sbrn.commons.multicore.*;

class CodeMLAnalysis extends TokenThread
{	
	// Directory where results will be stored (and temp files worked on)
	// Why two different places? Because while running on the cluster the job
	// directory is usually an NFS share - fine for writing final results to,
	// but during analysis itself it's best to write to a local HD's directory
	private File runDir, wrkDir;
	// And settings
	private CodeMLResult result;
	
	public static void main(String[] args)
	{ 
		CodeMLAnalysis analysis = null;
		
		try
		{
			analysis = new CodeMLAnalysis(new File(args[0]));
			analysis.run();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(analysis.runDir, "error.txt"), e);
		}
	}
	
	CodeMLAnalysis(File runDir)
		{ this.runDir = runDir; }
	
	public void run()
	{
		try
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
			
			// 2) Write out the input files that CODEML requires			
			RunCodeML runCodeML = new RunCodeML(wrkDir, result);
			runCodeML.saveCTLSettings(0);
			runCodeML.createTree();
			
			// 4) Run the job
			runCodeML.run();
			
		}
		catch (Exception e)
		{
			if (e.getMessage().equals("cancel") == false)			
				ClusterUtils.writeError(new File(runDir, "error.txt"), e);
		}
		
//		ClusterUtils.emptyDirectory(wrkDir, true);		
		giveToken();
	}
}