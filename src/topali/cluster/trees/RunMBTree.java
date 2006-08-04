// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.trees;

import java.io.*;
import java.util.logging.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;

public class RunMBTree extends Thread
{
	private static Logger logger = Logger.getLogger("topali.cluster");
	
	private SequenceSet ss;
	private MBTreeResult result;
	
	// Directory where the job will run
	private File jobDir;
	
	public RunMBTree(File jobDir, SequenceSet ss, MBTreeResult result)
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
//			// Create the percent tracking directory
//			new File(jobDir, "percent").mkdir();
			
			// Store the MBTreeResult object where it can be read by the sub-job
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			// Store the SequenceSet where it can be read by the sub-job
			Castor.saveXML(ss, new File(jobDir, "ss.xml"));
			
			// Run the analysis
			runAnalysis();
		}
		catch (Exception e)
		{
			logger.severe(""+e);
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void runAnalysis()
		throws Exception
	{
		if (result.isRemote)
		{
			logger.info("analysis ready: submitting to cluster");
			MBTreeWebService.runScript(jobDir);
		}
		else
		{
			MBTreeAnalysis analysis = new MBTreeAnalysis(jobDir);
			analysis.startThread(LocalJobs.manager);
		}
	}
}