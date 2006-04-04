// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.pdm2;

import java.io.*;
import java.net.*;
import java.util.*;

import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

public class PDMInitializer extends Thread
{
	private SequenceSet ss;
	private PDMResult result;
	
	// Directory where the job will run
	private File jobDir;
	
	public PDMInitializer(File jobDir, SequenceSet ss, PDMResult result)
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
			
			// Store the PDMResult object where it can be read by the sub-job
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			
			// Run the analysis
			runAnalysis();
		}
		catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	private void runAnalysis()
		throws Exception
	{
		ThreadManager manager = new ThreadManager();
		
		// Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		
		// Number of processors/jobs/slots/whatever available for use
		int nodeCount = manager.getMaxTokens();
		if (result.isRemote) nodeCount = 54;						// TODO: fill in with real value

		// Before the job can be started (on the cluster) we need to break it
		// down into a set of groups, where each group contains [n] windows
		WindowChopperUpper wcu = new WindowChopperUpper(ss, result);
		RegionAnnotations.Region[] regions = wcu.getRegions(nodeCount);
		
		System.out.println("Split alignment into " + regions.length + " regions");
		
		int i = 1;
		for (RegionAnnotations.Region r: regions)
		{
			System.out.println("WINDOW: " + i);
			
			// For each region, we now save its data to a sub folder run[n]
			File runDir = new File(jobDir, "run" + (i++));
			runDir.mkdirs();
			
			File seqFile = new File(runDir, "pdm.fasta");
			ss.save(seqFile, indices, r.getS(), r.getE(), Filters.FAS, true);
			
			// Start task locally...
			if (result.isRemote == false)
			{
				PDMAnalysis analysis = new PDMAnalysis(runDir);
				analysis.startThread(manager);
			}
		}
		
		// Or on the cluster...
		if (result.isRemote)
			PDMWebService.runScript(jobDir);
	}
}