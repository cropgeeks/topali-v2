// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.cluster.jobs.dss;

import java.io.File;

import pal.alignment.SimpleAlignment;
import pal.distance.JukesCantorDistanceMatrix;
import pal.substmodel.SubstitutionModel;
import pal.tree.*;
import topali.analyses.TreeUtilities;
import topali.cluster.*;
import topali.cluster.jobs.dss.analysis.*;
import topali.data.*;
import topali.fileio.Castor;
import topali.mod.Filters;

public class DSSInitializer extends Thread
{
	private SequenceSet ss;

	private DSSResult result;

	// Directory where the job's data will be stored
	private File jobDir;

	public DSSInitializer(File jobDir, SequenceSet ss, DSSResult result)
	{
		this.jobDir = jobDir;
		this.ss = ss;
		this.result = result;
	}

	
	public void run()
	{
		try
		{
			startThreads();
		} catch (Exception e)
		{
			ClusterUtils.writeError(new File(jobDir, "error.txt"), e);
		}
	}

	public void startThreads() throws Exception
	{
		// Ensure the directory for this job exists
		jobDir.mkdirs();

		// Store the DSSResult object where the individual runs can get it
		Castor.saveXML(result, new File(jobDir, "submit.xml"));

		// Sequences that should be selected/saved for processing
		int[] indices = ss.getIndicesFromNames(result.selectedSeqs);

		for (int i = 1; i <= result.runs; i++)
		{
			if (LocalJobs.isRunning(result.jobId) == false)
				return;

			File runDir = new File(jobDir, "run" + i);
			runDir.mkdirs();

			// This is the dataset that DSS will run on
			SequenceSet dataSS = ss;
			// And if it's not the first run, then it needs to be simulated data
			if (i > 1)
				dataSS = getSimulatedAlignment();

			dataSS.save(new File(runDir, "dss.fasta"), indices, Filters.FAS,
					false);

			if (result.isRemote == false)
				new DSSAnalysis(runDir).start(LocalJobs.manager);
		}

		if (result.isRemote)
			DSSWebService.runScript(jobDir, result);
	}

	private SequenceSet getSimulatedAlignment() throws Exception
	{
		SimpleAlignment a = ss.getAlignment(false);

		// Create a distance matrix from this alignment
		JukesCantorDistanceMatrix distance = new JukesCantorDistanceMatrix(a);

		// Create a NJ tree from the distance matrix
		NeighborJoiningTree tree = new NeighborJoiningTree(distance);

		// SubstitutionModel
		SubstitutionModel model = null;

		if (result.method == DSS.METHOD_F84)
			model = TreeUtilities.getF84SubstitutionModel(a, result.tRatio,
					result.alpha);
		else if (result.method == DSS.METHOD_JC)
			model = TreeUtilities.getJCSubstitutionModel(a);

		// Simulate...
		SimulatedAlignment sim = new SimulatedAlignment(a.getLength(), tree,
				model);
		sim.simulate();

		// And convert back to TOPALi-friendly SequenceSet format
		return new SequenceSet(sim);
	}
}