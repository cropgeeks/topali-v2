// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.lrt;

import java.io.*;
import java.net.*;
import java.util.*;

import pal.alignment.*;
import pal.substmodel.*;
import pal.distance.*;
import pal.tree.*;

import topali.analyses.*;
import topali.cluster.*;
import topali.data.*;
import topali.fileio.*;
import topali.mod.*;

public class RunLRT extends Thread
{
	private SequenceSet ss;
	private LRTResult result;
	
	// Directory where the job's data will be stored
	private File jobDir;
		
	public RunLRT(File jobDir, SequenceSet ss, LRTResult result)
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
			
			// Store the LRTResult object where the individual runs can get it
			Castor.saveXML(result, new File(jobDir, "submit.xml"));
			
			// Run the analyses
			runAnalyses();
		}
		catch (Exception e)
		{
			System.out.println("RunLRT: " + e);
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}
	
	// Run [n]+1 number of LRT runs on this alignment
	private void runAnalyses()
		throws Exception
	{
		ThreadManager manager = new ThreadManager(1);
		
		// Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);
		
		for (int i = 1; i <= result.runs; i++)
		{
			File runDir = new File(jobDir, "run" + i);
			runDir.mkdirs();
			
			// This is the dataset that LRT will run on
			SequenceSet dataSS = ss;
			// And if it's not the first run, then it needs to be simulated data
			if (i > 1)
				dataSS = getSimulatedAlignment();
			
			Castor.saveXML(dataSS, new File(runDir, "ss.xml"));

			if (result.isRemote == false)
			{
				LRTAnalysis analysis = new LRTAnalysis(runDir);
				analysis.startThread(manager);
			}
		}
				
		if (result.isRemote)
			LRTWebService.runScript(jobDir, result);
	}
	
	private SequenceSet getSimulatedAlignment()
		throws Exception
	{
		SimpleAlignment a = ss.getAlignment(false);

		// Create a distance matrix from this alignment
		JukesCantorDistanceMatrix distance = new JukesCantorDistanceMatrix(a);
				
		// Create a NJ tree from the distance matrix
		NeighborJoiningTree tree = new NeighborJoiningTree(distance);
		
		// SubstitutionModel
		SubstitutionModel model = null;
		
		if (result.method == LRT.METHOD_F84)
			model = TreeUtilities.getF84SubstitutionModel(a, result.tRatio, result.alpha);
		else if (result.method == LRT.METHOD_JC)
			model = TreeUtilities.getJCSubstitutionModel(a);
		
		// Simulate...
		SimulatedAlignment sim = new SimulatedAlignment(a.getLength(), tree, model);
		sim.simulate();
		
		// And convert back to TOPALi-friendly SequenceSet format
		return new SequenceSet(sim);
	}
}