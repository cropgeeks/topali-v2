// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm;

import java.io.*;
import java.net.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

public class RunPDM extends Thread
{
	private SequenceSet ss;
	private PDMResult result;
	
	// Directory where the job will run
	private File jobDir;
	
	public RunPDM(File jobDir, SequenceSet ss, PDMResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	public void run()
	{
		try
		{
			// Ensure the directory for this job exists
			jobDir.mkdirs();
			// Create the percent tracking directory
			new File(jobDir, "percent").mkdir();
			
			// Store the PDMResult object where it can be read by the sub-job
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			// Store the SequenceSet where it can be read by the sub-job
			Castor.saveXML(ss, new File(jobDir, "ss.xml"));
			
			// Run the analysis
			runAnalysis();
		}
		catch (Exception e)
		{
			System.out.println("RunPDM: " + e);
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void runAnalysis()
		throws Exception
	{
		if (result.isRemote)
			PDMWebService.runScript(jobDir);
		else
		{
			ThreadManager manager = new ThreadManager();
						
			PDMAnalysis analysis = new PDMAnalysis(jobDir);
			analysis.startThread(manager);
		}
	}
}